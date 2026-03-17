import { BaseParameters } from '@app/types/parameters';
import { useBaseParameters, BaseParametersHook } from '@app/hooks/tools/shared/useBaseParameters';

export interface CertSignParameters extends BaseParameters {
  // Always WINDOWS_STORE - smart card (HSPD-12) signing only
  certType: 'WINDOWS_STORE';
  certificateAlias: string;

  // Signature appearance options
  showSignature: boolean;
  reason: string;
  location: string;
  name: string;
  pageNumber: number;
  showLogo: boolean;
}

export const defaultParameters: CertSignParameters = {
  certType: 'WINDOWS_STORE',
  certificateAlias: '',
  showSignature: false,
  reason: '',
  location: '',
  name: '',
  pageNumber: 1,
  showLogo: true,
};

export type CertSignParametersHook = BaseParametersHook<CertSignParameters>;

export const useCertSignParameters = (): CertSignParametersHook => {
  return useBaseParameters({
    defaultParameters,
    endpointName: 'cert-sign',
    validateFn: () => {
      // WINDOWS_STORE always valid - Windows handles certificate selection and PIN
      return true;
    },
  });
};
