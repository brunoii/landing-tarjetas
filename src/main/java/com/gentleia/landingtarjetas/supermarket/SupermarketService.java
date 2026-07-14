package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupermarketService {

    private final SuperCategoryRepository categoryRepository;
    private final SuperItemRepository itemRepository;

    public SupermarketService(SuperCategoryRepository categoryRepository, SuperItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public List<SuperCategoryResponse> listCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(SuperCategoryResponse::from)
                .toList();
    }

    @Transactional
    public SuperCategoryResponse createCategory(SuperCategoryRequest request) {
        ensureUniqueCategoryName(request.name(), null);
        SuperCategory category = new SuperCategory(request.name().trim());
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return SuperCategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public SuperCategoryResponse updateCategory(Long id, SuperCategoryRequest request) {
        SuperCategory category = getCategory(id);
        ensureUniqueCategoryName(request.name(), id);
        category.setName(request.name().trim());
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return SuperCategoryResponse.from(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        SuperCategory category = getCategory(id);
        if (itemRepository.existsByCategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar la categoría porque tiene productos asociados");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<SuperItemResponse> listItems() {
        return itemRepository.findActiveOrderedForList().stream()
                .map(SuperItemResponse::from)
                .toList();
    }

    @Transactional
    public SuperItemResponse createItem(SuperItemRequest request) {
        SuperCategory category = getActiveCategory(request.categoryId());
        SuperItem item = new SuperItem(request.name().trim(), category);
        item.setChecked(Boolean.TRUE.equals(request.checked()));
        item.setNotes(trimToNull(request.notes()));
        applyInventoryConfiguration(item, request);
        return SuperItemResponse.from(itemRepository.save(item));
    }

    @Transactional
    public SuperItemResponse updateItem(Long id, SuperItemRequest request) {
        SuperItem item = getActiveItem(id);
        item.setName(request.name().trim());
        item.setCategory(getActiveCategory(request.categoryId()));
        if (request.checked() != null) {
            item.setChecked(request.checked());
        }
        item.setNotes(trimToNull(request.notes()));
        applyInventoryConfiguration(item, request);
        return SuperItemResponse.from(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        getActiveItem(id).setActive(false);
    }

    @Transactional
    public SuperItemResponse updateItemChecked(Long id, Boolean checked) {
        SuperItem item = getActiveItem(id);
        item.setChecked(Boolean.TRUE.equals(checked));
        return SuperItemResponse.from(item);
    }

    @Transactional
    public void uncheckAllItems() {
        itemRepository.findByActiveTrueAndCheckedTrueOrderByNameAsc()
                .forEach(item -> item.setChecked(false));
    }

    private SuperCategory getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la categoría del super"));
    }

    private SuperCategory getActiveCategory(Long id) {
        SuperCategory category = getCategory(id);
        if (!category.isActive()) {
            throw new IllegalArgumentException("No se puede asignar una categoría inactiva al producto");
        }
        return category;
    }

    private SuperItem getActiveItem(Long id) {
        return itemRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el producto del super"));
    }

    private void ensureUniqueCategoryName(String name, Long currentId) {
        categoryRepository.findByNameIgnoreCase(name.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new IllegalArgumentException("Ya existe una categoría del super con ese nombre");
            }
        });
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void applyInventoryConfiguration(SuperItem item, SuperItemRequest request) {
        validateHabitualObjective(request.habitualObjective());
        item.setUnit(trimToNull(request.unit()));
        item.setHabitualObjective(request.habitualObjective());
    }

    private void validateHabitualObjective(BigDecimal habitualObjective) {
        if (habitualObjective != null && habitualObjective.signum() <= 0) {
            throw new IllegalArgumentException("Objetivo habitual: debe ser mayor a 0");
        }
    }
}
