import { Stack, Text, ColorInput } from "@mantine/core";
import { useTranslation } from "react-i18next";
import { AddWatermarkParameters } from "@app/hooks/tools/addWatermark/useAddWatermarkParameters";
import { Z_INDEX_AUTOMATE_DROPDOWN } from "@app/styles/zIndex";

interface WatermarkTextStyleProps {
  parameters: AddWatermarkParameters;
  onParameterChange: <K extends keyof AddWatermarkParameters>(key: K, value: AddWatermarkParameters[K]) => void;
  disabled?: boolean;
}

const WatermarkTextStyle = ({ parameters, onParameterChange, disabled = false }: WatermarkTextStyleProps) => {
  const { t } = useTranslation();


  return (
    <Stack gap="sm">
      <Stack gap="xs">
        <Text size="xs" fw={500}>
          {t("watermark.settings.color", "Colour")}
        </Text>
        <ColorInput
          value={parameters.customColor}
          onChange={(value) => onParameterChange("customColor", value)}
          disabled={disabled}
          format="hex"
          popoverProps={{ withinPortal: true, zIndex: Z_INDEX_AUTOMATE_DROPDOWN }}
        />
      </Stack>
    </Stack>
  );
};

export default WatermarkTextStyle;
