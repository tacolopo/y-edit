// Global error handler — catches errors outside React tree and displays them on screen
window.onerror = (msg, source, line, col, error) => {
  const el = document.getElementById('root');
  if (el) {
    el.innerHTML = `<div style="padding:2rem;font-family:monospace;color:red">
      <h2>Y-Edit Startup Error</h2>
      <p><b>${msg}</b></p>
      <p>Source: ${source}:${line}:${col}</p>
      <pre style="white-space:pre-wrap;max-height:400px;overflow:auto;background:#111;color:#f88;padding:1rem;border-radius:4px">${error?.stack || 'no stack'}</pre>
    </div>`;
  }
};
window.onunhandledrejection = (event) => {
  console.error('Unhandled rejection:', event.reason);
};

import '@mantine/core/styles.css';
import '@mantine/dates/styles.css';
import '../vite-env.d.ts'; // eslint-disable-line no-restricted-imports -- Outside app paths
import '@app/styles/index.css';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { ColorSchemeScript } from '@mantine/core';
import { BrowserRouter } from 'react-router-dom';
import App from '@app/App';
import '@app/i18n';
import { BASE_PATH } from '@app/constants/app';

// In Tauri desktop mode, discover the backend port in the background.
// The API client reads window.STIRLING_PDF_API_BASE_URL dynamically per request,
// so it will pick up the URL as soon as it's set.
if (typeof window !== 'undefined' && (window as any).__TAURI_INTERNALS__) {
  import('@tauri-apps/api/core').then(({ invoke }) => {
    const poll = async () => {
      for (let i = 0; i < 120; i++) {
        try {
          const port = await invoke<number | null>('get_backend_port');
          if (port) {
            (window as any).STIRLING_PDF_API_BASE_URL = `http://localhost:${port}`;
            console.log(`Y-Edit: Backend at http://localhost:${port}`);
            return;
          }
        } catch (e) {
          console.warn('Y-Edit: invoke error', e);
        }
        await new Promise(r => setTimeout(r, 500));
      }
      console.error('Y-Edit: Backend port not found after 60s');
    };
    poll();
  }).catch(e => console.error('Y-Edit: Tauri API load failed', e));
}

const container = document.getElementById('root');
if (!container) {
  throw new Error("Root container missing in index.html");
}

const root = ReactDOM.createRoot(container);
root.render(
  <React.StrictMode>
    <ColorSchemeScript />
    <BrowserRouter basename={BASE_PATH}>
      <App />
    </BrowserRouter>
  </React.StrictMode>
);
