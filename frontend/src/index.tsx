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
import initBackendUrl from './client/initBackendUrl';

const container = document.getElementById('root');
if (!container) {
  throw new Error("Root container missing in index.html");
}

// In Tauri desktop mode, discover the backend port before rendering
initBackendUrl().then(() => {
  const root = ReactDOM.createRoot(container);
  root.render(
    <React.StrictMode>
      <ColorSchemeScript />
      <BrowserRouter basename={BASE_PATH}>
        <App />
      </BrowserRouter>
    </React.StrictMode>
  );
});
