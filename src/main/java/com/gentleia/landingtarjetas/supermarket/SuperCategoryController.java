package com.gentleia.landingtarjetas.supermarket;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super/categories")
public class SuperCategoryController {

    private final SupermarketService supermarketService;

    public SuperCategoryController(SupermarketService supermarketService) {
        this.supermarketService = supermarketService;
    }

    @GetMapping
    public List<SuperCategoryResponse> list() {
        return supermarketService.listCategories();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuperCategoryResponse create(@Valid @RequestBody SuperCategoryRequest request) {
        return supermarketService.createCategory(request);
    }

    @PutMapping("/{id}")
    public SuperCategoryResponse update(@PathVariable Long id, @Valid @RequestBody SuperCategoryRequest request) {
        return supermarketService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        supermarketService.deleteCategory(id);
    }
}
