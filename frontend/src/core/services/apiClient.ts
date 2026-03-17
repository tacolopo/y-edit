import axios from 'axios';
import { handleHttpError } from '@app/services/httpErrorHandler';
import { setupApiInterceptors } from '@app/services/apiClientSetup';
import { getApiBaseUrl } from '@app/services/apiClientConfig';

// Create axios instance — baseURL is set dynamically per request
const apiClient = axios.create({
  baseURL: getApiBaseUrl(),
  responseType: 'json',
  withCredentials: true,
});

// Dynamically update baseURL on every request so the runtime-discovered
// backend port (set by initBackendUrl.ts) is always used.
apiClient.interceptors.request.use((config) => {
  config.baseURL = getApiBaseUrl();
  return config;
});

// Setup interceptors (core does nothing, proprietary adds JWT auth)
setupApiInterceptors(apiClient);

// ---------- Install error interceptor ----------
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    await handleHttpError(error); // Handle error (shows toast unless suppressed)
    return Promise.reject(error);
  }
);


// ---------- Exports ----------
export default apiClient;
