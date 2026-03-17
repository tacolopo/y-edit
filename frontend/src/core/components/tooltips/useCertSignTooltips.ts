import { useTranslation } from 'react-i18next';
import { TooltipContent } from '@app/types/tips';

export const useCertSignTooltips = (): TooltipContent => {
  const { t } = useTranslation();

  return {
    header: {
      title: t("certSign.tooltip.header.title", "About Managing Signatures")
    },
    tips: [
      {
        title: t("certSign.tooltip.overview.title", "What can this tool do?"),
        description: t("certSign.tooltip.overview.text", "This tool lets you check if your PDFs are digitally signed and add new digital signatures. Digital signatures prove who created or approved a document and show if it has been changed since signing."),
        bullets: [
          t("certSign.tooltip.overview.bullet1", "Check existing signatures and their validity"),
          t("certSign.tooltip.overview.bullet2", "View detailed information about signers and certificates"),
          t("certSign.tooltip.overview.bullet3", "Add new digital signatures to secure your documents"),
          t("certSign.tooltip.overview.bullet4", "Multiple files supported with easy navigation")
        ]
      },
      {
        title: t("certSign.tooltip.validation.title", "Checking Signatures"),
        description: t("certSign.tooltip.validation.text", "When you check signatures, the tool tells you if they're valid, who signed the document, when it was signed, and whether the document has been changed since signing."),
        bullets: [
          t("certSign.tooltip.validation.bullet1", "Shows if signatures are valid or invalid"),
          t("certSign.tooltip.validation.bullet2", "Displays signer information and signing date"),
          t("certSign.tooltip.validation.bullet3", "Checks if the document was modified after signing"),
          t("certSign.tooltip.validation.bullet4", "Can use custom certificates for verification")
        ]
      },
      {
        title: t("certSign.tooltip.signing.title", "Adding Signatures"),
        description: t("certSign.tooltip.signing.text", "Insert your HSPD-12 badge or smart card to digitally sign PDFs. Windows will prompt for your PIN. You can make the signature visible or keep it invisible."),
        bullets: [
          t("certSign.tooltip.signing.bullet1", "Uses your HSPD-12 badge or PIV smart card"),
          t("certSign.tooltip.signing.bullet2", "Option to show or hide signature on the PDF"),
          t("certSign.tooltip.signing.bullet3", "Add reason, location, and signer name"),
          t("certSign.tooltip.signing.bullet4", "Choose which page to place visible signatures")
        ]
      }
    ]
  };
};