import { useState, useCallback } from 'react';
import apiClient from '@app/services/apiClient';

export interface SystemCertificate {
  alias: string;
  subject: string;
  issuer: string;
  notBefore: string;
  notAfter: string;
  serialNumber: string;
}

export const useSystemCertificates = () => {
  const [certificates, setCertificates] = useState<SystemCertificate[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchCertificates = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get('/api/v1/security/certificates/system');
      setCertificates(response.data || []);
    } catch (err: any) {
      setError(err?.message || 'Failed to load system certificates');
      setCertificates([]);
    } finally {
      setLoading(false);
    }
  }, []);

  return { certificates, loading, error, fetchCertificates };
};
