package stirling.software.SPDF.controller.api.security;

import java.awt.*;
import java.beans.PropertyEditorSupport;
import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.examples.signature.CreateSignatureBase;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.util.Matrix;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;

import stirling.software.SPDF.config.swagger.StandardPdfResponse;
import stirling.software.SPDF.model.api.security.SignPDFWithCertRequest;
import stirling.software.common.annotations.AutoJobPostMapping;
import stirling.software.common.service.CustomPDFDocumentFactory;
import stirling.software.common.service.ServerCertificateServiceInterface;
import stirling.software.common.util.ExceptionUtils;
import stirling.software.common.util.GeneralUtils;
import stirling.software.common.util.WebResponseUtils;

@RestController
@RequestMapping("/api/v1/security")
@Slf4j
@Tag(name = "Security", description = "Security APIs")
public class CertSignController {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(
                MultipartFile.class,
                new PropertyEditorSupport() {
                    @Override
                    public void setAsText(String text) throws IllegalArgumentException {
                        setValue(null);
                    }
                });
    }

    private final CustomPDFDocumentFactory pdfDocumentFactory;
    private final ServerCertificateServiceInterface serverCertificateService;

    public CertSignController(
            CustomPDFDocumentFactory pdfDocumentFactory,
            @Autowired(required = false)
                    ServerCertificateServiceInterface serverCertificateService) {
        this.pdfDocumentFactory = pdfDocumentFactory;
        this.serverCertificateService = serverCertificateService;
    }

    private static void sign(
            CustomPDFDocumentFactory pdfDocumentFactory,
            MultipartFile input,
            OutputStream output,
            CreateSignature instance,
            Boolean showSignature,
            Integer pageNumber,
            String name,
            String location,
            String reason,
            Boolean showLogo,
            String signatureFieldName,
            PDRectangle userRect)
            throws Exception {
        try (PDDocument doc = pdfDocumentFactory.load(input)) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(name);
            signature.setLocation(location);
            signature.setReason(reason);
            signature.setSignDate(Calendar.getInstance());

            // Determine signature rectangle from: user-drawn box > existing field > default
            PDRectangle sigRect = userRect;

            // Convert user-drawn rect from screen coords (top-left origin) to PDF coords (bottom-left origin)
            if (userRect != null) {
                float pageHeight = doc.getPage(pageNumber).getMediaBox().getHeight();
                sigRect = new PDRectangle(
                        userRect.getLowerLeftX(),
                        pageHeight - userRect.getLowerLeftY() - userRect.getHeight(),
                        userRect.getWidth(),
                        userRect.getHeight());
            }

            if (sigRect == null && signatureFieldName != null && !signatureFieldName.isBlank()) {
                PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
                if (acroForm != null) {
                    PDField field = acroForm.getField(signatureFieldName);
                    if (field instanceof PDSignatureField sigField) {
                        PDAnnotationWidget widget = sigField.getWidgets().get(0);
                        sigRect = widget.getRectangle();
                        PDPage fieldPage = widget.getPage();
                        if (fieldPage != null) {
                            pageNumber = doc.getPages().indexOf(fieldPage);
                        }
                        sigField.setValue(signature);
                    }
                }
            }

            // If we have a rectangle (user-drawn or from field), always show visible signature
            if (sigRect != null) {
                showSignature = true;
            }

            if (Boolean.TRUE.equals(showSignature)) {
                try (SignatureOptions signatureOptions = new SignatureOptions()) {
                    signatureOptions.setVisualSignature(
                            instance.createVisibleSignature(
                                    doc, signature, pageNumber, showLogo, sigRect));
                    signatureOptions.setPage(pageNumber);
                    doc.addSignature(signature, instance, signatureOptions);
                    doc.saveIncremental(output);
                }
            } else {
                doc.addSignature(signature, instance);
                doc.saveIncremental(output);
            }
        }
    }

    @AutoJobPostMapping(
            consumes = {
                MediaType.MULTIPART_FORM_DATA_VALUE,
                MediaType.APPLICATION_FORM_URLENCODED_VALUE
            },
            value = "/cert-sign")
    @StandardPdfResponse
    @Operation(
            summary = "Sign PDF with a Digital Certificate",
            description =
                    "This endpoint accepts a PDF file, a digital certificate and related"
                            + " information to sign the PDF. It then returns the digitally signed PDF"
                            + " file. Input:PDF Output:PDF Type:SISO")
    public ResponseEntity<byte[]> signPDFWithCert(@ModelAttribute SignPDFWithCertRequest request)
            throws Exception {
        MultipartFile pdf = request.getFileInput();
        Boolean showSignature = request.getShowSignature();
        String reason = request.getReason();
        String location = request.getLocation();
        String name = request.getName();
        // Convert 1-indexed page number (user input) to 0-indexed page number (API requirement)
        Integer pageNumber = request.getPageNumber() != null ? (request.getPageNumber() - 1) : null;
        Boolean showLogo = request.getShowLogo();

        KeyStore ks = null;
        String keystorePassword = "";
        Exception lastError = null;
        String pin = request.getPin();
        boolean usedPkcs11 = false;
        java.security.Provider signingProvider = null;

        // Strategy 1: On Windows, try the Windows Certificate Store first.
        // This picks up smart card certificates automatically via the built-in
        // minidriver — no PKCS#11 DLL required.
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            log.info("Trying Windows-MY certificate store (built-in smart card support)...");
            try {
                ks = KeyStore.getInstance("Windows-MY");
                ks.load(null, null);

                String foundAlias = null;
                int winTotalAliases = 0;
                java.util.Enumeration<String> aliases = ks.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    winTotalAliases++;
                    boolean isKey = ks.isKeyEntry(alias);
                    log.info("Windows-MY alias: {} (isKeyEntry={})", alias, isKey);
                    if (isKey && foundAlias == null) {
                        foundAlias = alias;
                    }
                }
                log.info("Windows-MY: {} total aliases found", winTotalAliases);

                if (foundAlias != null) {
                    if (request.getCertificateAlias() == null
                            || request.getCertificateAlias().isBlank()) {
                        request.setCertificateAlias(foundAlias);
                        if (name == null || name.isBlank() || "SPDF".equals(name)) {
                            name = foundAlias;
                        }
                    }
                    log.info("Using Windows-MY certificate: {}", request.getCertificateAlias());
                } else {
                    log.warn("No signing certificates found in Windows-MY store");
                    ks = null;
                }
            } catch (Exception e) {
                log.warn("Windows-MY keystore not available: {}", e.getMessage());
                lastError = e;
                ks = null;
            }
        }

        // Strategy 2: Fall back to PKCS#11 middleware DLLs
        if (ks == null) {
            log.info("Trying PKCS#11 smart card middleware...");

            String[] pkcs11Paths = {
                System.getenv("SystemRoot") + "\\System32\\opensc-pkcs11.dll",
                System.getenv("ProgramFiles") + "\\HID Global\\ActivClient\\acpkcs211.dll",
                System.getenv("ProgramFiles(x86)") + "\\HID Global\\ActivClient\\acpkcs211.dll",
                System.getenv("ProgramFiles") + "\\OpenSC Project\\OpenSC\\pkcs11\\opensc-pkcs11.dll",
                "C:\\Windows\\System32\\opensc-pkcs11.dll",
                "/usr/lib/opensc-pkcs11.so",
                "/usr/lib/x86_64-linux-gnu/opensc-pkcs11.so",
                "/usr/lib64/opensc-pkcs11.so",
            };

            String triedPaths = "";

            for (String dllPath : pkcs11Paths) {
                if (dllPath == null || !new java.io.File(dllPath).exists()) {
                    continue;
                }
                log.info("Found PKCS#11 library: {}", dllPath);
                triedPaths += dllPath + "; ";
                try {
                    String pkcs11Config = "--name=SmartCard\nlibrary=" + dllPath;
                    java.security.Provider baseProvider = Security.getProvider("SunPKCS11");
                    if (baseProvider == null) {
                        baseProvider =
                                (java.security.Provider)
                                        Class.forName("sun.security.pkcs11.SunPKCS11")
                                                .getDeclaredConstructor()
                                                .newInstance();
                    }
                    java.security.Provider pkcs11Provider = baseProvider.configure(pkcs11Config);
                    Security.addProvider(pkcs11Provider);

                    ks = KeyStore.getInstance("PKCS11", pkcs11Provider);
                    char[] pinChars =
                            (pin != null && !pin.isBlank()) ? pin.toCharArray() : null;
                    ks.load(null, pinChars);

                    String foundAlias = null;
                    String preferredAlias = null;
                    int certOnlyCount = 0;
                    java.util.Enumeration<String> aliases = ks.aliases();
                    while (aliases.hasMoreElements()) {
                        String alias = aliases.nextElement();
                        boolean isKey = ks.isKeyEntry(alias);
                        boolean isCert = ks.isCertificateEntry(alias);
                        log.info(
                                "PKCS#11 alias: {} (isKeyEntry={}, isCertificateEntry={})",
                                alias,
                                isKey,
                                isCert);
                        if (isKey) {
                            if (foundAlias == null) foundAlias = alias;
                            String lower = alias.toLowerCase();
                            if (lower.contains("digital signature")
                                    || lower.contains("signing")) {
                                preferredAlias = alias;
                            }
                        } else if (isCert) {
                            certOnlyCount++;
                        }
                    }
                    if (preferredAlias != null) foundAlias = preferredAlias;

                    if (foundAlias != null) {
                        if (request.getCertificateAlias() == null
                                || request.getCertificateAlias().isBlank()) {
                            request.setCertificateAlias(foundAlias);
                            if (name == null || name.isBlank() || "SPDF".equals(name)) {
                                name = foundAlias;
                            }
                        }
                        log.info("Using PKCS#11 certificate: {}", request.getCertificateAlias());
                        usedPkcs11 = true;
                        signingProvider = pkcs11Provider;
                        break;
                    } else {
                        if (certOnlyCount > 0) {
                            log.warn(
                                    "Found {} certificate(s) but no private keys via {}."
                                        + " PIN authentication may be required.",
                                    certOnlyCount,
                                    dllPath);
                        } else {
                            log.warn("No entries found on card via {}", dllPath);
                        }
                        ks = null;
                    }
                } catch (Exception e) {
                    log.error("PKCS#11 error with {}: {}", dllPath, e.getMessage(), e);
                    lastError = e;
                    ks = null;
                }
            }
        }

        if (ks == null) {
            String errorMsg = "No signing certificate found.";
            if (pin == null || pin.isBlank()) {
                errorMsg +=
                        " Your smart card may require a PIN — enter it in the PIN field"
                                + " and try again.";
            } else {
                errorMsg += " Ensure your smart card is inserted and the PIN is correct.";
            }
            errorMsg += " Also verify certificates are visible in certmgr.msc.";
            if (lastError != null) {
                errorMsg += " Error: " + lastError.getMessage();
            }
            log.error(errorMsg);
            throw ExceptionUtils.createIllegalArgumentException(
                    "error.noCertificateFound", errorMsg);
        }

        // Build user-specified signature rectangle if coordinates provided
        PDRectangle userRect = null;
        if (request.getSigX() != null && request.getSigY() != null
                && request.getSigWidth() != null && request.getSigHeight() != null) {
            userRect = new PDRectangle(
                    request.getSigX(), request.getSigY(),
                    request.getSigWidth(), request.getSigHeight());
            log.info("Using user-specified signature box: x={}, y={}, w={}, h={}",
                    request.getSigX(), request.getSigY(),
                    request.getSigWidth(), request.getSigHeight());
        }

        String keyPin =
                usedPkcs11 && pin != null && !pin.isBlank() ? pin : keystorePassword;
        CreateSignature createSignature = new CreateSignature(ks, keyPin.toCharArray());
        if (signingProvider != null) {
            createSignature.setSigningProvider(signingProvider);
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            sign(
                    pdfDocumentFactory,
                    pdf,
                    baos,
                    createSignature,
                    showSignature,
                    pageNumber,
                    name,
                    location,
                    reason,
                    showLogo,
                    request.getSignatureFieldName(),
                    userRect);
            // Return the signed PDF
            return WebResponseUtils.bytesToWebResponse(
                    baos.toByteArray(),
                    GeneralUtils.generateFilename(pdf.getOriginalFilename(), "_signed.pdf"));
        } finally {
            // Clean up temporary logo file to prevent leak
            if (createSignature.logoFile != null && createSignature.logoFile.exists()) {
                createSignature.logoFile.delete();
            }
        }
    }

    private MultipartFile validateFilePresent(
            MultipartFile file, String argumentName, String errorDescription) {
        if (file == null || file.isEmpty()) {
            throw ExceptionUtils.createIllegalArgumentException(
                    "error.invalidArgument",
                    "Invalid argument: {0}",
                    argumentName + " - " + errorDescription);
        }
        return file;
    }

    private PrivateKey getPrivateKeyFromPEM(byte[] pemBytes, String password)
            throws IOException, OperatorCreationException, PKCSException {
        try (PEMParser pemParser =
                new PEMParser(new InputStreamReader(new ByteArrayInputStream(pemBytes)))) {
            Object pemObject = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            PrivateKeyInfo pkInfo;
            if (pemObject instanceof PKCS8EncryptedPrivateKeyInfo pkcs8EncryptedPrivateKeyInfo) {
                InputDecryptorProvider decProv =
                        new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password.toCharArray());
                pkInfo = pkcs8EncryptedPrivateKeyInfo.decryptPrivateKeyInfo(decProv);
            } else if (pemObject instanceof PEMEncryptedKeyPair pemEncryptedKeyPair) {
                PEMDecryptorProvider decProv =
                        new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
                pkInfo = pemEncryptedKeyPair.decryptKeyPair(decProv).getPrivateKeyInfo();
            } else {
                pkInfo = ((PEMKeyPair) pemObject).getPrivateKeyInfo();
            }
            return converter.getPrivateKey(pkInfo);
        }
    }

    private Certificate getCertificateFromPEM(byte[] pemBytes)
            throws IOException, CertificateException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(pemBytes)) {
            return CertificateFactory.getInstance("X.509").generateCertificate(bis);
        }
    }

    class CreateSignature extends CreateSignatureBase {
        File logoFile;

        public CreateSignature(KeyStore keystore, char[] pin)
                throws KeyStoreException,
                        UnrecoverableKeyException,
                        NoSuchAlgorithmException,
                        IOException,
                        CertificateException {
            super(keystore, pin);
            ClassPathResource resource = new ClassPathResource("static/images/signature.png");
            try (InputStream is = resource.getInputStream()) {
                logoFile = Files.createTempFile("signature", ".png").toFile();
                FileUtils.copyInputStreamToFile(is, logoFile);
            } catch (IOException e) {
                log.error("Failed to load image signature file");
                throw e;
            }
        }

        public InputStream createVisibleSignature(
                PDDocument srcDoc,
                PDSignature signature,
                Integer pageNumber,
                Boolean showLogo,
                PDRectangle existingFieldRect)
                throws IOException {
            // modified from org.apache.pdfbox.examples.signature.CreateVisibleSignature2
            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage(srcDoc.getPage(pageNumber).getMediaBox());
                doc.addPage(page);
                PDAcroForm acroForm = new PDAcroForm(doc);
                doc.getDocumentCatalog().setAcroForm(acroForm);
                PDSignatureField signatureField = new PDSignatureField(acroForm);
                PDAnnotationWidget widget = signatureField.getWidgets().get(0);
                List<PDField> acroFormFields = acroForm.getFields();
                acroForm.setSignaturesExist(true);
                acroForm.setAppendOnly(true);
                acroForm.getCOSObject().setDirect(true);
                acroFormFields.add(signatureField);

                // Use existing field rectangle if signing into a specific field,
                // otherwise use default dimensions
                PDRectangle rect =
                        existingFieldRect != null
                                ? existingFieldRect
                                : new PDRectangle(0, 0, 200, 50);

                widget.setRectangle(rect);

                // from PDVisualSigBuilder.createHolderForm()
                PDStream stream = new PDStream(doc);
                PDFormXObject form = new PDFormXObject(stream);
                PDResources res = new PDResources();
                form.setResources(res);
                form.setFormType(1);
                PDRectangle bbox = new PDRectangle(rect.getWidth(), rect.getHeight());
                float height = bbox.getHeight();
                form.setBBox(bbox);
                PDFont font = new PDType1Font(FontName.TIMES_BOLD);

                // from PDVisualSigBuilder.createAppearanceDictionary()
                PDAppearanceDictionary appearance = new PDAppearanceDictionary();
                appearance.getCOSObject().setDirect(true);
                PDAppearanceStream appearanceStream = new PDAppearanceStream(form.getCOSObject());
                appearance.setNormalAppearance(appearanceStream);
                widget.setAppearance(appearance);

                try (PDPageContentStream cs = new PDPageContentStream(doc, appearanceStream)) {
                    if (Boolean.TRUE.equals(showLogo)) {
                        cs.saveGraphicsState();
                        PDExtendedGraphicsState extState = new PDExtendedGraphicsState();
                        extState.setBlendMode(BlendMode.MULTIPLY);
                        extState.setNonStrokingAlphaConstant(0.5f);
                        cs.setGraphicsStateParameters(extState);
                        cs.transform(Matrix.getScaleInstance(0.08f, 0.08f));
                        PDImageXObject img =
                                PDImageXObject.createFromFileByExtension(logoFile, doc);
                        cs.drawImage(img, 100, 0);
                        cs.restoreGraphicsState();
                    }

                    // Dynamic font size: fit all lines within the box height
                    String reason = signature.getReason();
                    int lineCount = 2; // "Signed by ..." and date
                    if (reason != null && !reason.isEmpty()) lineCount++;
                    // Each line needs fontSize * 1.5 (leading), plus one leading of top margin
                    // Total = (lineCount + 1) * fontSize * 1.5 <= height
                    float fontSize = Math.min(10f, height / ((lineCount + 1) * 1.5f));
                    fontSize = Math.max(3f, fontSize); // floor: below 3pt is unreadable
                    float leading = fontSize * 1.5f;
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.setNonStrokingColor(Color.black);
                    cs.newLineAtOffset(fontSize, height - leading);
                    cs.setLeading(leading);

                    // Use user-entered name from PDSignature; fall back to certificate CN
                    String signedByName = signature.getName();
                    if (signedByName == null || signedByName.isBlank()) {
                        java.security.cert.Certificate[] chain = getCertificateChain();
                        if (chain != null && chain.length > 0 && chain[0] instanceof X509Certificate cert) {
                            X500Name x500Name = new X500Name(cert.getSubjectX500Principal().getName());
                            RDN[] cnRdns = x500Name.getRDNs(BCStyle.CN);
                            if (cnRdns.length > 0) {
                                signedByName = IETFUtils.valueToString(cnRdns[0].getFirst().getValue());
                            } else {
                                signedByName = "Unknown Signer";
                            }
                        } else {
                            signedByName = "Unknown Signer";
                        }
                    }

                    String date = signature.getSignDate().getTime().toString();

                    cs.showText("Signed by " + signedByName);
                    cs.newLine();
                    cs.showText(date);
                    cs.newLine();
                    if (reason != null && !reason.isEmpty()) {
                        cs.showText(reason);
                    }

                    cs.endText();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                doc.save(baos);
                return new ByteArrayInputStream(baos.toByteArray());
            }
        }
    }
}
