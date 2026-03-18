import { BaseParameters } from '@app/types/parameters';
import { useBaseParameters, BaseParametersHook } from '@app/hooks/tools/shared/useBaseParameters';

export interface CertSignParameters extends BaseParameters {
  // Signature box placement (set by drawing on the PDF)
  sigX: number | null;
  sigY: number | null;
  sigWidth: number | null;
  sigHeight: number | null;
  pageNumber: number;

  // Optional: sign into an existing field
  signatureFieldName: string;

  // Smart card PIN
  pin: string;

  // Signature metadata
  reason: string;
  location: string;
  name: string;
  showLogo: boolean;
}

export const defaultParameters: CertSignParameters = {
  sigX: null,
  sigY: null,
  sigWidth: null,
  sigHeight: null,
  pageNumber: 1,
  signatureFieldName: '',
  pin: '',
  reason: '',
  location: '',
  name: '',
  showLogo: false,
};

export type CertSignParametersHook = BaseParametersHook<CertSignParameters>;

export const useCertSignParameters = (): CertSignParametersHook => {
  return useBaseParameters({
    defaultParameters,
    endpointName: 'cert-sign',
    validateFn: () => true,
  });
};
