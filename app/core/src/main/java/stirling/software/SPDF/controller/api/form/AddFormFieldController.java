package stirling.software.SPDF.controller.api.form;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.github.pixee.security.Filenames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import stirling.software.common.service.CustomPDFDocumentFactory;
import stirling.software.common.util.WebResponseUtils;

@RestController
@RequestMapping("/api/v1/form")
@Tag(name = "Forms", description = "Work with PDF form fields")
@RequiredArgsConstructor
public class AddFormFieldController {

    private final CustomPDFDocumentFactory pdfDocumentFactory;

    @PostMapping(value = "/add-text-field", consumes = "multipart/form-data")
    @Operation(
            summary = "Add a text field to a PDF",
            description =
                    "Adds an AcroForm text field at the specified position on the given page."
                            + " The field is editable in any standard PDF viewer.")
    public ResponseEntity<byte[]> addTextField(
            @RequestPart("fileInput") MultipartFile file,
            @RequestParam("pageNumber") @Parameter(description = "Page number (1-indexed)") int pageNumber,
            @RequestParam("x") @Parameter(description = "X coordinate (points from left)") float x,
            @RequestParam("y") @Parameter(description = "Y coordinate (points from bottom)") float y,
            @RequestParam("width") @Parameter(description = "Field width in points") float width,
            @RequestParam("height") @Parameter(description = "Field height in points") float height,
            @RequestParam(value = "fieldName", defaultValue = "TextField1")
                    @Parameter(description = "Field name")
                    String fieldName,
            @RequestParam(value = "defaultValue", defaultValue = "")
                    @Parameter(description = "Default text value")
                    String defaultValue,
            @RequestParam(value = "fontSize", defaultValue = "12")
                    @Parameter(description = "Font size in points")
                    float fontSize)
            throws Exception {

        try (PDDocument doc = pdfDocumentFactory.load(file)) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                acroForm = new PDAcroForm(doc);
                doc.getDocumentCatalog().setAcroForm(acroForm);
            }

            // Set default resources with a standard font
            PDResources resources = acroForm.getDefaultResources();
            if (resources == null) {
                resources = new PDResources();
                acroForm.setDefaultResources(resources);
            }
            PDType1Font font = new PDType1Font(FontName.HELVETICA);
            resources.put(COSName.getPDFName("Helv"), font);
            acroForm.setDefaultAppearance("/Helv " + fontSize + " Tf 0 g");

            // Ensure unique field name
            String uniqueName = fieldName;
            int counter = 1;
            while (acroForm.getField(uniqueName) != null) {
                uniqueName = fieldName + "_" + counter++;
            }

            PDTextField textField = new PDTextField(acroForm);
            textField.setPartialName(uniqueName);

            // Create widget annotation for the field
            PDAnnotationWidget widget = textField.getWidgets().get(0);

            // Convert 1-indexed to 0-indexed
            int pageIdx = pageNumber - 1;
            if (pageIdx < 0 || pageIdx >= doc.getNumberOfPages()) {
                throw new IllegalArgumentException("Page number out of range: " + pageNumber);
            }
            PDPage page = doc.getPage(pageIdx);

            PDRectangle rect = new PDRectangle(x, y, width, height);
            widget.setRectangle(rect);
            widget.setPage(page);

            // Set border appearance
            PDAppearanceCharacteristicsDictionary appearanceDict =
                    new PDAppearanceCharacteristicsDictionary(
                            new org.apache.pdfbox.cos.COSDictionary());
            widget.setAppearanceCharacteristics(appearanceDict);

            page.getAnnotations().add(widget);
            acroForm.getFields().add(textField);

            // Set default value if provided
            if (defaultValue != null && !defaultValue.isEmpty()) {
                textField.setValue(defaultValue);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);

            String originalName = Filenames.toSimpleFileName(file.getOriginalFilename());
            String outputName =
                    originalName != null
                            ? originalName.replaceFirst("\\.[^.]+$", "_form.pdf")
                            : "document_form.pdf";

            return WebResponseUtils.bytesToWebResponse(baos.toByteArray(), outputName);
        }
    }

    @PostMapping(value = "/add-signature-field", consumes = "multipart/form-data")
    @Operation(
            summary = "Add a signature field to a PDF",
            description =
                    "Adds an AcroForm signature field at the specified position on the given page."
                            + " The field can be signed using digital certificates in any PDF viewer"
                            + " or via the cert-sign endpoint.")
    public ResponseEntity<byte[]> addSignatureField(
            @RequestPart("fileInput") MultipartFile file,
            @RequestParam("pageNumber") @Parameter(description = "Page number (1-indexed)") int pageNumber,
            @RequestParam("x") @Parameter(description = "X coordinate (points from left)") float x,
            @RequestParam("y") @Parameter(description = "Y coordinate (points from bottom)") float y,
            @RequestParam("width") @Parameter(description = "Field width in points") float width,
            @RequestParam("height") @Parameter(description = "Field height in points") float height,
            @RequestParam(value = "fieldName", defaultValue = "SignatureField1")
                    @Parameter(description = "Signature field name")
                    String fieldName)
            throws Exception {

        try (PDDocument doc = pdfDocumentFactory.load(file)) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                acroForm = new PDAcroForm(doc);
                doc.getDocumentCatalog().setAcroForm(acroForm);
            }

            // Ensure unique field name
            String uniqueName = fieldName;
            int counter = 1;
            while (acroForm.getField(uniqueName) != null) {
                uniqueName = fieldName + "_" + counter++;
            }

            PDSignatureField sigField = new PDSignatureField(acroForm);
            sigField.setPartialName(uniqueName);

            PDAnnotationWidget widget = sigField.getWidgets().get(0);

            int pageIdx = pageNumber - 1;
            if (pageIdx < 0 || pageIdx >= doc.getNumberOfPages()) {
                throw new IllegalArgumentException("Page number out of range: " + pageNumber);
            }
            PDPage page = doc.getPage(pageIdx);

            PDRectangle rect = new PDRectangle(x, y, width, height);
            widget.setRectangle(rect);
            widget.setPage(page);

            // Set border appearance
            PDAppearanceCharacteristicsDictionary appearanceDict =
                    new PDAppearanceCharacteristicsDictionary(
                            new org.apache.pdfbox.cos.COSDictionary());
            widget.setAppearanceCharacteristics(appearanceDict);

            page.getAnnotations().add(widget);
            acroForm.getFields().add(sigField);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);

            String originalName = Filenames.toSimpleFileName(file.getOriginalFilename());
            String outputName =
                    originalName != null
                            ? originalName.replaceFirst("\\.[^.]+$", "_sigfield.pdf")
                            : "document_sigfield.pdf";

            return WebResponseUtils.bytesToWebResponse(baos.toByteArray(), outputName);
        }
    }
}
