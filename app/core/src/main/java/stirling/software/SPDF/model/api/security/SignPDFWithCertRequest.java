package stirling.software.SPDF.model.api.security;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import stirling.software.common.model.api.PDFFile;

@Data
@EqualsAndHashCode(callSuper = true)
public class SignPDFWithCertRequest extends PDFFile {

    @Schema(description = "Certificate alias from smart card. Auto-detected if not provided.")
    private String certificateAlias;

    @Schema(description = "Name of an existing signature field to sign into.")
    private String signatureFieldName;

    @Schema(
            description = "Whether to visually show the signature in the PDF file",
            defaultValue = "true")
    private Boolean showSignature;

    @Schema(description = "The reason for signing the PDF")
    private String reason;

    @Schema(description = "The location where the PDF is signed")
    private String location;

    @Schema(description = "The name of the signer")
    private String name;

    @Schema(description = "Page number where the signature should appear (1-indexed)", defaultValue = "1")
    private Integer pageNumber;

    @Schema(description = "X coordinate of signature box (points from left)")
    private Float sigX;

    @Schema(description = "Y coordinate of signature box (points from bottom)")
    private Float sigY;

    @Schema(description = "Width of signature box in points")
    private Float sigWidth;

    @Schema(description = "Height of signature box in points")
    private Float sigHeight;

    @Schema(description = "Whether to show a logo in the signature", defaultValue = "false")
    private Boolean showLogo;

    @Schema(description = "Smart card PIN for PKCS#11 authentication. Required for most smart cards.")
    private String pin;
}
