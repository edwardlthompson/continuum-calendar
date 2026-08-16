use std::io::{Read, Write};
use std::net::TcpListener;
use std::thread;
use std::time::Duration;

use serde::Serialize;
use tauri::{AppHandle, Emitter, Manager};

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct OAuthCallbackPayload {
    code: String,
    state: String,
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct OAuthErrorPayload {
    error: String,
    error_description: String,
}

/// Bind a loopback listener. Google redirects here; we emit `oauth-callback` or `oauth-error`.
#[tauri::command]
pub fn start_oauth_loopback(app: AppHandle) -> Result<u16, String> {
    let listener = TcpListener::bind("127.0.0.1:0").map_err(|e| e.to_string())?;
    listener.set_nonblocking(false).map_err(|e| e.to_string())?;
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
                    if handle_oauth_conn(&app, &mut stream) {
                        break;
                    }
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

/// POST the token form from native code so WebView CORS cannot block Google.
#[tauri::command]
pub fn google_oauth_token(body: String) -> Result<String, String> {
    if body.is_empty() || body.len() > 16_384 {
        return Err("Token request is empty or too large".into());
    }
    let resp = ureq::post("https://oauth2.googleapis.com/token")
        .set("Content-Type", "application/x-www-form-urlencoded")
        .timeout(Duration::from_secs(30))
        .send_string(&body);
    match resp {
        Ok(r) => r.into_string().map_err(|e| format!("Token exchange failed: {e}")),
        Err(ureq::Error::Status(code, r)) => {
            let text = r.into_string().unwrap_or_default();
            Err(summarize_token_error(code, &text))
        }
        Err(e) => Err(format!("Token exchange failed: {e}")),
    }
}

fn handle_oauth_conn(app: &AppHandle, stream: &mut std::net::TcpStream) -> bool {
    let _ = stream.set_read_timeout(Some(Duration::from_secs(5)));
    let mut buf = [0u8; 8192];
    let n = stream.read(&mut buf).unwrap_or(0);
    let req = String::from_utf8_lossy(&buf[..n]);
    let path = req
        .lines()
        .next()
        .and_then(|line| line.split_whitespace().nth(1))
        .unwrap_or("/");

    if path.starts_with("/favicon") || !path.contains('?') {
        let _ = write_http(stream, 204, "");
        return false;
    }
    if let Some(err) = parse_oauth_error(path) {
        let body = format!(
            "<!DOCTYPE html><html><body style=\"font-family:system-ui;padding:2rem\">\
            <p>Google sign-in did not finish ({}) — return to Continuum Calendar.</p>\
            </body></html>",
            html_escape(&err.error)
        );
        let _ = write_http(stream, 200, &body);
        write_last_oauth_error(app, &format!("{} — {}", err.error, err.error_description));
        if let Err(e) = app.emit("oauth-error", err) {
            log::error!("failed to emit oauth-error: {e}");
        }
        return true;
    }
    if let Some(payload) = parse_oauth_query(path) {
        let body = "<!DOCTYPE html><html><body style=\"font-family:system-ui;padding:2rem\">\
            <p>Signed in — you can close this tab and return to Continuum Calendar.</p>\
            </body></html>";
        let _ = write_http(stream, 200, body);
        if let Err(e) = app.emit("oauth-callback", payload) {
            log::error!("failed to emit oauth-callback: {e}");
        }
        if let Some(win) = app.get_webview_window("main") {
            let _ = win.set_focus();
        }
        return true;
    }
    let _ = write_http(stream, 204, "");
    false
}

fn write_last_oauth_error(app: &AppHandle, msg: &str) {
    if let Ok(dir) = app.path().app_log_dir() {
        let _ = std::fs::create_dir_all(&dir);
        let _ = std::fs::write(dir.join("oauth-last-error.txt"), msg);
    }
}

fn summarize_token_error(code: u16, text: &str) -> String {
    if let Ok(v) = serde_json::from_str::<serde_json::Value>(text) {
        let err = v.get("error").and_then(|x| x.as_str()).unwrap_or("");
        let desc = v
            .get("error_description")
            .and_then(|x| x.as_str())
            .unwrap_or("");
        let parts = [err, desc].into_iter().filter(|s| !s.is_empty());
        let joined = parts.collect::<Vec<_>>().join(" — ");
        if !joined.is_empty() {
            return format!("Token exchange failed ({code}): {joined}");
        }
    }
    format!("Token exchange failed ({code})")
}

fn write_http(stream: &mut std::net::TcpStream, status: u16, body: &str) -> std::io::Result<()> {
    let reason = match status {
        200 => "OK",
        204 => "No Content",
        _ => "OK",
    };
    let resp = format!(
        "HTTP/1.1 {status} {reason}\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{body}",
        body.len()
    );
    stream.write_all(resp.as_bytes())?;
    stream.flush()
}

fn html_escape(s: &str) -> String {
    s.replace('&', "&amp;")
        .replace('<', "&lt;")
        .replace('>', "&gt;")
}

fn parse_oauth_error(path: &str) -> Option<OAuthErrorPayload> {
    let q = path.split('?').nth(1)?;
    let mut error: Option<String> = None;
    let mut desc = String::new();
    for pair in q.split('&') {
        let mut parts = pair.splitn(2, '=');
        let k = parts.next()?;
        let v = urlencoding_decode(parts.next().unwrap_or(""));
        match k {
            "error" => error = Some(v),
            "error_description" => desc = v,
            _ => {}
        }
    }
    Some(OAuthErrorPayload {
        error: error?,
        error_description: desc,
    })
}

fn parse_oauth_query(path: &str) -> Option<OAuthCallbackPayload> {
    let q = path.split('?').nth(1)?;
    let mut code: Option<String> = None;
    let mut state: Option<String> = None;
    for pair in q.split('&') {
        let mut parts = pair.splitn(2, '=');
        let k = parts.next()?;
        let v = urlencoding_decode(parts.next().unwrap_or(""));
        match k {
            "code" => code = Some(v),
            "state" => state = Some(v),
            _ => {}
        }
    }
    Some(OAuthCallbackPayload {
        code: code?,
        state: state?,
    })
}

fn urlencoding_decode(s: &str) -> String {
    let mut out = Vec::with_capacity(s.len());
    let b = s.as_bytes();
    let mut i = 0;
    while i < b.len() {
        match b[i] {
            b'%' if i + 2 < b.len() => {
                if let Ok(v) = u8::from_str_radix(&s[i + 1..i + 3], 16) {
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
    String::from_utf8_lossy(&out).into_owned()
}
