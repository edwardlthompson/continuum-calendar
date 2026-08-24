use std::path::{Path, PathBuf};
use tauri::App;
use tauri_plugin_autostart::ManagerExt;

const RUN_VALUE: &str = "Continuum Calendar";

/// True when `path` is a Cargo `target/debug` binary (dev `tauri:dev` EXE).
pub fn is_cargo_debug_exe(path: &Path) -> bool {
    path_has_target_profile(path, "debug")
}

/// True when `path` is a Cargo `target/release` binary (repo build, not product install).
pub fn is_cargo_release_exe(path: &Path) -> bool {
    path_has_target_profile(path, "release")
}

fn path_has_target_profile(path: &Path, profile: &str) -> bool {
    let parts: Vec<String> = path
        .components()
        .filter_map(|c| c.as_os_str().to_str().map(|s| s.to_ascii_lowercase()))
        .collect();
    parts
        .windows(2)
        .any(|w| w[0] == "target" && w[1] == profile)
}

/// Run key should not point at repo `target\debug` or `target\release`.
pub fn run_value_is_repo_cargo_exe(value: &str) -> bool {
    let trimmed = value.trim().trim_matches('"');
    let p = Path::new(trimmed);
    is_cargo_debug_exe(p) || is_cargo_release_exe(p)
}

pub fn run_value_is_stale_dev(value: &str) -> bool {
    let trimmed = value.trim().trim_matches('"');
    is_cargo_debug_exe(Path::new(trimmed))
}

fn installed_product_exe() -> Option<PathBuf> {
    let local = std::env::var_os("LOCALAPPDATA")?;
    let p = PathBuf::from(local)
        .join("Continuum Calendar")
        .join("app.exe");
    if p.is_file() {
        Some(p)
    } else {
        None
    }
}

/// Debug EXEs use `localhost:5173`. Never persist those for login.
/// Also rewrite Run keys that point at repo `target\debug` or `target\release`
/// to the AppData install when that file exists.
pub fn heal_on_startup(app: &App) {
    let exe = std::env::current_exe().unwrap_or_default();
    let mgr = app.autolaunch();
    let this_is_dev = cfg!(debug_assertions) || is_cargo_debug_exe(&exe);

    if this_is_dev {
        if mgr.is_enabled().unwrap_or(false) {
            if let Err(e) = mgr.disable() {
                log::warn!("autostart: refused debug login entry ({e})");
            }
        }
        return;
    }

    let run_val = windows_run_value(RUN_VALUE);
    let needs_heal = run_val
        .as_ref()
        .is_some_and(|v| run_value_is_repo_cargo_exe(v))
        || run_val
            .as_ref()
            .is_some_and(|v| run_value_is_stale_dev(v));

    if needs_heal {
        if let Some(product) = installed_product_exe() {
            if let Err(e) = write_windows_run(RUN_VALUE, &product) {
                log::warn!("autostart: could not rewrite Run key ({e})");
            }
        }
    }

    if needs_heal || mgr.is_enabled().unwrap_or(false) {
        if let Err(e) = mgr.enable() {
            log::warn!("autostart: could not point login at installed EXE ({e})");
        }
    }
}

#[cfg(windows)]
fn windows_run_value(name: &str) -> Option<String> {
    let out = std::process::Command::new("reg")
        .args([
            "query",
            r"HKCU\Software\Microsoft\Windows\CurrentVersion\Run",
            "/v",
            name,
        ])
        .output()
        .ok()?;
    if !out.status.success() {
        return None;
    }
    let text = String::from_utf8_lossy(&out.stdout);
    let line = text.lines().find(|l| l.contains("REG_SZ"))?;
    let idx = line.find("REG_SZ")?;
    Some(line[idx + 6..].trim().to_string())
}

#[cfg(windows)]
fn write_windows_run(name: &str, exe: &Path) -> Result<(), String> {
    let quoted = format!("\"{}\"", exe.display());
    let status = std::process::Command::new("reg")
        .args([
            "add",
            r"HKCU\Software\Microsoft\Windows\CurrentVersion\Run",
            "/v",
            name,
            "/t",
            "REG_SZ",
            "/d",
            &quoted,
            "/f",
        ])
        .status()
        .map_err(|e| e.to_string())?;
    if status.success() {
        Ok(())
    } else {
        Err(format!("reg add exit {status}"))
    }
}

#[cfg(not(windows))]
fn windows_run_value(_name: &str) -> Option<String> {
    None
}

#[cfg(not(windows))]
fn write_windows_run(_name: &str, _exe: &Path) -> Result<(), String> {
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;

    #[test]
    fn detects_cargo_debug_exe() {
        let p = PathBuf::from(r"C:\repo\apps\desktop\src-tauri\target\debug\app.exe");
        assert!(is_cargo_debug_exe(&p));
        assert!(run_value_is_stale_dev(r#""C:\repo\target\debug\app.exe""#));
        assert!(run_value_is_repo_cargo_exe(r#"C:\repo\target\debug\app.exe"#));
    }

    #[test]
    fn detects_cargo_release_exe() {
        let p = PathBuf::from(r"C:\repo\apps\desktop\src-tauri\target\release\app.exe");
        assert!(is_cargo_release_exe(&p));
        assert!(run_value_is_repo_cargo_exe(&p.to_string_lossy()));
        assert!(!run_value_is_stale_dev(&p.to_string_lossy()));
    }

    #[test]
    fn installed_exe_is_not_dev() {
        let p = PathBuf::from(r"C:\Users\edwar\AppData\Local\Continuum Calendar\app.exe");
        assert!(!is_cargo_debug_exe(&p));
        assert!(!is_cargo_release_exe(&p));
        assert!(!run_value_is_stale_dev(&p.to_string_lossy()));
        assert!(!run_value_is_repo_cargo_exe(&p.to_string_lossy()));
    }
}
