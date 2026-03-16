pub mod backend;
pub mod files;
pub mod default_app;
pub mod platform;
pub mod print;

pub use backend::{cleanup_backend, get_backend_port, start_backend};
pub use files::{add_opened_file, clear_opened_files, get_opened_files, pop_opened_files};
pub use default_app::{is_default_pdf_handler, set_as_default_pdf_handler};
pub use platform::get_desktop_os;
pub use print::print_pdf_file_native;
