package com.gentleia.landingtarjetas.statement;

import java.util.List;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.ParsingStatus;
import com.gentleia.landingtarjetas.shared.Provider;

public record StatementUploadResultResponse(
        UploadedFileResponse uploadedFile,
        Provider detectedProvider,
        CardBrand detectedCardBrand,
        ParsingStatus parsingStatus,
        String parserName,
        List<String> warnings,
        String error,
        StatementDetailResponse draftStatement
) {
}
