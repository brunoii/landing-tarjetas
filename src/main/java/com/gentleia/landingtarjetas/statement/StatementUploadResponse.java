package com.gentleia.landingtarjetas.statement;

import java.util.List;

public record StatementUploadResponse(
        List<StatementUploadResultResponse> files
) {
}
