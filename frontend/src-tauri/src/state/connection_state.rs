use std::sync::atomic::{AtomicBool, AtomicU16, Ordering};

/// Simplified app state for Y-Edit desktop mode (local backend only)
pub struct AppState {
    pub backend_ready: AtomicBool,
    pub backend_port: AtomicU16,
}

impl Default for AppState {
    fn default() -> Self {
        Self {
            backend_ready: AtomicBool::new(false),
            backend_port: AtomicU16::new(0),
        }
    }
}

impl AppState {
    pub fn set_port(&self, port: u16) {
        self.backend_port.store(port, Ordering::SeqCst);
        self.backend_ready.store(true, Ordering::SeqCst);
    }

    pub fn get_port(&self) -> Option<u16> {
        if self.backend_ready.load(Ordering::SeqCst) {
            Some(self.backend_port.load(Ordering::SeqCst))
        } else {
            None
        }
    }
}
