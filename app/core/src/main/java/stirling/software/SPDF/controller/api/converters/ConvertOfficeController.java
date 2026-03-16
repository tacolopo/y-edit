package stirling.software.SPDF.controller.api.converters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import io.github.pixee.security.Filenames;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import stirling.software.common.annotations.AutoJobPostMapping;
import stirling.software.common.annotations.api.ConvertApi;
import stirling.software.common.model.api.GeneralFile;
import stirling.software.common.service.CustomPDFDocumentFactory;
import stirling.software.common.util.ExceptionUtils;
import stirling.software.common.util.GeneralUtils;
import stirling.software.common.util.MsOfficeConverter;
import stirling.software.common.util.RegexPatternUtils;
import stirling.software.common.util.WebResponseUtils;

@ConvertApi
@RequiredArgsConstructor
@Slf4j
public class ConvertOfficeController {

    private final CustomPDFDocumentFactory pdfDocumentFactory;
    private final MsOfficeConverter msOfficeConverter;

    public File convertToPdf(MultipartFile inputFile) throws IOException, InterruptedException {
        String originalFilename = Filenames.toSimpleFileName(inputFile.getOriginalFilename());
        if (originalFilename == null || originalFilename.isBlank()) {
            throw ExceptionUtils.createFileNoNameException();
        }

        String extension = FilenameUtils.getExtension(originalFilename);
        if (extension == null || !isValidFileExtension(extension)) {
            throw ExceptionUtils.createIllegalArgumentException(
                    "error.invalid.extension", "Invalid file extension: " + extension);
        }
        String extensionLower = extension.toLowerCase(Locale.ROOT);

        String baseName = FilenameUtils.getBaseName(originalFilename);
        if (baseName == null || baseName.isBlank()) {
            baseName = "input";
        }

        if (!msOfficeConverter.canConvertToPdf(extensionLower)) {
            throw new IOException(
                    "MS Office cannot convert this file type: " + extensionLower);
        }

        // Create temp input file
        Path workDir = Files.createTempDirectory("office2pdf_");
        Path inputPath = workDir.resolve(baseName + "." + extensionLower);
        Files.copy(inputFile.getInputStream(), inputPath, StandardCopyOption.REPLACE_EXISTING);

        try {
            File result = msOfficeConverter.convertToPdf(inputPath.toFile(), extensionLower);
            return result;
        } finally {
            try {
                Files.deleteIfExists(inputPath);
            } catch (IOException e) {
                log.warn("Failed to delete temp input file: {}", inputPath, e);
            }
        }
    }

    private boolean isValidFileExtension(String fileExtension) {
        return RegexPatternUtils.getInstance()
                .getFileExtensionValidationPattern()
                .matcher(fileExtension)
                .matches();
    }

    @AutoJobPostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, value = "/file/pdf")
    @Operation(
            summary = "Convert a file to a PDF using Microsoft Office",
            description =
                    "This endpoint converts a given file to a PDF using MS Office.  Input:ANY"
                            + " Output:PDF Type:SISO")
    public ResponseEntity<byte[]> processFileToPDF(@ModelAttribute GeneralFile generalFile)
            throws Exception {
        MultipartFile inputFile = generalFile.getFileInput();
        File file = null;
        try {
            file = convertToPdf(inputFile);

            try (PDDocument doc = pdfDocumentFactory.load(file)) {
                return WebResponseUtils.pdfDocToWebResponse(
                        doc,
                        GeneralUtils.generateFilename(
                                inputFile.getOriginalFilename(), "_convertedToPDF.pdf"));
            }
        } finally {
            if (file != null) {
                FileUtils.deleteQuietly(file);
                if (file.getParent() != null) {
                    FileUtils.deleteQuietly(file.getParentFile());
                }
            }
        }
    }
}
