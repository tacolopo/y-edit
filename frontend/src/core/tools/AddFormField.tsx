import { useState } from "react";
import { useTranslation } from "react-i18next";
import { NumberInput, TextInput, Select, Stack, Group } from "@mantine/core";
import { createToolFlow } from "@app/components/tools/shared/createToolFlow";
import {
  useAddTextFieldParameters,
  useAddSignatureFieldParameters,
  useAddTextFieldOperation,
  useAddSignatureFieldOperation,
} from "@app/hooks/tools/addFormField/useAddFormFieldOperation";
import { useBaseTool } from "@app/hooks/tools/shared/useBaseTool";
import { BaseToolProps, ToolComponent } from "@app/types/tool";

const AddFormField = (props: BaseToolProps) => {
  const { t } = useTranslation();
  const [fieldType, setFieldType] = useState<'text' | 'signature'>('signature');

  const textBase = useBaseTool(
    'addFormField' as any,
    useAddTextFieldParameters,
    useAddTextFieldOperation,
    props
  );

  const sigBase = useBaseTool(
    'addFormField' as any,
    useAddSignatureFieldParameters,
    useAddSignatureFieldOperation,
    props
  );

  const base = fieldType === 'text' ? textBase : sigBase;
  const params = fieldType === 'text' ? textBase.params : sigBase.params;

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
                { value: 'signature', label: t("addFormField.signatureField", "Signature Field") },
                { value: 'text', label: t("addFormField.textField", "Text Field") },
              ]}
              value={fieldType}
              onChange={(val) => setFieldType((val as 'text' | 'signature') || 'signature')}
            />

            <Group grow>
              <NumberInput
                label={t("addFormField.page", "Page")}
                value={params.parameters.pageNumber}
                onChange={(val) => params.updateParameter('pageNumber', typeof val === 'number' ? val : 1)}
                min={1}
              />
              <TextInput
                label={t("addFormField.name", "Field Name")}
                value={params.parameters.fieldName}
                onChange={(e) => params.updateParameter('fieldName', e.currentTarget.value)}
              />
            </Group>

            <Group grow>
              <NumberInput
                label="X"
                value={params.parameters.x}
                onChange={(val) => params.updateParameter('x', typeof val === 'number' ? val : 0)}
                min={0}
              />
              <NumberInput
                label="Y"
                value={params.parameters.y}
                onChange={(val) => params.updateParameter('y', typeof val === 'number' ? val : 0)}
                min={0}
              />
              <NumberInput
                label={t("addFormField.width", "Width")}
                value={params.parameters.width}
                onChange={(val) => params.updateParameter('width', typeof val === 'number' ? val : 100)}
                min={10}
              />
              <NumberInput
                label={t("addFormField.height", "Height")}
                value={params.parameters.height}
                onChange={(val) => params.updateParameter('height', typeof val === 'number' ? val : 20)}
                min={10}
              />
            </Group>

            {fieldType === 'text' && (
              <Group grow>
                <TextInput
                  label={t("addFormField.defaultValue", "Default Value")}
                  value={(params.parameters as any).defaultValue ?? ''}
                  onChange={(e) => params.updateParameter('defaultValue' as any, e.currentTarget.value)}
                />
                <NumberInput
                  label={t("addFormField.fontSize", "Font Size")}
                  value={(params.parameters as any).fontSize ?? 12}
                  onChange={(val) => params.updateParameter('fontSize' as any, typeof val === 'number' ? val : 12)}
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
