// src/api/axiosInstance.js — Updated
// Adds:
//   • Retry up to 2 times on 503 or network errors, with 1s delay between retries
//   • Console warning on each retry attempt
//   • Toast warning via a lightweight event bus (avoids circular dep with ToastContext)

import axios from 'axios';

/* ─── Config ─────────────────────────────────────────────────────────── */
const MAX_RETRIES  = 2;
const RETRY_DELAY  = 1000; // ms

/* ─── Helpers ────────────────────────────────────────────────────────── */
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Emit a toast via a custom DOM event so axiosInstance doesn't need to
 * import ToastContext directly (avoids circular dependency).
 * ToastProvider listens for this event in toast.js.
 */
function emitToast(message, type = 'warning') {
  window.dispatchEvent(new CustomEvent('gympro:toast', { detail: { message, type } }));
}

function shouldRetry(error) {
  // Retry on network error (no response) or explicit 503 / 502
  if (!error.response) return true; // network / CORS / offline
  return [502, 503].includes(error.response.status);
}

/* ─── Axios Instance ─────────────────────────────────────────────────── */
const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach JWT from sessionStorage
api.interceptors.request.use(
  (config) => {
    // Track retry count in the config object itself
    config._retryCount = config._retryCount ?? 0;

    const token = sessionStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: auto-logout on 401, retry on 503/network
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config;

    // Auto-logout on unauthorized
    if (error.response?.status === 401) {
      sessionStorage.clear();
      window.location.href = '/login';
      return Promise.reject(error);
    }

    // Retry logic
    if (config && shouldRetry(error) && config._retryCount < MAX_RETRIES) {
      config._retryCount += 1;

      const attempt  = config._retryCount;
      const reason   = error.response
        ? `HTTP ${error.response.status}`
        : 'Network error';

      console.warn(
        `[GymPro] Request failed (${reason}). Retry attempt ${attempt}/${MAX_RETRIES}: ${config.method?.toUpperCase()} ${config.url}`
      );

      emitToast(
        `Connection issue — retrying${attempt < MAX_RETRIES ? ` (${attempt}/${MAX_RETRIES})` : ''}…`,
        'warning'
      );

      await sleep(RETRY_DELAY);
      return api(config); // retry the original request
    }

    return Promise.reject(error);
  }
);

export default api;