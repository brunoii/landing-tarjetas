package com.gentleia.landingtarjetas.supermarket;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super")
public class SuperItemBarcodeAliasController {

    private final SupermarketService supermarketService;

    public SuperItemBarcodeAliasController(SupermarketService supermarketService) {
        this.supermarketService = supermarketService;
    }

    @GetMapping("/barcode-aliases")
    public SuperBarcodeLookupResponse lookup(@RequestParam String code) {
        return supermarketService.lookupBarcodeAlias(code);
    }

    @PostMapping("/items/{itemId}/barcode-aliases")
    @ResponseStatus(HttpStatus.CREATED)
    public SuperItemBarcodeAliasResponse attach(@PathVariable Long itemId, @Valid @RequestBody SuperItemBarcodeAliasRequest request) {
        return supermarketService.attachBarcodeAlias(itemId, request);
    }

    @DeleteMapping("/items/{itemId}/barcode-aliases/{aliasId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long itemId, @PathVariable Long aliasId) {
        supermarketService.deactivateBarcodeAlias(itemId, aliasId);
    }
}
