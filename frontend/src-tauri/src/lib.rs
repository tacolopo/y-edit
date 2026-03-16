use tauri::{Emitter, Manager, RunEvent, WindowEvent};

mod utils;
mod commands;
mod state;

use commands::{
    add_opened_file,
    cleanup_backend,
    clear_opened_files,
    is_default_pdf_handler,
    get_backend_port,
    get_opened_files,
    pop_opened_files,
    set_as_default_pdf_handler,
    get_desktop_os,
    print_pdf_file_native,
    start_backend,
};
use state::connection_state::AppState;
use utils::{add_log, get_tauri_logs};

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(
            tauri_plugin_log::Builder::new()
                .level(log::LevelFilter::Info)
                .build()
        )
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_store::Builder::new().build())
        .plugin(tauri_plugin_notification::init())
        .plugin(tauri_plugin_window_state::Builder::default().build())
        .manage(AppState::default())
        .plugin(tauri_plugin_single_instance::init(|app, args, _cwd| {
            // Second instance detected — forward files to existing instance
            add_log(format!("Second instance detected with args: {:?}", args));

            for arg in args.iter().skip(1) {
                if std::path::Path::new(arg).exists() {
                    add_log(format!("Forwarding file to existing instance: {}", arg));
                    add_opened_file(arg.clone());

                    if let Some(window) = app.get_webview_window("main") {
                        let _ = window.set_focus();
                        let _ = window.unminimize();
                    }
                }
            }

            let _ = app.emit("files-changed", ());
        }))
        .setup(|app| {
            add_log("Y-Edit app setup started".to_string());

            // Process command line arguments on first launch
            let args: Vec<String> = std::env::args().collect();
            for arg in args.iter().skip(1) {
                if std::path::Path::new(arg).exists() {
                    add_log(format!("Initial file from command line: {}", arg));
                    add_opened_file(arg.clone());
                }
            }

            // Start backend immediately
            let app_handle = app.handle().clone();
            tauri::async_runtime::spawn(async move {
                add_log("Starting bundled backend...".to_string());
                let app_state = app_handle.state::<AppState>();
                if let Err(e) = commands::backend::start_backend(app_handle.clone(), app_state).await {
                    add_log(format!("Backend start failed: {}", e));
                }
            });

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            start_backend,
            get_backend_port,
            get_opened_files,
            pop_opened_files,
            clear_opened_files,
            get_tauri_logs,
            is_default_pdf_handler,
            set_as_default_pdf_handler,
            get_desktop_os,
            print_pdf_file_native,
        ])
        .build(tauri::generate_context!())
        .expect("error while building tauri application")
        .run(|app_handle, event| {
            match event {
                RunEvent::ExitRequested { .. } => {
                    add_log("App exit requested, cleaning up...".to_string());
                    cleanup_backend();
                    app_handle.cleanup_before_exit();
                }
                RunEvent::WindowEvent { event: WindowEvent::DragDrop(drag_drop_event), .. } => {
                    use tauri::DragDropEvent;
                    if let DragDropEvent::Drop { paths, .. } = drag_drop_event {
                        add_log(format!("Files dropped: {:?}", paths));
                        let mut added_files = false;

                        for path in paths {
                            if let Some(path_str) = path.to_str() {
                                add_opened_file(path_str.to_string());
                                added_files = true;
                            }
                        }

                        if added_files {
                            let _ = app_handle.emit("files-changed", ());
                        }
                    }
                }
                _ => {}
            }
        });
}
