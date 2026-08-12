use std::io::{Read, Write};
use std::net::TcpListener;
use std::path::Path;
use std::sync::Mutex;
use std::thread;
use std::time::Duration;

use serde::Serialize;
use tauri::{AppHandle, Emitter, Manager};

static PENDING_OPENS: Mutex<Vec<String>> = Mutex::new(Vec::new());

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct OAuthCallbackPayload {
    code: String,
    state: String,
}

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

/// Bind a one-shot loopback HTTP listener and return its port.
/// When Google redirects here, emit `oauth-callback` with `{ code, state }`.
#[tauri::command]
fn start_oauth_loopback(app: AppHandle) -> Result<u16, String> {
    let listener = TcpListener::bind("127.0.0.1:0").map_err(|e| e.to_string())?;
    listener
        .set_nonblocking(false)
        .map_err(|e| e.to_string())?;
    let port = listener.local_addr().map_err(|e| e.to_string())?.port();
    let _ = listener.set_nonblocking(true);

    thread::spawn(move || {
        let deadline = std::time::Instant::now() + Duration::from_secs(300);
        loop {
            if std::time::Instant::now() > deadline {
                log::warn!("oauth loopback timed out waiting for redirect");
                break;
            }
            match listener.accept() {
                Ok((mut stream, _)) => {
                    let _ = stream.set_read_timeout(Some(Duration::from_secs(5)));
                    let mut buf = [0u8; 8192];
                    let n = stream.read(&mut buf).unwrap_or(0);
                    let req = String::from_utf8_lossy(&buf[..n]);
                    let path = req
                        .lines()
                        .next()
                        .and_then(|line| line.split_whitespace().nth(1))
                        .unwrap_or("/");

                    let body = "<!DOCTYPE html><html><body style=\"font-family:system-ui;padding:2rem\">\
                        <p>Signed in — you can close this tab and return to Continuum Calendar.</p>\
                        </body></html>";
                    let resp = format!(
                        "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{}",
                        body.len(),
                        body
                    );
                    let _ = stream.write_all(resp.as_bytes());
                    let _ = stream.flush();

                    if let Some(payload) = parse_oauth_query(path) {
                        if let Err(e) = app.emit("oauth-callback", payload) {
                            log::error!("failed to emit oauth-callback: {e}");
                        }
                        if let Some(win) = app.get_webview_window("main") {
                            let _ = win.set_focus();
                        }
                    }
                    break;
                }
                Err(ref e) if e.kind() == std::io::ErrorKind::WouldBlock => {
                    thread::sleep(Duration::from_millis(50));
                }
                Err(e) => {
                    log::error!("oauth loopback accept error: {e}");
                    break;
                }
            }
        }
    });

    Ok(port)
}

#[tauri::command]
fn take_pending_open_paths() -> Vec<String> {
    PENDING_OPENS.lock().map(|mut g| std::mem::take(&mut *g)).unwrap_or_default()
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

fn parse_oauth_query(path: &str) -> Option<OAuthCallbackPayload> {
    let q = path.split('?').nth(1)?;
    let mut code: Option<String> = None;
    let mut state: Option<String> = None;
    for pair in q.split('&') {
        let mut parts = pair.splitn(2, '=');
        let k = parts.next()?;
        let v = parts.next().unwrap_or("");
        let decoded = urlencoding_decode(v);
        match k {
            "code" => code = Some(decoded),
            "state" => state = Some(decoded),
            _ => {}
        }
    }
    Some(OAuthCallbackPayload {
        code: code?,
        state: state?,
    })
}

fn urlencoding_decode(s: &str) -> String {
    let bytes: Vec<u8> = {
        let mut out = Vec::with_capacity(s.len());
        let b = s.as_bytes();
        let mut i = 0;
        while i < b.len() {
            match b[i] {
                b'%' if i + 2 < b.len() => {
                    let hex = &s[i + 1..i + 3];
                    if let Ok(v) = u8::from_str_radix(hex, 16) {
                        out.push(v);
                        i += 3;
                        continue;
                    }
                    out.push(b[i]);
                    i += 1;
                }
                b'+' => {
                    out.push(b' ');
                    i += 1;
                }
                c => {
                    out.push(c);
                    i += 1;
                }
            }
        }
        out
    };
    String::from_utf8_lossy(&bytes).into_owned()
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    // Capture launch argv (Windows file association / URL protocol) before UI starts.
    for arg in std::env::args().skip(1) {
        if let Some((_ev, payload)) = classify_open_arg(&arg) {
            push_pending_open(payload);
        }
    }

    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![
            start_oauth_loopback,
            take_pending_open_paths,
            read_text_file_limited
        ])
        .setup(|app| {
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
