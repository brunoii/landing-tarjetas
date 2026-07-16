package com.gentleia.landingtarjetas.supermarket;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super/suggested-list")
public class SuperSuggestedListController {

    private final SupermarketService supermarketService;

    public SuperSuggestedListController(SupermarketService supermarketService) {
        this.supermarketService = supermarketService;
    }

    @GetMapping
    public List<SuperSuggestedItemResponse> list() {
        return supermarketService.listSuggestedItems();
    }
}
