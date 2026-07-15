package com.gentleia.landingtarjetas.supermarket;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super/movements")
public class SuperItemMovementController {

    private final SupermarketService supermarketService;

    public SuperItemMovementController(SupermarketService supermarketService) {
        this.supermarketService = supermarketService;
    }

    @GetMapping
    public List<SuperItemStockMovementResponse> list(@RequestParam(required = false) Long itemId,
            @RequestParam(defaultValue = "50") int limit) {
        return supermarketService.listStockMovements(itemId, limit);
    }
}
