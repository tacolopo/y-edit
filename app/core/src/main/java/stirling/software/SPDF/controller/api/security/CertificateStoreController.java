package stirling.software.SPDF.controller.api.security;

import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/security")
@Slf4j
@Tag(name = "Security", description = "Security APIs")
public class CertificateStoreController {

    @GetMapping("/certificates/system")
    @Operation(
            summary = "List signing certificates from smart card reader",
            description =
                    "Lists digital signing certificates accessible via PKCS#11 smart card reader."
                            + " Requires PIV middleware (OpenSC or ActivClient) to be installed.")
    public ResponseEntity<List<Map<String, String>>> listSystemCertificates() {
        List<Map<String, String>> certs = new ArrayList<>();

        String[] pkcs11Paths = {
            System.getenv("SystemRoot") + "\\System32\\opensc-pkcs11.dll",
            System.getenv("ProgramFiles") + "\\HID Global\\ActivClient\\acpkcs211.dll",
            System.getenv("ProgramFiles(x86)") + "\\HID Global\\ActivClient\\acpkcs211.dll",
            System.getenv("ProgramFiles")
                    + "\\OpenSC Project\\OpenSC\\pkcs11\\opensc-pkcs11.dll",
            "C:\\Windows\\System32\\opensc-pkcs11.dll",
        };

        for (String dllPath : pkcs11Paths) {
            if (dllPath == null || !new java.io.File(dllPath).exists()) {
                continue;
            }
            try {
                String pkcs11Config = "--name=SmartCardList\nlibrary=" + dllPath;
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

                KeyStore ks = KeyStore.getInstance("PKCS11", pkcs11Provider);
                ks.load(null, null);

                Enumeration<String> aliases = ks.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    if (!ks.isKeyEntry(alias)) {
                        continue;
                    }

                    java.security.cert.Certificate cert = ks.getCertificate(alias);
                    if (!(cert instanceof X509Certificate x509)) {
                        continue;
                    }

                    boolean[] keyUsage = x509.getKeyUsage();
                    if (keyUsage != null && !keyUsage[0]) {
                        continue;
                    }

                    Map<String, String> certInfo = new HashMap<>();
                    certInfo.put("alias", alias);
                    certInfo.put("subject", x509.getSubjectX500Principal().getName());
                    certInfo.put("issuer", x509.getIssuerX500Principal().getName());
                    certInfo.put("notBefore", x509.getNotBefore().toInstant().toString());
                    certInfo.put("notAfter", x509.getNotAfter().toInstant().toString());
                    certInfo.put("serialNumber", x509.getSerialNumber().toString(16));
                    certs.add(certInfo);
                }

                if (!certs.isEmpty()) {
                    break; // Found certs, no need to try other DLLs
                }

                Security.removeProvider(pkcs11Provider.getName());
            } catch (Exception e) {
                log.warn("PKCS#11 listing failed with {}: {}", dllPath, e.getMessage());
            }
        }

        return ResponseEntity.ok(certs);
    }
}
