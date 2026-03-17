package stirling.software.SPDF.controller.api.security;

import java.security.KeyStore;
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
            summary = "List system signing certificates",
            description =
                    "Lists digital signing certificates from the Windows certificate store."
                            + " Only returns certificates with the digitalSignature key usage bit."
                            + " Requires Windows with SunMSCAPI provider.")
    public ResponseEntity<List<Map<String, String>>> listSystemCertificates() {
        List<Map<String, String>> certs = new ArrayList<>();

        try {
            KeyStore ks = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
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

                // Filter for certificates with digitalSignature key usage
                boolean[] keyUsage = x509.getKeyUsage();
                if (keyUsage != null && !keyUsage[0]) {
                    // keyUsage[0] is digitalSignature
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
        } catch (java.security.NoSuchProviderException e) {
            log.warn("SunMSCAPI provider not available - not running on Windows");
            return ResponseEntity.ok(certs); // Return empty list on non-Windows
        } catch (Exception e) {
            log.error("Error listing system certificates: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(certs);
    }
}
