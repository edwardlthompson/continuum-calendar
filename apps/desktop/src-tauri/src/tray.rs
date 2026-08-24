use std::sync::atomic::{AtomicBool, Ordering};
use tauri::image::Image;
use tauri::menu::{MenuBuilder, MenuItemBuilder};
use tauri::tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent};
use tauri::{AppHandle, Emitter, Manager};

static ICON_LOCKED: AtomicBool = AtomicBool::new(false);

const TRAY_PNG: &[u8] = include_bytes!("../icons/icon.png");

fn write_status(app: &AppHandle, msg: &str) {
    if let Ok(dir) = app.path().app_log_dir() {
        let _ = std::fs::create_dir_all(&dir);
        let _ = std::fs::write(dir.join("tray-status.txt"), msg);
    }
}

pub(crate) fn write_install(app: &AppHandle, msg: &str) {
    if let Ok(dir) = app.path().app_log_dir() {
        let _ = std::fs::create_dir_all(&dir);
        let _ = std::fs::write(dir.join("tray-install.txt"), msg);
    }
}

fn fallback_icon(app: &AppHandle) -> Option<Image<'_>> {
    Image::from_bytes(TRAY_PNG)
        .ok()
        .or_else(|| app.default_window_icon().cloned())
}

fn build_tray(app: &AppHandle, icon: Option<Image<'_>>, tip: &str) -> Result<(), String> {
    let show = MenuItemBuilder::with_id("show", "Open Continuum")
        .build(app)
        .map_err(|e| e.to_string())?;
    let new_ev = MenuItemBuilder::with_id("new", "New event")
        .build(app)
        .map_err(|e| e.to_string())?;
    let quit = MenuItemBuilder::with_id("quit", "Quit")
        .build(app)
        .map_err(|e| e.to_string())?;
    let menu = MenuBuilder::new(app)
        .item(&show)
        .item(&new_ev)
        .separator()
        .item(&quit)
        .build()
        .map_err(|e| e.to_string())?;
    let mut builder = TrayIconBuilder::with_id("main")
        .menu(&menu)
        .tooltip(tip)
        .show_menu_on_left_click(false)
        .on_menu_event(|app, event| match event.id().as_ref() {
            "show" => crate::window_behavior::show_main(app),
            "new" => {
                let _ = app.emit("tray-new-event", ());
            }
            "quit" => crate::window_behavior::request_exit(app),
            _ => {}
        })
        .on_tray_icon_event(|tray, event| {
            if let TrayIconEvent::Click {
                button: MouseButton::Left,
                button_state: MouseButtonState::Up,
                ..
            } = event
            {
                crate::window_behavior::show_main(tray.app_handle());
            }
        });
    if let Some(icon) = icon {
        builder = builder.icon(icon);
    } else if let Ok(icon) = Image::from_bytes(TRAY_PNG) {
        builder = builder.icon(icon);
    }
    builder.build(app).map_err(|e| e.to_string())?;
    Ok(())
}

pub fn install(app: &tauri::App) -> Result<(), Box<dyn std::error::Error>> {
    let _ = crate::tray_promote::sweep_stale_own_keys();
    crate::tray_promote::capture_baseline();
    build_tray(&app.handle(), fallback_icon(&app.handle()), "Continuum Calendar")?;
    write_install(&app.handle(), "tauri-tray");
    write_status(&app.handle(), "tauri-tray installed");
    crate::tray_promote::promote_later(app.handle().clone());
    Ok(())
}

fn apply_badge(app: &AppHandle, count: u32, png: Vec<u8>, overlay: Vec<u8>, tip: String) {
    let mut update = String::from("keep");
    if let Some(tray) = app.tray_by_id("main") {
        let _ = tray.set_tooltip(Some(tip.as_str()));
        if !png.is_empty() && !ICON_LOCKED.load(Ordering::SeqCst) {
            if let Ok(img) = Image::from_bytes(&png) {
                match tray.set_icon(Some(img)) {
                    Ok(()) => update = "badge-icon".into(),
                    Err(e) => {
                        ICON_LOCKED.store(true, Ordering::SeqCst);
                        update = format!("bright-keep ({e})");
                    }
                }
            }
        }
    } else {
        update = format!("missing {:?}", build_tray(app, fallback_icon(app), &tip).err());
    }
    if let Some(win) = app.get_webview_window("main") {
        if count > 0 && !overlay.is_empty() {
            if let Ok(img) = Image::from_bytes(&overlay) {
                let _ = win.set_overlay_icon(Some(img));
            }
        } else {
            let _ = win.set_overlay_icon(None);
        }
    }
    let promote = crate::tray_promote::promote_own_icon();
    write_status(
        app,
        &format!(
            "badge count={count} png={} overlay={} {update} promote={promote}",
            png.len(),
            overlay.len()
        ),
    );
}

#[tauri::command]
pub fn set_day_badge(
    app: AppHandle,
    count: u32,
    png: Vec<u8>,
    overlay: Vec<u8>,
    summary: String,
) -> Result<(), String> {
    let tip = if summary.is_empty() {
        format!("{count} remaining today")
    } else {
        summary
    };
    let handle = app.clone();
    app.run_on_main_thread(move || apply_badge(&handle, count, png, overlay, tip))
        .map_err(|e| e.to_string())
}
