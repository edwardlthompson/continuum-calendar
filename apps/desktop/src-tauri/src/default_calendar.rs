//! Register Continuum as the default calendar handler (MIME + webcal).
//! Prefer the packaged `.deb` desktop file so a second user-local copy is not created.

use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;

const FALLBACK_DESKTOP_ID: &str = "org.continuumcalendar.app.desktop";
const PACKAGED_DESKTOP_IDS: &[&str] = &[
    "continuum-calendar.desktop",
    "org.continuumcalendar.app.desktop",
    "Continuum Calendar.desktop",
];
const MIME_TYPES: &[&str] = &[
    "text/calendar",
    "application/ics",
    "x-scheme-handler/webcal",
    "x-scheme-handler/webcals",
];

fn applications_dir() -> PathBuf {
    let base = std::env::var_os("XDG_DATA_HOME")
        .map(PathBuf::from)
        .unwrap_or_else(|| {
            dirs_fallback_home()
                .map(|h| h.join(".local/share"))
                .unwrap_or_else(|| PathBuf::from("."))
        });
    base.join("applications")
}

fn dirs_fallback_home() -> Option<PathBuf> {
    std::env::var_os("HOME").map(PathBuf::from)
}

fn packaged_desktop_id() -> Option<&'static str> {
    PACKAGED_DESKTOP_IDS.iter().copied().find(|id| {
        Path::new("/usr/share/applications").join(id).is_file()
    })
}

fn desktop_file_contents(exec: &Path) -> String {
    let exec_s = exec.display().to_string();
    format!(
        "[Desktop Entry]\n\
Type=Application\n\
Version=1.0\n\
Name=Continuum Calendar\n\
Comment=Continuum Calendar\n\
Exec=\"{exec_s}\" %u\n\
Icon=continuum-calendar\n\
Terminal=false\n\
Categories=Office;Calendar;\n\
StartupWMClass=org.continuumcalendar.app\n\
SingleMainWindow=true\n\
MimeType=text/calendar;application/ics;x-scheme-handler/webcal;x-scheme-handler/webcals;\n\
StartupNotify=true\n"
    )
}

fn run_checked(cmd: &str, args: &[&str]) -> Result<(), String> {
    let status = Command::new(cmd)
        .args(args)
        .status()
        .map_err(|e| format!("Failed to run {cmd}: {e}"))?;
    if status.success() {
        Ok(())
    } else {
        Err(format!("{cmd} exited with {status}"))
    }
}

fn claim_mimes(desktop_id: &str) -> Result<Vec<String>, String> {
    for mime in MIME_TYPES {
        run_checked("xdg-mime", &["default", desktop_id, mime])?;
    }
    let mut ok = Vec::new();
    for mime in MIME_TYPES {
        let out = Command::new("xdg-mime")
            .args(["query", "default", mime])
            .output()
            .map_err(|e| e.to_string())?;
        let got = String::from_utf8_lossy(&out.stdout).trim().to_string();
        ok.push(format!("{mime} → {got}"));
    }
    Ok(ok)
}

/// Claim calendar MIME + webcal handlers. Uses the `.deb` desktop file when present.
#[tauri::command]
pub fn claim_default_calendar() -> Result<String, String> {
    #[cfg(target_os = "linux")]
    {
        if let Some(id) = packaged_desktop_id() {
            let leftover = applications_dir().join(FALLBACK_DESKTOP_ID);
            let _ = fs::remove_file(&leftover);
            let ok = claim_mimes(id)?;
            return Ok(format!(
                "Continuum is the default calendar app.\n{}\nDesktop: /usr/share/applications/{id}",
                ok.join("\n")
            ));
        }
        let dir = applications_dir();
        fs::create_dir_all(&dir).map_err(|e| format!("Could not create applications dir: {e}"))?;
        let path = dir.join(FALLBACK_DESKTOP_ID);
        let exec = std::env::current_exe().map_err(|e| e.to_string())?;
        fs::write(&path, desktop_file_contents(&exec))
            .map_err(|e| format!("Could not write {FALLBACK_DESKTOP_ID}: {e}"))?;
        let _ = run_checked("update-desktop-database", &[dir.to_str().unwrap_or(".")]);
        let ok = claim_mimes(FALLBACK_DESKTOP_ID)?;
        Ok(format!(
            "Continuum is the default calendar app.\n{}\nDesktop: {}",
            ok.join("\n"),
            path.display()
        ))
    }
    #[cfg(target_os = "windows")]
    {
        Ok(
            "On Windows, Continuum registers .ics via the installer. \
Set Continuum as default under Settings → Apps → Default apps → Calendar (or choose Continuum for .ics)."
                .into(),
        )
    }
    #[cfg(not(any(target_os = "linux", target_os = "windows")))]
    {
        Err("Claiming the default calendar app is only supported on Linux and Windows.".into())
    }
}

/// True when text/calendar already points at Continuum.
#[tauri::command]
pub fn is_default_calendar() -> Result<bool, String> {
    #[cfg(target_os = "linux")]
    {
        let out = Command::new("xdg-mime")
            .args(["query", "default", "text/calendar"])
            .output()
            .map_err(|e| e.to_string())?;
        let got = String::from_utf8_lossy(&out.stdout).trim().to_string();
        Ok(PACKAGED_DESKTOP_IDS.contains(&got.as_str()) || got == FALLBACK_DESKTOP_ID)
    }
    #[cfg(not(target_os = "linux"))]
    {
        Ok(false)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn desktop_entry_lists_webcal_and_percent_u() {
        let body = desktop_file_contents(Path::new("/usr/bin/continuum-calendar"));
        assert!(body.contains("%u"));
        assert!(body.contains("x-scheme-handler/webcal"));
        assert!(body.contains("text/calendar"));
        assert!(body.contains("SingleMainWindow=true"));
    }
}
