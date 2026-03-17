/**
 * Get the base URL for API requests.
 *
 * In Tauri desktop mode, window.STIRLING_PDF_API_BASE_URL is set to
 * http://localhost:{port} before React renders (see index.tsx).
 */
export function getApiBaseUrl(): string {
  if (typeof window !== 'undefined' && (window as any).STIRLING_PDF_API_BASE_URL) {
    return (window as any).STIRLING_PDF_API_BASE_URL;
  }

  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL;
  }

  return '/';
}
