package stirling.software.common.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;

import io.github.pixee.security.Filenames;

import lombok.extern.slf4j.Slf4j;

import stirling.software.common.configuration.RuntimePathConfig;
import stirling.software.common.util.ProcessExecutor.ProcessExecutorResult;

@Slf4j
public class PDFToFile {

    private static final Pattern PATTERN =
            Pattern.compile("(!\\[.*?\\])\\((?!images/)([^/)][^)]*?)\\)");
    private final TempFileManager tempFileManager;
    private final RuntimePathConfig runtimePathConfig;
    private final MsOfficeConverter msOfficeConverter;

    public PDFToFile(TempFileManager tempFileManager) {
        this(tempFileManager, null, null);
    }

    public PDFToFile(TempFileManager tempFileManager, RuntimePathConfig runtimePathConfig) {
        this(tempFileManager, runtimePathConfig, null);
    }

    public PDFToFile(
            TempFileManager tempFileManager,
            RuntimePathConfig runtimePathConfig,
            MsOfficeConverter msOfficeConverter) {
        this.tempFileManager = tempFileManager;
        this.runtimePathConfig = runtimePathConfig;
        this.msOfficeConverter = msOfficeConverter;
    }

    public ResponseEntity<byte[]> processPdfToMarkdown(MultipartFile inputFile)
            throws IOException, InterruptedException {
        if (!MediaType.APPLICATION_PDF_VALUE.equals(inputFile.getContentType())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        MutableDataSet options =
                new MutableDataSet()
                        .set(
                                FlexmarkHtmlConverter.MAX_BLANK_LINES,
                                2) // Control max consecutive blank lines
                        .set(
                                FlexmarkHtmlConverter.MAX_TRAILING_BLANK_LINES,
                                1) // Control trailing blank lines
                        .set(
                                FlexmarkHtmlConverter.SETEXT_HEADINGS,
                                true) // Use Setext headings for h1 and h2
                        .set(
                                FlexmarkHtmlConverter.OUTPUT_UNKNOWN_TAGS,
                                false) // Don't output HTML for unknown tags
                        .set(
                                FlexmarkHtmlConverter.TYPOGRAPHIC_QUOTES,
                                true) // Convert quotation marks
                        .set(
                                FlexmarkHtmlConverter.BR_AS_PARA_BREAKS,
                                true) // Convert <br> to paragraph breaks
                        .set(FlexmarkHtmlConverter.CODE_INDENT, "    "); // Indent for code blocks

        FlexmarkHtmlConverter htmlToMarkdownConverter =
                FlexmarkHtmlConverter.builder(options).build();

        String originalPdfFileName = Filenames.toSimpleFileName(inputFile.getOriginalFilename());
        String pdfBaseName = originalPdfFileName;
        if (originalPdfFileName.contains(".")) {
            pdfBaseName = originalPdfFileName.substring(0, originalPdfFileName.lastIndexOf('.'));
        }

        byte[] fileBytes;
        String fileName;

        try (TempFile tempInputFile = new TempFile(tempFileManager, ".pdf");
                TempDirectory tempOutputDir = new TempDirectory(tempFileManager)) {
            inputFile.transferTo(tempInputFile.getFile());

            List<String> command =
                    new ArrayList<>(
                            Arrays.asList(
                                    "pdftohtml",
                                    "-s",
                                    "-noframes",
                                    "-c",
                                    tempInputFile.getAbsolutePath(),
                                    pdfBaseName));

            ProcessExecutorResult returnCode =
                    ProcessExecutor.getInstance(ProcessExecutor.Processes.PDFTOHTML)
                            .runCommandWithOutputHandling(
                                    command, tempOutputDir.getPath().toFile());
            // Process HTML files to Markdown
            File[] outputFiles =
                    Objects.requireNonNull(tempOutputDir.getPath().toFile().listFiles());
            List<File> markdownFiles = new ArrayList<>();
            List<File> imageFiles = new ArrayList<>();

            // Convert HTML files to Markdown and collect image files
            for (File outputFile : outputFiles) {
                if (outputFile.getName().endsWith(".html")) {
                    String html = Files.readString(outputFile.toPath());
                    String markdown = htmlToMarkdownConverter.convert(html);

                    // Update image references to point to images/ folder
                    markdown = updateImageReferences(markdown);

                    String mdFileName = outputFile.getName().replace(".html", ".md");
                    File mdFile = new File(tempOutputDir.getPath().toFile(), mdFileName);
                    Files.writeString(mdFile.toPath(), markdown);
                    markdownFiles.add(mdFile);
                } else if (!outputFile.getName().endsWith(".md")) {
                    // Collect non-HTML, non-MD files as images/assets
                    imageFiles.add(outputFile);
                }
            }

            // Always create a ZIP file
            fileName = pdfBaseName + "ToMarkdown.zip";
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
                // Add markdown files to root of ZIP
                for (File mdFile : markdownFiles) {
                    ZipEntry mdEntry = new ZipEntry(mdFile.getName());
                    zipOutputStream.putNextEntry(mdEntry);
                    Files.copy(mdFile.toPath(), zipOutputStream);
                    zipOutputStream.closeEntry();
                }

                // Add images and other assets to images/ folder
                for (File imageFile : imageFiles) {
                    ZipEntry assetEntry = new ZipEntry("images/" + imageFile.getName());
                    zipOutputStream.putNextEntry(assetEntry);
                    Files.copy(imageFile.toPath(), zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }

            fileBytes = byteArrayOutputStream.toByteArray();
        }
        return WebResponseUtils.bytesToWebResponse(
                fileBytes, fileName, MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * Updates image references in markdown to point to the images/ folder. Matches patterns like
     * ![alt](filename.png) and converts to ![alt](images/filename.png)
     */
    private String updateImageReferences(String markdown) {
        // Match markdown image syntax: ![alt text](image.png)
        // Only update if the path doesn't already start with images/
        return PATTERN.matcher(markdown).replaceAll("$1(images/$2)");
    }

    public ResponseEntity<byte[]> processPdfToHtml(MultipartFile inputFile)
            throws IOException, InterruptedException {
        if (!MediaType.APPLICATION_PDF_VALUE.equals(inputFile.getContentType())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // Get the original PDF file name without the extension
        String originalPdfFileName = Filenames.toSimpleFileName(inputFile.getOriginalFilename());
        String pdfBaseName = originalPdfFileName;
        if (originalPdfFileName.contains(".")) {
            pdfBaseName = originalPdfFileName.substring(0, originalPdfFileName.lastIndexOf('.'));
        }

        byte[] fileBytes;
        String fileName;

        try (TempFile inputFileTemp = new TempFile(tempFileManager, ".pdf");
                TempDirectory outputDirTemp = new TempDirectory(tempFileManager)) {

            Path tempInputFile = inputFileTemp.getPath();
            Path tempOutputDir = outputDirTemp.getPath();

            // Save the uploaded file to a temporary location
            inputFile.transferTo(tempInputFile);

            // Run the pdftohtml command with complex output
            List<String> command =
                    new ArrayList<>(
                            Arrays.asList(
                                    "pdftohtml", "-c", tempInputFile.toString(), pdfBaseName));

            ProcessExecutorResult returnCode =
                    ProcessExecutor.getInstance(ProcessExecutor.Processes.PDFTOHTML)
                            .runCommandWithOutputHandling(command, tempOutputDir.toFile());

            // Get output files
            File[] outputFiles = Objects.requireNonNull(tempOutputDir.toFile().listFiles());

            // Return output files in a ZIP archive
            fileName = pdfBaseName + "ToHtml.zip";
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
                for (File outputFile : outputFiles) {
                    ZipEntry entry = new ZipEntry(outputFile.getName());
                    zipOutputStream.putNextEntry(entry);
                    try (FileInputStream fis = new FileInputStream(outputFile)) {
                        IOUtils.copy(fis, zipOutputStream);
                    } catch (IOException e) {
                        log.error("Exception writing zip entry", e);
                    }
                    zipOutputStream.closeEntry();
                }
            } catch (IOException e) {
                log.error("Exception writing zip", e);
            }
            fileBytes = byteArrayOutputStream.toByteArray();
        }

        return WebResponseUtils.bytesToWebResponse(
                fileBytes, fileName, MediaType.APPLICATION_OCTET_STREAM);
    }

    public ResponseEntity<byte[]> processPdfToOfficeFormat(
            MultipartFile inputFile, String outputFormat, String libreOfficeFilter)
            throws IOException, InterruptedException {

        if (!MediaType.APPLICATION_PDF_VALUE.equals(inputFile.getContentType())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        String originalPdfFileName = Filenames.toSimpleFileName(inputFile.getOriginalFilename());
        if (originalPdfFileName == null || originalPdfFileName.trim().isEmpty()) {
            originalPdfFileName = "output.pdf";
        }
        String pdfBaseName = originalPdfFileName;
        if (originalPdfFileName.contains(".")) {
            pdfBaseName = originalPdfFileName.substring(0, originalPdfFileName.lastIndexOf('.'));
        }

        // Map output formats — strip LibreOffice-specific suffixes
        String cleanFormat = resolvePrimaryExtension(outputFormat);
        List<String> allowedFormats =
                Arrays.asList("doc", "docx", "ppt", "pptx", "rtf", "txt");
        if (!allowedFormats.contains(cleanFormat)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (msOfficeConverter == null || !msOfficeConverter.isAvailable()) {
            throw new IOException(
                    "MS Office is not available for PDF-to-Office conversion. "
                            + "Please install Microsoft Office.");
        }

        byte[] fileBytes;
        String fileName;

        try (TempFile inputFileTemp = new TempFile(tempFileManager, ".pdf")) {
            inputFile.transferTo(inputFileTemp.getFile());

            File outputFile =
                    msOfficeConverter.convertFromPdf(inputFileTemp.getFile(), cleanFormat);
            try {
                fileName = pdfBaseName + "." + cleanFormat;
                fileBytes = FileUtils.readFileToByteArray(outputFile);
            } finally {
                FileUtils.deleteQuietly(outputFile);
            }
        }

        return WebResponseUtils.bytesToWebResponse(
                fileBytes, fileName, MediaType.APPLICATION_OCTET_STREAM);
    }

    private String resolvePrimaryExtension(String outputFormat) {
        if (outputFormat == null) {
            return "";
        }
        int colonIndex = outputFormat.indexOf(':');
        return colonIndex > 0 ? outputFormat.substring(0, colonIndex) : outputFormat;
    }
}
