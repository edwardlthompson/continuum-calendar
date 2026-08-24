//! When a debug / cargo-debug EXE opens without Vite, replace the Edge
//! localhost error with a Continuum message (KB-035).
use std::net::{SocketAddr, TcpStream};
use std::time::Duration;
use tauri::{App, AppHandle, Manager, Url};

const FALLBACK_HTML: &str = r#"<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8"/><title>Continuum Calendar</title>
<style>
  body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;
    font-family:Segoe UI,system-ui,sans-serif;background:#0B1220;color:#E8EEF7;}
  main{max-width:28rem;padding:2rem;text-align:center;}
  h1{font-size:1.35rem;margin:0 0 .75rem;color:#00E5FF;}
  p{margin:.5rem 0;line-height:1.45;color:#B8C4D6;}
  code{font-size:.9em;color:#F5F7FA;background:#1A2438;padding:.15rem .4rem;border-radius:4px;}
</style></head><body><main>
  <h1>Developer build</h1>
  <p>This EXE loads the Vite UI at <code>localhost:5173</code>, which is not running.</p>
  <p>From <code>apps/desktop</code> run <code>npm run tauri:dev</code>, or install a release build with <code>npm run install:local</code>.</p>
</main></body></html>"#;

fn vite_listening() -> bool {
    let addr = SocketAddr::from(([127, 0, 0, 1], 5173));
    TcpStream::connect_timeout(&addr, Duration::from_millis(300)).is_ok()
}

fn is_dev_exe() -> bool {
    if cfg!(debug_assertions) {
        return true;
    }
    std::env::current_exe()
        .map(|p| crate::autostart_guard::is_cargo_debug_exe(&p))
        .unwrap_or(false)
}

fn show_fallback(app: &AppHandle) {
    let Some(win) = app.get_webview_window("main") else {
        return;
    };
    let encoded = urlencoding_minimal(FALLBACK_HTML);
    let href = format!("data:text/html;charset=utf-8,{encoded}");
    match Url::parse(&href) {
        Ok(url) => {
            if let Err(e) = win.navigate(url) {
                log::warn!("dev_ui_guard: navigate failed ({e})");
                let _ = win.eval(format!(
                    "document.open();document.write({});document.close();",
                    serde_json::to_string(FALLBACK_HTML).unwrap_or_else(|_| "''".into())
                ));
            }
        }
        Err(e) => log::warn!("dev_ui_guard: bad data url ({e})"),
    }
}

/// If this is a debug binary and Vite is down, show Continuum HTML instead of Edge.
pub fn apply_if_needed(app: &App) {
    if !is_dev_exe() {
        return;
    }
    if vite_listening() {
        return;
    }
    let handle = app.handle().clone();
    show_fallback(&handle);
    // Edge may paint ERR_CONNECTION_REFUSED after first navigation — replace again.
    std::thread::spawn(move || {
        std::thread::sleep(Duration::from_millis(900));
        if vite_listening() {
            return;
        }
        let h = handle.clone();
        let _ = handle.run_on_main_thread(move || show_fallback(&h));
    });
}

fn urlencoding_minimal(s: &str) -> String {
    let mut out = String::with_capacity(s.len() * 2);
    for b in s.bytes() {
        match b {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                out.push(b as char);
            }
            b' ' => out.push_str("%20"),
            _ => out.push_str(&format!("%{b:02X}")),
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::urlencoding_minimal;

    #[test]
    fn encodes_spaces_and_hash() {
        assert_eq!(urlencoding_minimal("a b"), "a%20b");
        assert!(urlencoding_minimal("<h1>").contains("%3C"));
    }
}
