package stirling.software.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import com.documents4j.api.DocumentType;
import com.documents4j.api.IConverter;
import com.documents4j.job.LocalConverter;

import lombok.extern.slf4j.Slf4j;

/**
 * Converts office documents to/from PDF using Microsoft Office via documents4j. Requires MS Office
 * (Word, Excel, PowerPoint) installed on the Windows host. On non-Windows systems, this converter
 * reports itself as unavailable.
 */
@Slf4j
@Component
public class MsOfficeConverter {

    private static final Set<String> WORD_EXTENSIONS =
            Set.of("doc", "docx", "odt", "rtf", "txt", "html", "htm");
    private static final Set<String> EXCEL_EXTENSIONS = Set.of("xls", "xlsx", "ods", "csv");
    private static final Set<String> POWERPOINT_EXTENSIONS = Set.of("ppt", "pptx", "odp");

    private IConverter converter;
    private boolean available;

    @PostConstruct
    public void init() {
        if (!isWindows()) {
            log.info("MsOfficeConverter: Not running on Windows, Office conversion disabled");
            available = false;
            return;
        }

        try {
            converter =
                    LocalConverter.builder()
                            .workerPool(1, 2, 2, TimeUnit.MINUTES)
                            .processTimeout(5, TimeUnit.MINUTES)
                            .build();
            available = true;
            log.info("MsOfficeConverter: MS Office converter initialized successfully");
        } catch (Exception e) {
            log.warn(
                    "MsOfficeConverter: Failed to initialize MS Office converter: {}",
                    e.getMessage());
            available = false;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (converter != null) {
            try {
                converter.shutDown();
            } catch (Exception e) {
                log.warn("MsOfficeConverter: Error during shutdown: {}", e.getMessage());
            }
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Convert an office file (Word, Excel, PowerPoint) to PDF.
     *
     * @param inputFile the source office document
     * @param extension the file extension (e.g. "docx", "xlsx", "pptx")
     * @return a temporary File containing the PDF output
     */
    public File convertToPdf(File inputFile, String extension) throws IOException {
        if (!available) {
            throw new IOException("MS Office converter is not available");
        }

        String ext = extension.toLowerCase(Locale.ROOT);
        DocumentType sourceType = resolveSourceType(ext);
        if (sourceType == null) {
            throw new IOException("Unsupported file extension for Office conversion: " + ext);
        }

        Path outputPath = Files.createTempFile("msoffice_", ".pdf");
        try (InputStream in = new FileInputStream(inputFile);
                OutputStream out = new FileOutputStream(outputPath.toFile())) {
            boolean success =
                    converter
                            .convert(in)
                            .as(sourceType)
                            .to(out)
                            .as(DocumentType.PDF)
                            .execute();

            if (!success) {
                Files.deleteIfExists(outputPath);
                throw new IOException("MS Office conversion failed for extension: " + ext);
            }
        } catch (Exception e) {
            Files.deleteIfExists(outputPath);
            throw new IOException("MS Office conversion error: " + e.getMessage(), e);
        }

        return outputPath.toFile();
    }

    /**
     * Convert a PDF to an office format (currently supports PDF to Word/DOCX).
     *
     * @param inputFile the source PDF file
     * @param outputFormat the target format (e.g. "docx")
     * @return a temporary File containing the converted output
     */
    public File convertFromPdf(File inputFile, String outputFormat) throws IOException {
        if (!available) {
            throw new IOException("MS Office converter is not available");
        }

        String fmt = outputFormat.toLowerCase(Locale.ROOT);
        DocumentType targetType = resolveTargetType(fmt);
        if (targetType == null) {
            throw new IOException("Unsupported output format for PDF conversion: " + fmt);
        }

        Path outputPath = Files.createTempFile("msoffice_", "." + fmt);
        try (InputStream in = new FileInputStream(inputFile);
                OutputStream out = new FileOutputStream(outputPath.toFile())) {
            boolean success =
                    converter
                            .convert(in)
                            .as(DocumentType.PDF)
                            .to(out)
                            .as(targetType)
                            .execute();

            if (!success) {
                Files.deleteIfExists(outputPath);
                throw new IOException("MS Office PDF-to-" + fmt + " conversion failed");
            }
        } catch (Exception e) {
            Files.deleteIfExists(outputPath);
            throw new IOException("MS Office conversion error: " + e.getMessage(), e);
        }

        return outputPath.toFile();
    }

    public boolean canConvertToPdf(String extension) {
        if (!available || extension == null) return false;
        String ext = extension.toLowerCase(Locale.ROOT);
        return WORD_EXTENSIONS.contains(ext)
                || EXCEL_EXTENSIONS.contains(ext)
                || POWERPOINT_EXTENSIONS.contains(ext);
    }

    private DocumentType resolveSourceType(String ext) {
        if (WORD_EXTENSIONS.contains(ext)) {
            return DocumentType.MS_WORD;
        } else if (EXCEL_EXTENSIONS.contains(ext)) {
            return DocumentType.MS_EXCEL;
        } else if (POWERPOINT_EXTENSIONS.contains(ext)) {
            return DocumentType.MS_POWERPOINT;
        }
        return null;
    }

    private DocumentType resolveTargetType(String format) {
        return switch (format) {
            case "doc", "docx" -> DocumentType.MS_WORD;
            case "xls", "xlsx" -> DocumentType.MS_EXCEL;
            case "ppt", "pptx" -> DocumentType.MS_POWERPOINT;
            case "rtf" -> DocumentType.RTF;
            default -> null;
        };
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
