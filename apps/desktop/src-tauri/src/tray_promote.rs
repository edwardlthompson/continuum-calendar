use std::collections::HashSet;
use std::path::Path;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Mutex;
use tauri::Manager;

static PROMOTED: AtomicBool = AtomicBool::new(false);
static BASELINE: Mutex<Option<HashSet<String>>> = Mutex::new(None);

pub fn normalize_exe_path(raw: &str) -> String {
    raw.trim()
        .trim_matches('"')
        .trim_start_matches(r"\\?\")
        .replace('/', "\\")
        .to_ascii_lowercase()
}

pub fn exe_paths_match(left: &str, right: &str) -> bool {
    let a = normalize_exe_path(left);
    let b = normalize_exe_path(right);
    !a.is_empty() && a == b
}

/// Win11 writes new icons as IsPromoted=0. That is not a user hide.
pub(crate) fn should_force_promote(is_new: bool, current: Option<u32>) -> bool {
    match current {
        Some(0) if !is_new => false,
        Some(1) => false,
        _ => true,
    }
}

/// Snapshot NotifyIconSettings subkeys before NIM_ADD so orphans can be claimed.
pub fn capture_baseline() {
    #[cfg(windows)]
    {
        let set = match read_subkey_names() {
            Ok(s) => Some(s),
            Err(_) => None,
        };
        if let Ok(mut g) = BASELINE.lock() {
            *g = set;
        }
    }
}

/// Drop leftover keys from prior UID (retry-add used COUNTER=2; each process starts at 1).
pub fn sweep_stale_own_keys() -> u32 {
    PROMOTED.store(false, Ordering::Relaxed);
    #[cfg(windows)]
    {
        return try_sweep().unwrap_or(0);
    }
    #[cfg(not(windows))]
    0
}

pub fn promote_later(app: tauri::AppHandle) {
    std::thread::spawn(move || {
        for wait in [2u64, 5, 12] {
            std::thread::sleep(std::time::Duration::from_secs(wait));
            let p = promote_own_icon();
            if let Ok(dir) = app.path().app_log_dir() {
                let _ = std::fs::write(dir.join("tray-promote.txt"), format!("promote={p}"));
            }
            if p == "promoted" || p == "already" {
                break;
            }
        }
    });
}

pub fn promote_own_icon() -> &'static str {
    if PROMOTED.load(Ordering::Relaxed) {
        return "already";
    }
    match try_promote() {
        Ok(true) => {
            PROMOTED.store(true, Ordering::Relaxed);
            "promoted"
        }
        Ok(false) => "no-key-yet",
        Err(_) => "reg-err",
    }
}

#[cfg(windows)]
fn read_subkey_names() -> Result<HashSet<String>, Box<dyn std::error::Error>> {
    let hkcu = winreg::RegKey::predef(winreg::enums::HKEY_CURRENT_USER);
    let settings = hkcu.open_subkey(r"Control Panel\NotifyIconSettings")?;
    Ok(settings.enum_keys().filter_map(Result::ok).collect())
}

#[cfg(windows)]
fn own_exe_key(path: &str, exe: &Path, exe_s: &str) -> bool {
    !path.is_empty() && (exe_paths_match(path, exe_s) || path_ends_with_app(path, exe))
}

#[cfg(windows)]
fn try_sweep() -> Result<u32, Box<dyn std::error::Error>> {
    let exe = std::env::current_exe()?;
    let exe_s = exe.to_string_lossy().to_string();
    let hkcu = winreg::RegKey::predef(winreg::enums::HKEY_CURRENT_USER);
    let settings = hkcu.open_subkey_with_flags(
        r"Control Panel\NotifyIconSettings",
        winreg::enums::KEY_READ | winreg::enums::KEY_WRITE,
    )?;
    let mut stale = Vec::new();
    for name in settings.enum_keys().filter_map(Result::ok) {
        let sub = match settings.open_subkey(&name) {
            Ok(s) => s,
            Err(_) => continue,
        };
        let path: String = sub.get_value("ExecutablePath").unwrap_or_default();
        if !own_exe_key(&path, &exe, &exe_s) {
            continue;
        }
        let promoted: u32 = sub.get_value("IsPromoted").unwrap_or(2);
        if promoted == 0 {
            continue;
        }
        stale.push(name);
    }
    let mut deleted = 0u32;
    for name in &stale {
        if settings.delete_subkey_all(name).is_ok() {
            deleted += 1;
        }
    }
    Ok(deleted)
}

