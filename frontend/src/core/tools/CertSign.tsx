import { useTranslation } from "react-i18next";
import { createToolFlow } from "@app/components/tools/shared/createToolFlow";
import SignatureAppearanceSettings from "@app/components/tools/certSign/SignatureAppearanceSettings";
import { useCertSignParameters } from "@app/hooks/tools/certSign/useCertSignParameters";
import { useCertSignOperation } from "@app/hooks/tools/certSign/useCertSignOperation";
import { useSignatureAppearanceTips } from "@app/components/tooltips/useSignatureAppearanceTips";
import { useBaseTool } from "@app/hooks/tools/shared/useBaseTool";
import { BaseToolProps, ToolComponent } from "@app/types/tool";
import { Text } from "@mantine/core";

const CertSign = (props: BaseToolProps) => {
  const { t } = useTranslation();

  const base = useBaseTool(
    'certSign',
    useCertSignParameters,
    useCertSignOperation,
    props
  );

  const appearanceTips = useSignatureAppearanceTips();

  return createToolFlow({
    forceStepNumbers: true,
    files: {
      selectedFiles: base.selectedFiles,
      isCollapsed: base.hasResults,
    },
    steps: [
      {
        title: t("certSign.smartCard.stepTitle", "Smart Card"),
        isCollapsed: base.settingsCollapsed,
        onCollapsedClick: base.settingsCollapsed ? base.handleSettingsReset : undefined,
        content: (
          <Text size="sm" c="dimmed">
            {t("certSign.smartCard.description", "Insert your HSPD-12 badge or smart card. Windows will prompt for your PIN when signing.")}
          </Text>
        ),
      },
      {
        title: t("certSign.appearance.stepTitle", "Signature Appearance"),
        isCollapsed: base.settingsCollapsed,
        onCollapsedClick: base.settingsCollapsed ? base.handleSettingsReset : undefined,
        tooltip: appearanceTips,
        content: (
          <SignatureAppearanceSettings
            parameters={base.params.parameters}
            onParameterChange={base.params.updateParameter}
            disabled={base.endpointLoading}
          />
        ),
      },
    ],
    executeButton: {
      text: t("certSign.sign.submit", "Sign PDF"),
      isVisible: !base.hasResults,
      loadingText: t("loading"),
      onClick: base.handleExecute,
      disabled: !base.params.validateParameters() || !base.hasFiles || !base.endpointEnabled,
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
