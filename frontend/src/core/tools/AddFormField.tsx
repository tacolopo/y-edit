import { useState, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { NumberInput, TextInput, Select, Stack, Group } from "@mantine/core";
import { createToolFlow } from "@app/components/tools/shared/createToolFlow";
import { useAddTextFieldOperation, useAddSignatureFieldOperation } from "@app/hooks/tools/addFormField/useAddFormFieldOperation";
import type { AddTextFieldParameters, AddSignatureFieldParameters } from "@app/hooks/tools/addFormField/useAddFormFieldOperation";
import { defaultTextFieldParameters, defaultSignatureFieldParameters } from "@app/hooks/tools/addFormField/useAddFormFieldOperation";
import { useBaseTool } from "@app/hooks/tools/shared/useBaseTool";
import { BaseToolProps, ToolComponent } from "@app/types/tool";

type FieldType = 'text' | 'signature';

const AddFormField = (props: BaseToolProps) => {
  const { t } = useTranslation();
  const [fieldType, setFieldType] = useState<FieldType>('text');

  const [textParams, setTextParams] = useState<AddTextFieldParameters>(defaultTextFieldParameters);
  const [sigParams, setSigParams] = useState<AddSignatureFieldParameters>(defaultSignatureFieldParameters);

  const useTextFieldParams = useCallback(() => ({
    parameters: textParams,
    updateParameter: <K extends keyof AddTextFieldParameters>(key: K, value: AddTextFieldParameters[K]) => {
      setTextParams(prev => ({ ...prev, [key]: value }));
    },
    validateParameters: () => true,
  }), [textParams]);

  const useSigFieldParams = useCallback(() => ({
    parameters: sigParams,
    updateParameter: <K extends keyof AddSignatureFieldParameters>(key: K, value: AddSignatureFieldParameters[K]) => {
      setSigParams(prev => ({ ...prev, [key]: value }));
    },
    validateParameters: () => true,
  }), [sigParams]);

  const textBase = useBaseTool(
    'addTextField' as any,
    useTextFieldParams,
    useAddTextFieldOperation,
    props
  );

  const sigBase = useBaseTool(
    'addSignatureField' as any,
    useSigFieldParams,
    useAddSignatureFieldOperation,
    props
  );

  const base = fieldType === 'text' ? textBase : sigBase;

  return createToolFlow({
    files: {
      selectedFiles: base.selectedFiles,
      isCollapsed: base.hasResults,
    },
    steps: [
      {
        title: t("addFormField.settings", "Field Settings"),
        isCollapsed: base.settingsCollapsed,
        onCollapsedClick: base.settingsCollapsed ? base.handleSettingsReset : undefined,
        content: (
          <Stack gap="md">
            <Select
              label={t("addFormField.fieldType", "Field Type")}
              data={[
                { value: 'text', label: t("addFormField.textField", "Text Field") },
                { value: 'signature', label: t("addFormField.signatureField", "Signature Field") },
              ]}
              value={fieldType}
              onChange={(val) => setFieldType((val as FieldType) || 'text')}
            />

            <Group grow>
              <NumberInput
                label={t("addFormField.page", "Page")}
                value={fieldType === 'text' ? textParams.pageNumber : sigParams.pageNumber}
                onChange={(val) => {
                  const v = typeof val === 'number' ? val : 1;
                  if (fieldType === 'text') setTextParams(p => ({ ...p, pageNumber: v }));
                  else setSigParams(p => ({ ...p, pageNumber: v }));
                }}
                min={1}
              />
              <TextInput
                label={t("addFormField.name", "Field Name")}
                value={fieldType === 'text' ? textParams.fieldName : sigParams.fieldName}
                onChange={(e) => {
                  const v = e.currentTarget.value;
                  if (fieldType === 'text') setTextParams(p => ({ ...p, fieldName: v }));
                  else setSigParams(p => ({ ...p, fieldName: v }));
                }}
              />
            </Group>

            <Group grow>
              <NumberInput
                label="X"
                value={fieldType === 'text' ? textParams.x : sigParams.x}
                onChange={(val) => {
                  const v = typeof val === 'number' ? val : 0;
                  if (fieldType === 'text') setTextParams(p => ({ ...p, x: v }));
                  else setSigParams(p => ({ ...p, x: v }));
                }}
                min={0}
              />
              <NumberInput
                label="Y"
                value={fieldType === 'text' ? textParams.y : sigParams.y}
                onChange={(val) => {
                  const v = typeof val === 'number' ? val : 0;
                  if (fieldType === 'text') setTextParams(p => ({ ...p, y: v }));
                  else setSigParams(p => ({ ...p, y: v }));
                }}
                min={0}
              />
              <NumberInput
                label={t("addFormField.width", "Width")}
                value={fieldType === 'text' ? textParams.width : sigParams.width}
                onChange={(val) => {
                  const v = typeof val === 'number' ? val : 100;
                  if (fieldType === 'text') setTextParams(p => ({ ...p, width: v }));
                  else setSigParams(p => ({ ...p, width: v }));
                }}
                min={10}
              />
              <NumberInput
                label={t("addFormField.height", "Height")}
                value={fieldType === 'text' ? textParams.height : sigParams.height}
                onChange={(val) => {
                  const v = typeof val === 'number' ? val : 20;
                  if (fieldType === 'text') setTextParams(p => ({ ...p, height: v }));
                  else setSigParams(p => ({ ...p, height: v }));
                }}
                min={10}
              />
            </Group>

            {fieldType === 'text' && (
              <Group grow>
                <TextInput
                  label={t("addFormField.defaultValue", "Default Value")}
                  value={textParams.defaultValue}
                  onChange={(e) => setTextParams(p => ({ ...p, defaultValue: e.currentTarget.value }))}
                />
                <NumberInput
                  label={t("addFormField.fontSize", "Font Size")}
                  value={textParams.fontSize}
                  onChange={(val) => setTextParams(p => ({ ...p, fontSize: typeof val === 'number' ? val : 12 }))}
                  min={6}
                  max={72}
                />
              </Group>
            )}
          </Stack>
        ),
      },
    ],
    executeButton: {
      text: fieldType === 'text'
        ? t("addFormField.addTextField", "Add Text Field")
        : t("addFormField.addSignatureField", "Add Signature Field"),
      isVisible: !base.hasResults,
      loadingText: t("loading"),
      onClick: base.handleExecute,
      disabled: !base.hasFiles || !base.endpointEnabled,
    },
    review: {
      isVisible: base.hasResults,
      operation: base.operation,
      title: t("addFormField.results", "Form Field Added"),
      onFileClick: base.handleThumbnailClick,
      onUndo: base.handleUndo,
    },
  });
};

export default AddFormField as ToolComponent;
