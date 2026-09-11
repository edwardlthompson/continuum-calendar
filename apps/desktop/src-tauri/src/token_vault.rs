//! App-config token file (mode 0600 on Unix). Not a cloud keychain — local disk only.

use std::fs;
use std::io::ErrorKind;
use std::path::PathBuf;

use tauri::{AppHandle, Manager};

const FILE: &str = "google-tokens.json";
const MAX_BYTES: usize = 16_384;

fn vault_path(app: &AppHandle) -> Result<PathBuf, String> {
    let dir = app
        .path()
        .app_config_dir()
        .map_err(|e| format!("config dir: {e}"))?;
    fs::create_dir_all(&dir).map_err(|e| format!("create config dir: {e}"))?;
    Ok(dir.join(FILE))
}

fn lock_owner_rw(path: &std::path::Path) -> Result<(), String> {
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        let mut perms = fs::metadata(path)
            .map_err(|e| format!("token perms: {e}"))?
            .permissions();
        perms.set_mode(0o600);
        fs::set_permissions(path, perms).map_err(|e| format!("token chmod: {e}"))?;
    }
    let _ = path;
    Ok(())
}

#[tauri::command]
pub fn save_google_tokens(app: AppHandle, json: Option<String>) -> Result<(), String> {
    let path = vault_path(&app)?;
    match json {
        None => {
            if let Err(e) = fs::remove_file(&path) {
                if e.kind() != ErrorKind::NotFound {
                    return Err(format!("clear tokens: {e}"));
                }
            }
            Ok(())
        }
        Some(body) => {
            if body.len() > MAX_BYTES {
                return Err("token blob too large".into());
            }
            serde_json::from_str::<serde_json::Value>(&body)
                .map_err(|_| "token blob is not JSON".to_string())?;
            let tmp = path.with_extension("json.tmp");
            fs::write(&tmp, body.as_bytes()).map_err(|e| format!("write tokens: {e}"))?;
            lock_owner_rw(&tmp)?;
            fs::rename(&tmp, &path).map_err(|e| format!("commit tokens: {e}"))?;
            lock_owner_rw(&path)?;
            Ok(())
        }
    }
}

#[tauri::command]
pub fn load_google_tokens(app: AppHandle) -> Result<Option<String>, String> {
    let path = vault_path(&app)?;
    match fs::read_to_string(&path) {
        Ok(s) if s.len() > MAX_BYTES => Err("token blob too large".into()),
        Ok(s) => Ok(Some(s)),
        Err(e) if e.kind() == ErrorKind::NotFound => Ok(None),
        Err(e) => Err(format!("read tokens: {e}")),
    }
}
