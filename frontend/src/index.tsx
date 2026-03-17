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

declare global {
  interface Window {
    STIRLING_PDF_API_BASE_URL?: string;
    __TAURI_INTERNALS__?: unknown;
  }
}

const container = document.getElementById('root');
if (!container) {
  throw new Error("Root container missing in index.html");
}

async function main() {
  // In Tauri: wait for the backend port before rendering anything
  if (window.__TAURI_INTERNALS__) {
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      for (let i = 0; i < 120; i++) {
        const port = await invoke<number | null>('get_backend_port');
        if (port) {
          window.STIRLING_PDF_API_BASE_URL = `http://localhost:${port}`;
          break;
        }
        await new Promise(r => setTimeout(r, 500));
      }
    } catch (e) {
      console.error('Y-Edit: backend discovery failed', e);
    }
  }

  const root = ReactDOM.createRoot(container!);
  root.render(
    <React.StrictMode>
      <ColorSchemeScript />
      <BrowserRouter basename={BASE_PATH}>
        <App />
      </BrowserRouter>
    </React.StrictMode>
  );
}

main().catch(e => {
  console.error('Y-Edit: fatal startup error', e);
  container!.innerHTML = `<pre style="color:red;padding:2rem">${e}\n${e?.stack}</pre>`;
});
