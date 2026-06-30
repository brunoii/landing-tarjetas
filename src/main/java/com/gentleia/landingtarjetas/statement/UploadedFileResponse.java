package com.gentleia.landingtarjetas.statement;

import java.time.Instant;

import com.gentleia.landingtarjetas.shared.ParsingStatus;

public record UploadedFileResponse(
        Long id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        Instant uploadedAt,
        ParsingStatus parsingStatus,
        String parsingMessage
) {
    public static UploadedFileResponse from(UploadedFile file) {
        if (file == null) {
            return null;
        }
        return new UploadedFileResponse(
                file.getId(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getChecksumSha256(),
                file.getCreatedAt(),
                file.getParsingStatus(),
                file.getParsingMessage()
        );
    }
}
