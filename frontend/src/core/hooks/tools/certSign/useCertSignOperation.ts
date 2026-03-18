import { useTranslation } from 'react-i18next';
import { ToolType, useToolOperation } from '@app/hooks/tools/shared/useToolOperation';
import { createStandardErrorHandler } from '@app/utils/toolErrorHandler';
import { CertSignParameters, defaultParameters } from '@app/hooks/tools/certSign/useCertSignParameters';

export const buildCertSignFormData = (parameters: CertSignParameters, file: File): FormData => {
  const formData = new FormData();
  formData.append('fileInput', file);
  formData.append('showSignature', 'true');
  formData.append('pageNumber', parameters.pageNumber.toString());

  if (parameters.sigX != null && parameters.sigY != null
      && parameters.sigWidth != null && parameters.sigHeight != null) {
    formData.append('sigX', parameters.sigX.toString());
    formData.append('sigY', parameters.sigY.toString());
    formData.append('sigWidth', parameters.sigWidth.toString());
    formData.append('sigHeight', parameters.sigHeight.toString());
  }

  if (parameters.signatureFieldName) {
    formData.append('signatureFieldName', parameters.signatureFieldName);
  }

  if (parameters.pin) formData.append('pin', parameters.pin);
  if (parameters.reason) formData.append('reason', parameters.reason);
  if (parameters.location) formData.append('location', parameters.location);
  if (parameters.name) formData.append('name', parameters.name);
  formData.append('showLogo', (parameters.showLogo ?? false).toString());

  return formData;
};

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
    getErrorMessage: createStandardErrorHandler(
      t('certSign.error.failed', 'Signing failed. Ensure your smart card is inserted and middleware is installed.')
    ),
  });
};
