/**
 * Get the base URL for API requests.
 *
 * Priority:
 * 1. window.STIRLING_PDF_API_BASE_URL (runtime override — set by Y-Edit's initBackendUrl)
 * 2. import.meta.env.VITE_API_BASE_URL (build-time env var)
 * 3. '/' (relative path - works for same-origin deployments)
 *
 * This function is called per-request via an axios interceptor so that
 * the dynamically discovered backend port is always picked up.
 */
export function getApiBaseUrl(): string {
  // Runtime override (set by initBackendUrl.ts after discovering the backend port)
  if (typeof window !== 'undefined' && (window as any).STIRLING_PDF_API_BASE_URL) {
    return (window as any).STIRLING_PDF_API_BASE_URL;
  }

  // Build-time env var
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL;
  }

  // Fallback to relative path
  return '/';
}
