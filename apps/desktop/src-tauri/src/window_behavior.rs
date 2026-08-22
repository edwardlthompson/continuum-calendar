use std::sync::Mutex;
use tauri::{AppHandle, Manager, Window, WindowEvent};

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

pub fn show_main(app: &AppHandle) {
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
