use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Mutex;
use tauri::{AppHandle, Manager, WebviewUrl, WebviewWindowBuilder, Window, WindowEvent};

static ALLOW_EXIT: AtomicBool = AtomicBool::new(false);

#[derive(Clone, Copy)]
struct Prefs {
    close_to_tray: bool,
    minimize_to_tray: bool,
}

static PREFS: Mutex<Prefs> = Mutex::new(Prefs {
    close_to_tray: true,
    minimize_to_tray: false,
});

fn prefs() -> Prefs {
    PREFS.lock().map(|g| *g).unwrap_or(Prefs {
        close_to_tray: true,
        minimize_to_tray: false,
    })
}

pub fn request_exit(app: &AppHandle) {
    ALLOW_EXIT.store(true, Ordering::SeqCst);
    app.exit(0);
}

pub fn should_prevent_exit() -> bool {
    !ALLOW_EXIT.load(Ordering::SeqCst)
}

pub fn show_main(app: &AppHandle) {
    if app.get_webview_window("main").is_none() {
        let _ = WebviewWindowBuilder::new(app, "main", WebviewUrl::App("index.html".into()))
            .title("Continuum Calendar")
            .inner_size(1280.0, 800.0)
            .build();
    }
    if let Some(w) = app.get_webview_window("main") {
        let _ = w.set_skip_taskbar(false);
        let _ = w.unminimize();
        let _ = w.show();
        let _ = w.set_focus();
    }
}

fn hide_to_tray(window: &Window) {
    let _ = window.hide();
    let _ = window.set_skip_taskbar(true);
}

pub fn on_window_event(window: &Window, event: &WindowEvent) {
    match event {
        WindowEvent::CloseRequested { api, .. } => {
            if prefs().close_to_tray {
                api.prevent_close();
                hide_to_tray(window);
            }
        }
        WindowEvent::Resized(_) => {
            if prefs().minimize_to_tray && window.is_minimized().unwrap_or(false) {
                hide_to_tray(window);
            }
        }
        _ => {}
    }
}

#[tauri::command]
pub fn set_window_behavior(close_to_tray: bool, minimize_to_tray: bool) {
    if let Ok(mut g) = PREFS.lock() {
        *g = Prefs {
            close_to_tray,
            minimize_to_tray,
        };
    }
}
