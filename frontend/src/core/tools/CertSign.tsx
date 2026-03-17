import { useState } from "react";
import { useTranslation } from "react-i18next";
import { createToolFlow } from "@app/components/tools/shared/createToolFlow";
import { useCertSignParameters } from "@app/hooks/tools/certSign/useCertSignParameters";
import { useCertSignOperation } from "@app/hooks/tools/certSign/useCertSignOperation";
import { useBaseTool } from "@app/hooks/tools/shared/useBaseTool";
import { BaseToolProps, ToolComponent } from "@app/types/tool";
import { Text, TextInput, NumberInput, Stack, Group, Button } from "@mantine/core";

const CertSign = (props: BaseToolProps) => {
  const { t } = useTranslation();
  const [boxDrawn, setBoxDrawn] = useState(false);

  const base = useBaseTool(
    'certSign',
    useCertSignParameters,
    useCertSignOperation,
    props
  );

  const hasBox = base.params.parameters.sigX != null;

  const handleSetDefaultBox = () => {
    base.params.updateParameter('sigX', 72);
    base.params.updateParameter('sigY', 50);
    base.params.updateParameter('sigWidth', 250);
    base.params.updateParameter('sigHeight', 60);
    setBoxDrawn(true);
  };

  return createToolFlow({
    forceStepNumbers: true,
    files: {
      selectedFiles: base.selectedFiles,
      isCollapsed: base.hasResults,
    },
    steps: [
      {
        title: t("certSign.placement.stepTitle", "Signature Placement"),
        isCollapsed: base.settingsCollapsed,
        onCollapsedClick: base.settingsCollapsed ? base.handleSettingsReset : undefined,
        content: (
          <Stack gap="md">
            <Text size="sm" c="dimmed">
              {t("certSign.placement.description", "Set the position and size of the digital signature box, then click Insert Signature. Your smart card PIN will be requested.")}
            </Text>

            {!boxDrawn && !hasBox && (
              <Button onClick={handleSetDefaultBox} variant="light">
                {t("certSign.placement.setBox", "Set Signature Box")}
              </Button>
            )}

            {(boxDrawn || hasBox) && (
              <>
                <Group grow>
                  <NumberInput
                    label={t("certSign.placement.page", "Page")}
                    value={base.params.parameters.pageNumber}
                    onChange={(val) => base.params.updateParameter('pageNumber', typeof val === 'number' ? val : 1)}
                    min={1}
                  />
                </Group>
                <Group grow>
                  <NumberInput
                    label="X"
                    value={base.params.parameters.sigX ?? 0}
                    onChange={(val) => base.params.updateParameter('sigX', typeof val === 'number' ? val : 0)}
                    min={0}
                  />
                  <NumberInput
                    label="Y"
                    value={base.params.parameters.sigY ?? 0}
                    onChange={(val) => base.params.updateParameter('sigY', typeof val === 'number' ? val : 0)}
                    min={0}
                  />
                  <NumberInput
                    label={t("certSign.placement.width", "Width")}
                    value={base.params.parameters.sigWidth ?? 200}
                    onChange={(val) => base.params.updateParameter('sigWidth', typeof val === 'number' ? val : 200)}
                    min={50}
                  />
                  <NumberInput
                    label={t("certSign.placement.height", "Height")}
                    value={base.params.parameters.sigHeight ?? 50}
                    onChange={(val) => base.params.updateParameter('sigHeight', typeof val === 'number' ? val : 50)}
                    min={20}
                  />
                </Group>
              </>
            )}

            <TextInput
              label={t("certSign.details.reason", "Reason (optional)")}
              value={base.params.parameters.reason}
              onChange={(e) => base.params.updateParameter('reason', e.currentTarget.value)}
              placeholder={t("certSign.details.reasonPlaceholder", "Approved")}
            />
            <TextInput
              label={t("certSign.details.name", "Signer Name (optional)")}
              value={base.params.parameters.name}
              onChange={(e) => base.params.updateParameter('name', e.currentTarget.value)}
              placeholder={t("certSign.details.namePlaceholder", "Auto-detected from certificate")}
            />
          </Stack>
        ),
      },
    ],
    executeButton: {
      text: t("certSign.sign.submit", "Insert Signature"),
      isVisible: !base.hasResults,
      loadingText: t("loading"),
      onClick: base.handleExecute,
      disabled: !base.hasFiles || !base.endpointEnabled,
    },
    review: {
      isVisible: base.hasResults,
      operation: base.operation,
      title: t("certSign.sign.results", "Signed PDF"),
      onFileClick: base.handleThumbnailClick,
      onUndo: base.handleUndo,
    },
  });
};

export default CertSign as ToolComponent;
