package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupermarketService {

    private static final String MANUAL_SOURCE = "MANUAL";
    private static final String QUICK_SOURCE = "QUICK";
    private static final int DEFAULT_MOVEMENT_LIMIT = 50;
    private static final int MAX_MOVEMENT_LIMIT = 100;

    private final SuperCategoryRepository categoryRepository;
    private final SuperItemRepository itemRepository;
    private final SuperItemStockMovementRepository stockMovementRepository;

    public SupermarketService(SuperCategoryRepository categoryRepository, SuperItemRepository itemRepository,
            SuperItemStockMovementRepository stockMovementRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.stockMovementRepository = stockMovementRepository;
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
        ensureNoGenericStockMutation(request);
        SuperCategory category = getActiveCategory(request.categoryId());
        SuperItem item = new SuperItem(request.name().trim(), category);
        item.setChecked(Boolean.TRUE.equals(request.checked()));
        item.setNotes(trimToNull(request.notes()));
        applyInventoryConfiguration(item, request);
        return SuperItemResponse.from(itemRepository.save(item));
    }

    @Transactional
    public SuperItemResponse updateItem(Long id, SuperItemRequest request) {
        ensureNoGenericStockMutation(request);
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
    public SuperItemResponse adjustItemStock(Long id, SuperItemStockAdjustmentRequest request) {
        SuperItem item = getActiveItemForStockCommand(id);
        BigDecimal previousStock = item.getCurrentStock();
        item.setCurrentStock(request.currentStock());
        stockMovementRepository.save(new SuperItemStockMovement(item, previousStock, request.currentStock()));
        return SuperItemResponse.from(item);
    }

    @Transactional
    public SuperItemResponse purchaseItemStock(Long id, SuperItemStockMovementRequest request) {
        SuperItem item = getActiveItemForStockCommand(id);
        BigDecimal previousStock = requireKnownStock(item);
        BigDecimal resultingStock = previousStock.add(request.quantity());
        return applyStockMovement(item, SuperItemStockMovement.MovementType.PURCHASE, request.quantity(), request.notes(), MANUAL_SOURCE,
                previousStock, resultingStock, false);
    }

    @Transactional
    public SuperItemResponse consumeItemStock(Long id, SuperItemStockMovementRequest request) {
        SuperItem item = getActiveItemForStockCommand(id);
        BigDecimal previousStock = requireKnownStock(item);
        BigDecimal resultingStock = previousStock.subtract(request.quantity());
        return applyStockMovement(item, SuperItemStockMovement.MovementType.CONSUMPTION, request.quantity(), request.notes(), MANUAL_SOURCE,
                previousStock, resultingStock, request.allowsNegativeStock());
    }

    @Transactional
    public SuperItemResponse quickConsumeItemStock(Long id, SuperItemQuickConsumptionRequest request) {
        SuperItem item = getActiveItemForStockCommand(id);
        BigDecimal previousStock = requireKnownStock(item);
        BigDecimal quantity = item.getQuickQuantity();
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Configure una cantidad rápida positiva antes de usar consumo rápido");
        }
        BigDecimal resultingStock = previousStock.subtract(quantity);
        return applyStockMovement(item, SuperItemStockMovement.MovementType.QUICK_CONSUMPTION, quantity, null, QUICK_SOURCE,
                previousStock, resultingStock, request.allowsNegativeStock());
    }

    @Transactional(readOnly = true)
    public List<SuperItemStockMovementResponse> listStockMovements(Long itemId, int requestedLimit) {
        int limit = normalizeMovementLimit(requestedLimit);
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<SuperItemStockMovement> movements = itemId == null
                ? stockMovementRepository.findRecent(pageRequest)
                : stockMovementRepository.findRecentByItemId(itemId, pageRequest);
        return movements.stream()
                .map(SuperItemStockMovementResponse::from)
                .toList();
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

    private SuperItem getActiveItemForStockCommand(Long id) {
        return itemRepository.findActiveByIdForStockCommand(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el producto del super"));
    }

    private BigDecimal requireKnownStock(SuperItem item) {
        if (item.getCurrentStock() == null) {
            throw new IllegalArgumentException("Inicialice el stock con un ajuste antes de registrar movimientos");
        }
        return item.getCurrentStock();
    }

    private SuperItemResponse applyStockMovement(SuperItem item, SuperItemStockMovement.MovementType movementType, BigDecimal quantity,
            String notes, String source, BigDecimal previousStock, BigDecimal resultingStock, boolean allowNegativeStock) {
        if (resultingStock.signum() < 0 && !allowNegativeStock) {
            throw new SuperItemStockConflictException(item, quantity, resultingStock, movementType, allowNegativeStock);
        }
        item.setCurrentStock(resultingStock);
        stockMovementRepository.save(new SuperItemStockMovement(item, movementType, previousStock, resultingStock, quantity,
                trimToNull(notes), source));
        return SuperItemResponse.from(item);
    }

    private int normalizeMovementLimit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return DEFAULT_MOVEMENT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_MOVEMENT_LIMIT);
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
        item.setQuickQuantity(request.quickQuantity());
    }

    private void ensureNoGenericStockMutation(SuperItemRequest request) {
        if (request.currentStock() != null) {
            throw new IllegalArgumentException("No se puede modificar el stock desde el contrato genérico del producto");
        }
    }

    private void validateHabitualObjective(BigDecimal habitualObjective) {
        if (habitualObjective != null && habitualObjective.signum() <= 0) {
            throw new IllegalArgumentException("Objetivo habitual: debe ser mayor a 0");
        }
    }
}