#[cfg(windows)]
fn try_promote() -> Result<bool, Box<dyn std::error::Error>> {
    let exe = std::env::current_exe()?;
    let exe_s = exe.to_string_lossy().to_string();
    let hkcu = winreg::RegKey::predef(winreg::enums::HKEY_CURRENT_USER);
    let settings = hkcu.open_subkey_with_flags(
        r"Control Panel\NotifyIconSettings",
        winreg::enums::KEY_READ | winreg::enums::KEY_WRITE,
    )?;
    let baseline = BASELINE.lock().ok().and_then(|g| g.clone());
    let mut promoted_new = false;
    let mut orphans: Vec<String> = Vec::new();

    for name in settings.enum_keys().filter_map(Result::ok) {
        let sub = match settings.open_subkey_with_flags(
            &name,
            winreg::enums::KEY_READ | winreg::enums::KEY_SET_VALUE,
        ) {
            Ok(s) => s,
            Err(_) => continue,
        };
        let path: String = sub.get_value("ExecutablePath").unwrap_or_default();
        if !path.is_empty() {
            if !own_exe_key(&path, &exe, &exe_s) {
                continue;
            }
            let current: Option<u32> = sub.get_value("IsPromoted").ok();
            let is_new = baseline.as_ref().map(|b| !b.contains(&name)).unwrap_or(true);
            if should_force_promote(is_new, current) {
                sub.set_value("IsPromoted", &1u32)?;
            }
            if is_new {
                promoted_new = true;
            }
            continue;
        }
        let Some(ref base) = baseline else {
            continue;
        };
        if base.contains(&name) {
            continue;
        }
        let snap_ok = sub
            .get_raw_value("IconSnapshot")
            .map(|v| !v.bytes.is_empty())
            .unwrap_or(false);
        if !snap_ok {
            continue;
        }
        orphans.push(name);
    }

    if promoted_new {
        return Ok(true);
    }
    if orphans.len() == 1 {
        let name = &orphans[0];
        let sub = settings.open_subkey_with_flags(
            name,
            winreg::enums::KEY_READ | winreg::enums::KEY_SET_VALUE,
        )?;
        sub.set_value("ExecutablePath", &exe_s)?;
        sub.set_value("IsPromoted", &1u32)?;
        return Ok(true);
    }
    Ok(false)
}

fn path_ends_with_app(reg_path: &str, exe: &Path) -> bool {
    let name = exe.file_name().and_then(|n| n.to_str()).unwrap_or("");
    let parent = exe
        .parent()
        .and_then(|p| p.file_name())
        .and_then(|n| n.to_str())
        .unwrap_or("");
    if name.is_empty() || parent.is_empty() {
        return false;
    }
    let needle = format!("{}\\{}", parent.to_ascii_lowercase(), name.to_ascii_lowercase());
    normalize_exe_path(reg_path).ends_with(&needle)
}

#[cfg(not(windows))]
fn try_promote() -> Result<bool, Box<dyn std::error::Error>> {
    Ok(false)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn matches_installed_exe_case() {
        let a = r"C:\Users\edwar\AppData\Local\Continuum Calendar\app.exe";
        let b = r#"C:\users\edwar\appdata\local\continuum calendar\app.exe"#;
        assert!(exe_paths_match(a, b));
    }

    #[test]
    fn rejects_other_app_exe() {
        assert!(!exe_paths_match(
            r"C:\Users\edwar\AppData\Local\Continuum Calendar\app.exe",
            r"C:\Other\app.exe",
        ));
    }

    #[test]
    fn new_hidden_key_is_promoted() {
        assert!(should_force_promote(true, Some(0)));
        assert!(should_force_promote(true, None));
    }

    #[test]
    fn user_hid_old_key_is_respected() {
        assert!(!should_force_promote(false, Some(0)));
        assert!(!should_force_promote(false, Some(1)));
    }
}
