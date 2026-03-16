/**
 * Y-Edit Desktop: Discover the local backend port from Tauri and configure
 * the API base URL so all fetch/axios calls reach the bundled Spring Boot server.
 *
 * This must run BEFORE React renders, since API calls happen during component mounting.
 */

declare global {
  interface Window {
    STIRLING_PDF_API_BASE_URL?: string;
    __TAURI_INTERNALS__?: unknown;
  }
}

async function initBackendUrl(): Promise<void> {
  // Only in Tauri desktop mode
  if (!window.__TAURI_INTERNALS__) return;

  const { invoke } = await import('@tauri-apps/api/core');

  // Poll for backend port (backend may still be starting)
  let port: number | null = null;
  for (let i = 0; i < 120; i++) {
    port = await invoke<number | null>('get_backend_port');
    if (port) break;
    await new Promise(resolve => setTimeout(resolve, 500));
  }

  if (port) {
    window.STIRLING_PDF_API_BASE_URL = `http://localhost:${port}`;
    console.log(`Y-Edit: Backend discovered at http://localhost:${port}`);
  } else {
    console.error('Y-Edit: Failed to discover backend port after 60s');
  }
}

export default initBackendUrl;
