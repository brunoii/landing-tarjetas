package com.gentleia.landingtarjetas.supermarket;

public record SuperBarcodeLookupResponse(
        boolean found,
        String code,
        Long aliasId,
        String format,
        SuperItemResponse item
) {
    public static SuperBarcodeLookupResponse found(SuperItemBarcodeAlias alias) {
        return new SuperBarcodeLookupResponse(
                true,
                alias.getCode(),
                alias.getId(),
                alias.getFormat(),
                SuperItemResponse.from(alias.getItem())
        );
    }

    public static SuperBarcodeLookupResponse notFound(String code) {
        return new SuperBarcodeLookupResponse(false, code, null, null, null);
    }
}
