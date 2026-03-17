import { useCallback, useEffect, useState } from 'react';
import { getApiBaseUrl } from '@app/services/apiClientConfig';

type BackendStatus = 'up' | 'starting' | 'down';

interface BackendProbeState {
  status: BackendStatus;
  loginDisabled: boolean;
  loading: boolean;
}

/**
 * Lightweight backend probe that avoids global axios interceptors.
 * Uses getApiBaseUrl() to resolve the dynamic backend port in desktop mode.
 */
export function useBackendProbe() {
  const [state, setState] = useState<BackendProbeState>({
    status: 'starting',
    loginDisabled: false,
    loading: true,
  });

  const probe = useCallback(async () => {
    const base = getApiBaseUrl().replace(/\/$/, '');
    const statusUrl = `${base}/api/v1/info/status`;

    const next: BackendProbeState = {
      status: 'starting',
      loginDisabled: false,
      loading: false,
    };

    try {
      const res = await fetch(statusUrl, { method: 'GET', cache: 'no-store' });
      if (res.ok) {
        const data = await res.json().catch(() => null);
        if (data && data.status === 'UP') {
          next.status = 'up';
          // Desktop mode: login is always disabled
          next.loginDisabled = true;
          setState(next);
          return next;
        }
        next.status = 'starting';
      } else if (res.status === 404 || res.status === 503) {
        next.status = 'starting';
      } else {
        next.status = 'down';
      }
    } catch {
      next.status = 'down';
    }

    setState(next);
    return next;
  }, []);

  useEffect(() => {
    void probe();
  }, [probe]);

  return {
    ...state,
    probe,
  };
}
