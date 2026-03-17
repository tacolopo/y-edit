import { useTranslation } from 'react-i18next';
import { ToolType, useToolOperation } from '@app/hooks/tools/shared/useToolOperation';
import { createStandardErrorHandler } from '@app/utils/toolErrorHandler';
import { CertSignParameters, defaultParameters } from '@app/hooks/tools/certSign/useCertSignParameters';

// Build form data for HSPD-12 smart card signing
export const buildCertSignFormData = (parameters: CertSignParameters, file: File): FormData => {
  const formData = new FormData();
  formData.append('fileInput', file);
  formData.append('certType', 'WINDOWS_STORE');

  if (parameters.certificateAlias) {
    formData.append('certificateAlias', parameters.certificateAlias);
  }

  // Add signature appearance options if enabled
  if (parameters.showSignature) {
    formData.append('showSignature', 'true');
    formData.append('reason', parameters.reason);
    formData.append('location', parameters.location);
    formData.append('name', parameters.name);
    formData.append('pageNumber', parameters.pageNumber.toString());
    formData.append('showLogo', parameters.showLogo.toString());
  }

  return formData;
};

// Static configuration object
export const certSignOperationConfig = {
  toolType: ToolType.singleFile,
  buildFormData: buildCertSignFormData,
  operationType: 'certSign',
  endpoint: '/api/v1/security/cert-sign',
  multiFileEndpoint: false,
  defaultParameters,
} as const;

export const useCertSignOperation = () => {
  const { t } = useTranslation();

  return useToolOperation<CertSignParameters>({
    ...certSignOperationConfig,
    getErrorMessage: createStandardErrorHandler(t('certSign.error.failed', 'An error occurred while signing. Ensure your smart card is inserted.'))
  });
};
