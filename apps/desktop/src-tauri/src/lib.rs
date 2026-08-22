use std::path::Path;
use std::sync::Mutex;

mod location;
mod oauth;
mod tray;
mod window_behavior;

static PENDING_OPENS: Mutex<Vec<String>> = Mutex::new(Vec::new());

fn push_pending_open(value: String) {
    if let Ok(mut g) = PENDING_OPENS.lock() {
        g.push(value);
    }
}

fn classify_open_arg(raw: &str) -> Option<(&'static str, String)> {
    let s = raw.trim();
    if s.is_empty() {
        return None;
    }
    let lower = s.to_ascii_lowercase();
    if lower.starts_with("webcal://")
        || lower.starts_with("webcals://")
        || lower.starts_with("http://")
        || lower.starts_with("https://")
    {
        return Some(("open-calendar-url", s.to_string()));
    }
    let path = Path::new(s);
    if path
        .extension()
        .and_then(|e| e.to_str())
        .map(|e| e.eq_ignore_ascii_case("ics") || e.eq_ignore_ascii_case("ical"))
        .unwrap_or(false)
    {
        return Some(("open-ics-path", s.to_string()));
    }
    None
}

#[tauri::command]
fn take_pending_open_paths() -> Vec<String> {
    PENDING_OPENS
        .lock()
        .map(|mut g| std::mem::take(&mut *g))
        .unwrap_or_default()
}

#[tauri::command]
fn read_text_file_limited(path: String, max_bytes: usize) -> Result<String, String> {
    let p = Path::new(&path);
    let ext = p
        .extension()
        .and_then(|e| e.to_str())
        .unwrap_or("")
        .to_ascii_lowercase();
    if ext != "ics" && ext != "ical" {
        return Err("Only .ics / .ical files can be opened".into());
    }
    let meta = std::fs::metadata(p).map_err(|e| e.to_string())?;
    if meta.len() as usize > max_bytes {
        return Err(format!("File too large (max {max_bytes} bytes)"));
    }
    let bytes = std::fs::read(p).map_err(|e| e.to_string())?;
    if bytes.len() > max_bytes {
        return Err(format!("File too large (max {max_bytes} bytes)"));
    }
    String::from_utf8(bytes).map_err(|e| e.to_string())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    for arg in std::env::args().skip(1) {
        if let Some((_ev, payload)) = classify_open_arg(&arg) {
            push_pending_open(payload);
        }
    }

    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_autostart::Builder::new().build())
        .invoke_handler(tauri::generate_handler![
            oauth::start_oauth_loopback,
            oauth::google_oauth_token,
            take_pending_open_paths,
            read_text_file_limited,
            location::suggest_locations,
            tray::set_day_badge,
            window_behavior::set_window_behavior
        ])
        .on_window_event(|window, event| {
            window_behavior::on_window_event(window, event);
        })
        .setup(|app| {
            if let Err(e) = tray::install(app) {
                log::warn!("tray unavailable: {e}");
            }
            if cfg!(debug_assertions) {
                app.handle().plugin(
                    tauri_plugin_log::Builder::default()
                        .level(log::LevelFilter::Info)
                        .build(),
                )?;
            }
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
