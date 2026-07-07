package com.gentleia.landingtarjetas.statement;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfTextExtractionService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final StatementUploadProperties properties;

    public PdfTextExtractionService(StatementUploadProperties properties) {
        this.properties = properties;
    }

    public void validate(MultipartFile file) {
        if (file == null) {
            throw new IllegalArgumentException("Se requiere un archivo PDF");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo PDF está vacío");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException("El archivo PDF supera el límite de tamaño permitido");
        }
        if (!hasPdfContentType(file) && !hasPdfExtension(file)) {
            throw new IllegalArgumentException("Solo se aceptan archivos PDF");
        }
    }

    public String extractText(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            textStripper.setShouldSeparateByBeads(false);
            return textStripper.getText(document);
        }
    }

    public String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private boolean hasPdfContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && PDF_CONTENT_TYPE.equalsIgnoreCase(contentType.trim());
    }

    private boolean hasPdfExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }
}
