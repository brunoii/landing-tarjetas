package com.gentleia.landingtarjetas.supermarket;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super/items")
public class SuperItemController {

    private final SupermarketService supermarketService;

    public SuperItemController(SupermarketService supermarketService) {
        this.supermarketService = supermarketService;
    }

    @GetMapping
    public List<SuperItemResponse> list() {
        return supermarketService.listItems();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuperItemResponse create(@Valid @RequestBody SuperItemRequest request) {
        return supermarketService.createItem(request);
    }

    @PutMapping("/{id}")
    public SuperItemResponse update(@PathVariable Long id, @Valid @RequestBody SuperItemRequest request) {
        return supermarketService.updateItem(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        supermarketService.deleteItem(id);
    }

    @PatchMapping("/{id}/checked")
    public SuperItemResponse updateChecked(@PathVariable Long id, @Valid @RequestBody SuperItemCheckedRequest request) {
        return supermarketService.updateItemChecked(id, request.checked());
    }

    @PostMapping("/uncheck-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uncheckAll() {
        supermarketService.uncheckAllItems();
    }
}
