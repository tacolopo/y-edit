import axios from 'axios';
import { handleHttpError } from '@app/services/httpErrorHandler';
import { setupApiInterceptors } from '@app/services/apiClientSetup';
import { getApiBaseUrl } from '@app/services/apiClientConfig';

// Create axios instance with default config
const apiClient = axios.create({
  responseType: 'json',
  withCredentials: true,
});

// Resolve base URL dynamically on every request so that the Tauri desktop
// backend port (set asynchronously in index.tsx) is always picked up, even
// when this module is evaluated before window.STIRLING_PDF_API_BASE_URL is set.
apiClient.interceptors.request.use((config) => {
  if (!config.baseURL) {
    config.baseURL = getApiBaseUrl();
  }
  return config;
});

// Setup interceptors (core does nothing, proprietary adds JWT auth)
setupApiInterceptors(apiClient);

// ---------- Install error interceptor ----------
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    // Normalize Blob error data so all downstream handlers can read it.
    // Tool endpoints use responseType: 'blob', so error bodies arrive as Blob.
    if (error?.response?.data && typeof error.response.data?.text === 'function') {
      try {
        const text = await error.response.data.text();
        try { error.response.data = JSON.parse(text); } catch { error.response.data = text; }
      } catch { /* leave as-is */ }
    }
    await handleHttpError(error); // Handle error (shows toast unless suppressed)
    return Promise.reject(error);
  }
);


// ---------- Exports ----------
export default apiClient;
