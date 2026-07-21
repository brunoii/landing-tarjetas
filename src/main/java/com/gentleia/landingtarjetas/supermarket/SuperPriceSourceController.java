package com.gentleia.landingtarjetas.supermarket;

import java.util.List;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super/price-sources")
public class SuperPriceSourceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuperPriceSourceController.class);

    private final SupermarketService supermarketService;

    public SuperPriceSourceController(SupermarketService supermarketService) {
        this.supermarketService = supermarketService;
    }

    @GetMapping
    public List<SuperPriceSourceResponse> list() {
        List<SuperPriceSourceResponse> priceSources = supermarketService.listPriceSources();
        LOGGER.info("Listed {} active super price sources", priceSources.size());
        return priceSources;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuperPriceSourceResponse create(@Valid @RequestBody SuperPriceSourceRequest request) {
        SuperPriceSourceResponse response = supermarketService.createPriceSource(request);
        LOGGER.info("Created super price source id={} name={}", response.id(), response.name());
        return response;
    }
}
