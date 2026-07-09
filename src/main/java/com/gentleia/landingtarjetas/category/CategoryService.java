package com.gentleia.landingtarjetas.category;

import java.util.List;

import com.gentleia.landingtarjetas.manualexpense.ManualExpenseRepository;
import com.gentleia.landingtarjetas.transaction.StatementTransactionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoryService {

    private static final String HEX_COLOR_PATTERN = "#[0-9A-Fa-f]{6}";

    private final CategoryRepository categoryRepository;
    private final StatementTransactionRepository transactionRepository;
    private final ManualExpenseRepository manualExpenseRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           StatementTransactionRepository transactionRepository,
                           ManualExpenseRepository manualExpenseRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.manualExpenseRepository = manualExpenseRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        ensureUniqueName(request.name(), null);
        Category category = new Category(request.name().trim(), safeColorOrNull(request.color()));
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        ensureUniqueName(request.name(), id);
        category.setName(request.name().trim());
        category.setColor(safeColorOrNull(request.color()));
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return CategoryResponse.from(category);
    }

    @Transactional
    public void deleteSafely(Long id) {
        Category category = getCategory(id);
        if (transactionRepository.existsByCategoryId(id) || manualExpenseRepository.existsByCategoryId(id)) {
            category.setActive(false);
            return;
        }
        categoryRepository.delete(category);
    }

    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la categoría"));
    }

    private void ensureUniqueName(String name, Long currentId) {
        categoryRepository.findByNameIgnoreCase(name.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
            }
        });
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String safeColorOrNull(String value) {
        String color = trimToNull(value);
        if (color == null) {
            return null;
        }
        if (!color.matches(HEX_COLOR_PATTERN)) {
            throw new IllegalArgumentException("El color de la categoría debe estar vacío o ser hexadecimal, por ejemplo #38bdf8");
        }
        return color;
    }
}
