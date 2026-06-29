package com.gentleia.landingtarjetas.statement;

import com.gentleia.landingtarjetas.shared.ParsingStatus;

public record UploadedFileResponse(
        Long id,
        String originalFilename,
        String contentType,
        long sizeBytes,
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
                file.getParsingStatus(),
                file.getParsingMessage()
        );
    }
}
