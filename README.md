# Y-Edit

Self-contained Windows PDF editor with 50+ tools. Forked from [Stirling-PDF](https://github.com/Stirling-Tools/Stirling-PDF) (MIT-licensed core).

## Key Differences from Stirling-PDF

- **MS Office instead of LibreOffice** — Uses your already-installed Microsoft Office for document conversions via COM automation (documents4j). No 500MB LibreOffice bundle needed.
- **Self-contained installer** — All tools bundled: custom JRE, QPDF, Tesseract OCR, Ghostscript, embedded Python + weasyprint/opencv/ocrmypdf.
- **Desktop-first** — Tauri (Rust) shell with native WebView2. No browser needed.
- **No SaaS/auth** — Pure local desktop app. No login, no cloud, no telemetry.

## Features

- Merge, split, rotate, reorder PDF pages
- X.509 certificate signing (PEM, PKCS12, JKS)
- Digital signatures, watermarks, stamps
- OCR (Tesseract + OCRmyPDF)
- Office to PDF (Word, Excel, PowerPoint)
- PDF to Word/DOCX conversion
- HTML/URL to PDF (Weasyprint)
- Compress, repair (QPDF)
- PDF/A archival conversion (Ghostscript)
- Encrypt/decrypt, permissions
- Redaction, sanitization
- Image extraction and scan processing
- Metadata editing, form filling
- And 30+ more tools...

## Requirements

- Windows 10/11
- Microsoft Office (Word, Excel, PowerPoint)

## Building

```bash
# Build Spring Boot JAR
./gradlew :stirling-pdf:bootJar -x test

# Create custom JRE
jlink --no-header-files --no-man-pages --compress=zip-6 \
  --add-modules java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,java.xml.crypto,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.unsupported \
  --output frontend/src-tauri/runtime/jre

# Install frontend deps
cd frontend && npm ci

# Build Tauri app
npx tauri build --bundles nsis
```

## License

MIT — see [LICENSE](LICENSE)
