import { useTranslation } from 'react-i18next';
import { useToolOperation, ToolType } from '@app/hooks/tools/shared/useToolOperation';
import { createStandardErrorHandler } from '@app/utils/toolErrorHandler';
import { BaseParameters } from '@app/types/parameters';
import { useBaseParameters, BaseParametersHook } from '@app/hooks/tools/shared/useBaseParameters';

export interface AddTextFieldParameters extends BaseParameters {
  pageNumber: number;
  x: number;
  y: number;
  width: number;
  height: number;
  fieldName: string;
  defaultValue: string;
  fontSize: number;
}

export interface AddSignatureFieldParameters extends BaseParameters {
  pageNumber: number;
  x: number;
  y: number;
  width: number;
  height: number;
  fieldName: string;
}

export const defaultTextFieldParameters: AddTextFieldParameters = {
  pageNumber: 1,
  x: 72,
  y: 700,
  width: 200,
  height: 24,
  fieldName: 'TextField1',
  defaultValue: '',
  fontSize: 12,
};

export const defaultSignatureFieldParameters: AddSignatureFieldParameters = {
  pageNumber: 1,
  x: 72,
  y: 100,
  width: 200,
  height: 60,
  fieldName: 'SignatureField1',
};

export const buildAddTextFieldFormData = (parameters: AddTextFieldParameters, file: File): FormData => {
  const formData = new FormData();
  formData.append('fileInput', file);
  formData.append('pageNumber', parameters.pageNumber.toString());
  formData.append('x', parameters.x.toString());
  formData.append('y', parameters.y.toString());
  formData.append('width', parameters.width.toString());
  formData.append('height', parameters.height.toString());
  formData.append('fieldName', parameters.fieldName);
  formData.append('defaultValue', parameters.defaultValue);
  formData.append('fontSize', parameters.fontSize.toString());
  return formData;
};

export const buildAddSignatureFieldFormData = (parameters: AddSignatureFieldParameters, file: File): FormData => {
  const formData = new FormData();
  formData.append('fileInput', file);
  formData.append('pageNumber', parameters.pageNumber.toString());
  formData.append('x', parameters.x.toString());
  formData.append('y', parameters.y.toString());
  formData.append('width', parameters.width.toString());
  formData.append('height', parameters.height.toString());
  formData.append('fieldName', parameters.fieldName);
  return formData;
};

export const useAddTextFieldParameters = (): BaseParametersHook<AddTextFieldParameters> => {
  return useBaseParameters({
    defaultParameters: defaultTextFieldParameters,
    endpointName: 'add-text-field',
    validateFn: () => true,
  });
};

export const useAddSignatureFieldParameters = (): BaseParametersHook<AddSignatureFieldParameters> => {
  return useBaseParameters({
    defaultParameters: defaultSignatureFieldParameters,
    endpointName: 'add-signature-field',
    validateFn: () => true,
  });
};

export const useAddTextFieldOperation = () => {
  const { t } = useTranslation();

  return useToolOperation<AddTextFieldParameters>({
    toolType: ToolType.singleFile,
    operationType: 'addFormField' as any,
    endpoint: '/api/v1/form/add-text-field',
    buildFormData: buildAddTextFieldFormData,
    defaultParameters: defaultTextFieldParameters,
    getErrorMessage: createStandardErrorHandler(
      t('addFormField.error.textField', 'An error occurred while adding the text field.')
    ),
  });
};

export const useAddSignatureFieldOperation = () => {
  const { t } = useTranslation();

  return useToolOperation<AddSignatureFieldParameters>({
    toolType: ToolType.singleFile,
    operationType: 'addFormField' as any,
    endpoint: '/api/v1/form/add-signature-field',
    buildFormData: buildAddSignatureFieldFormData,
    defaultParameters: defaultSignatureFieldParameters,
    getErrorMessage: createStandardErrorHandler(
      t('addFormField.error.signatureField', 'An error occurred while adding the signature field.')
    ),
  });
};
