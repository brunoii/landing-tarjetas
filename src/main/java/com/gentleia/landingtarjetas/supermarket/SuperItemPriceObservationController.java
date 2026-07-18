package com.gentleia.landingtarjetas.supermarket;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
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
public class SuperItemPriceObservationController {

    private final SupermarketService supermarketService;

    public SuperItemPriceObservationController(SupermarketService supermarketService) {
        this.supermarketService = supermarketService;
    }

    @PostMapping("/items/{id}/price-observations")
    @ResponseStatus(HttpStatus.CREATED)
    public SuperItemPriceObservationResponse create(@PathVariable Long id,
            @Valid @RequestBody SuperItemPriceObservationRequest request) {
        return supermarketService.createPriceObservation(id, request);
    }

    @GetMapping("/price-observations")
    public List<SuperItemPriceObservationResponse> list(@RequestParam(required = false) Long itemId,
            @RequestParam(defaultValue = "50") int limit) {
        return supermarketService.listPriceObservations(itemId, limit);
    }
}
