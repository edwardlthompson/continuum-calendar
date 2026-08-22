use tauri::image::Image;
use tauri::menu::{MenuBuilder, MenuItemBuilder};
use tauri::tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent};
use tauri::{AppHandle, Emitter, Manager};

pub fn install(app: &tauri::App) -> Result<(), Box<dyn std::error::Error>> {
    let show = MenuItemBuilder::with_id("show", "Open Continuum").build(app)?;
    let new_ev = MenuItemBuilder::with_id("new", "New event").build(app)?;
    let quit = MenuItemBuilder::with_id("quit", "Quit").build(app)?;
    let menu = MenuBuilder::new(app)
        .item(&show)
        .item(&new_ev)
        .separator()
        .item(&quit)
        .build()?;
    let icon = app.default_window_icon().cloned();
    let mut builder = TrayIconBuilder::with_id("main")
        .menu(&menu)
        .tooltip("Continuum Calendar")
        .on_menu_event(|app, event| match event.id().as_ref() {
            "show" => crate::window_behavior::show_main(app),
            "new" => {
                let _ = app.emit("tray-new-event", ());
            }
            "quit" => app.exit(0),
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
    }
    builder.build(app)?;
    Ok(())
}

#[tauri::command]
pub fn set_day_badge(app: AppHandle, count: u32, png: Vec<u8>, summary: String) -> Result<(), String> {
    if let Some(tray) = app.tray_by_id("main") {
        let tip = if summary.is_empty() {
            format!("{count} remaining today")
        } else {
            summary
        };
        tray.set_tooltip(Some(&tip)).map_err(|e| e.to_string())?;
    }
    if let Some(win) = app.get_webview_window("main") {
        if count == 0 || png.is_empty() {
            let _ = win.set_overlay_icon(None);
        } else if let Ok(img) = Image::from_bytes(&png) {
            let _ = win.set_overlay_icon(Some(img));
        }
    }
    Ok(())
}
