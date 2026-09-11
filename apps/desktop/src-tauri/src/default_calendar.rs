//! Register Continuum as the default calendar handler (MIME + webcal), Linux-first.

use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;

const DESKTOP_ID: &str = "org.continuumcalendar.app.desktop";
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

fn install_bin_path() -> PathBuf {
    let share = std::env::var_os("XDG_DATA_HOME")
        .map(PathBuf::from)
        .unwrap_or_else(|| {
            dirs_fallback_home()
                .map(|h| h.join(".local/share"))
                .unwrap_or_else(|| PathBuf::from("."))
        });
    let candidate = share.join("continuum-calendar/app");
    if candidate.is_file() {
        return candidate;
    }
    let fixed = share.join("continuum-calendar/app-fixed");
    if fixed.is_file() {
        return fixed;
    }
    // Fall back to current executable (dev / sideload).
    std::env::current_exe().unwrap_or(candidate)
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
Icon=org.continuumcalendar.app\n\
Terminal=false\n\
Categories=Office;Calendar;\n\
StartupWMClass=org.continuumcalendar.app\n\
MimeType=text/calendar;application/ics;x-scheme-handler/webcal;x-scheme-handler/webcals;\n\
StartupNotify=true\n"
    )
}

fn write_desktop_file() -> Result<PathBuf, String> {
    let dir = applications_dir();
    fs::create_dir_all(&dir).map_err(|e| format!("Could not create applications dir: {e}"))?;
    let path = dir.join(DESKTOP_ID);
    let body = desktop_file_contents(&install_bin_path());
    fs::write(&path, body).map_err(|e| format!("Could not write {DESKTOP_ID}: {e}"))?;
    Ok(path)
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

/// Write/update the .desktop entry and claim calendar MIME + webcal handlers.
#[tauri::command]
pub fn claim_default_calendar() -> Result<String, String> {
    #[cfg(target_os = "linux")]
    {
        let desktop_path = write_desktop_file()?;
        let apps_dir = applications_dir();
        let _ = run_checked("update-desktop-database", &[apps_dir.to_str().unwrap_or(".")]);
        for mime in MIME_TYPES {
            run_checked("xdg-mime", &["default", DESKTOP_ID, mime])?;
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
        Ok(format!(
            "Continuum is the default calendar app.\n{}\nDesktop: {}",
            ok.join("\n"),
            desktop_path.display()
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

/// True when text/calendar (or Windows note) already points at Continuum.
#[tauri::command]
pub fn is_default_calendar() -> Result<bool, String> {
    #[cfg(target_os = "linux")]
    {
        let out = Command::new("xdg-mime")
            .args(["query", "default", "text/calendar"])
            .output()
            .map_err(|e| e.to_string())?;
        let got = String::from_utf8_lossy(&out.stdout).trim().to_string();
        Ok(got == DESKTOP_ID)
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
        let body = desktop_file_contents(Path::new("/tmp/continuum-calendar/app"));
        assert!(body.contains("%u"));
        assert!(body.contains("x-scheme-handler/webcal"));
        assert!(body.contains("text/calendar"));
    }
}
