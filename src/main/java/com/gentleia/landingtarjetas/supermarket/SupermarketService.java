package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
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
    private static final int DEFAULT_PRICE_OBSERVATION_LIMIT = 50;
    private static final int MAX_PRICE_OBSERVATION_LIMIT = 100;

    private final SuperCategoryRepository categoryRepository;
    private final SuperItemRepository itemRepository;
    private final SuperItemStockMovementRepository stockMovementRepository;
    private final SuperItemPriceObservationRepository priceObservationRepository;
    private final SuperPriceSourceRepository priceSourceRepository;
    private final SuperItemBarcodeAliasRepository barcodeAliasRepository;

    public SupermarketService(SuperCategoryRepository categoryRepository, SuperItemRepository itemRepository,
            SuperItemStockMovementRepository stockMovementRepository,
            SuperItemPriceObservationRepository priceObservationRepository,
            SuperPriceSourceRepository priceSourceRepository,
            SuperItemBarcodeAliasRepository barcodeAliasRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.priceObservationRepository = priceObservationRepository;
        this.priceSourceRepository = priceSourceRepository;
        this.barcodeAliasRepository = barcodeAliasRepository;
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

    @Transactional(readOnly = true)
    public List<SuperSuggestedItemResponse> listSuggestedItems() {
        return itemRepository.findActiveOrderedForList().stream()
                .filter(this::isSuggestedItem)
                .map(SuperSuggestedItemResponse::from)
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
        applyCommercialPresentation(item, request);
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
        applyCommercialPresentation(item, request);
        return SuperItemResponse.from(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        getActiveItem(id).setActive(false);
        barcodeAliasRepository.deactivateActiveAliasesByItemId(id, Instant.now());
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

    @Transactional(readOnly = true)
    public List<SuperPriceSourceResponse> listPriceSources() {
        return priceSourceRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(SuperPriceSourceResponse::from)
                .toList();
    }

    @Transactional
    public SuperPriceSourceResponse createPriceSource(SuperPriceSourceRequest request) {
        ensureUniquePriceSourceName(request.name());
        try {
            SuperPriceSource priceSource = new SuperPriceSource(request.name());
            return SuperPriceSourceResponse.from(priceSourceRepository.saveAndFlush(priceSource));
        } catch (DataIntegrityViolationException exception) {
            throw duplicatePriceSourceConflict();
        }
    }

    @Transactional
    public SuperItemPriceObservationResponse createPriceObservation(Long itemId, SuperItemPriceObservationRequest request) {
        SuperItem item = getActiveItem(itemId);
        String presentationLabel = trimToNull(item.getCommercialPresentationLabel());
        if (presentationLabel == null) {
            throw new IllegalArgumentException("Observación de precio: requiere presentación comercial");
        }
        BigDecimal pricePesos = normalizePriceObservationPrice(request.pricePesos());
        String sourceLabel = normalizePriceObservationSource(request.sourceLabel());
        LocalDate observedDate = normalizePriceObservationObservedDate(request.observedDate());
        return SuperItemPriceObservationResponse.from(priceObservationRepository.save(
                new SuperItemPriceObservation(item, pricePesos, sourceLabel, observedDate)));
    }

    @Transactional(readOnly = true)
    public List<SuperItemPriceObservationResponse> listPriceObservations(Long itemId, int requestedLimit) {
        int limit = normalizePriceObservationLimit(requestedLimit);
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<SuperItemPriceObservation> observations = itemId == null
                ? priceObservationRepository.findRecent(pageRequest)
                : priceObservationRepository.findRecentByItemId(itemId, pageRequest);
        return observations.stream()
                .map(SuperItemPriceObservationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SuperBarcodeLookupResponse lookupBarcodeAlias(String code) {
        String normalizedCode = normalizeBarcodeCode(code);
        return barcodeAliasRepository.findActiveByActiveCode(normalizedCode)
                .map(SuperBarcodeLookupResponse::found)
                .orElseGet(() -> SuperBarcodeLookupResponse.notFound(normalizedCode));
    }

    @Transactional
    public SuperItemBarcodeAliasResponse attachBarcodeAlias(Long itemId, SuperItemBarcodeAliasRequest request) {
        SuperItem item = getActiveItem(itemId);
        String normalizedCode = normalizeBarcodeCode(request.code());
        String normalizedFormat = trimToNull(request.format());
        barcodeAliasRepository.deactivateActiveAliasesForInactiveItemsByActiveCode(normalizedCode, Instant.now());
        if (barcodeAliasRepository.existsByActiveCodeAndActiveTrue(normalizedCode)) {
            throw duplicateBarcodeAliasConflict();
        }
        try {
            SuperItemBarcodeAlias alias = new SuperItemBarcodeAlias(item, normalizedCode, normalizedFormat);
            return SuperItemBarcodeAliasResponse.from(barcodeAliasRepository.saveAndFlush(alias));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateBarcodeAliasConflict();
        }
    }

    @Transactional
    public void deactivateBarcodeAlias(Long itemId, Long aliasId) {
        getActiveItem(itemId);
        SuperItemBarcodeAlias alias = barcodeAliasRepository.findByIdAndItemIdAndActiveTrue(aliasId, itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el alias de barcode para ese producto"));
        alias.deactivate();
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

    private int normalizePriceObservationLimit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return DEFAULT_PRICE_OBSERVATION_LIMIT;
        }
        return Math.min(requestedLimit, MAX_PRICE_OBSERVATION_LIMIT);
    }

    private BigDecimal normalizePriceObservationPrice(BigDecimal pricePesos) {
        if (pricePesos.signum() <= 0) {
            throw new IllegalArgumentException("Precio observado: debe ser mayor a 0");
        }
        return pricePesos.setScale(2);
    }

    private String normalizePriceObservationSource(String sourceLabel) {
        String normalizedSource = trimToNull(sourceLabel);
        if (normalizedSource != null
                && normalizedSource.length() > SupermarketLimits.ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH) {
            throw new IllegalArgumentException("Fuente de la observación: no puede superar "
                    + SupermarketLimits.ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH + " caracteres");
        }
        return normalizedSource;
    }

    private LocalDate normalizePriceObservationObservedDate(LocalDate observedDate) {
        if (observedDate != null && observedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Fecha observada de la observación: no puede ser futura");
        }
        return observedDate;
    }

    private void ensureUniqueCategoryName(String name, Long currentId) {
        categoryRepository.findByNameIgnoreCase(name.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new IllegalArgumentException("Ya existe una categoría del super con ese nombre");
            }
        });
    }

    private void ensureUniquePriceSourceName(String name) {
        priceSourceRepository.findByNormalizedKey(SuperPriceSource.normalizedKeyFor(name))
                .ifPresent(existing -> {
                    throw duplicatePriceSourceConflict();
                });
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isSuggestedItem(SuperItem item) {
        return item.isActive()
                && item.getUnit() != null
                && !item.getUnit().isBlank()
                && item.getHabitualObjective() != null
                && item.getCurrentStock() != null
                && item.getCurrentStock().compareTo(item.getHabitualObjective()) < 0;
    }

    private String normalizeBarcodeCode(String code) {
        String normalizedCode = trimToNull(code);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("Código de barcode: es obligatorio");
        }
        if (normalizedCode.length() > SupermarketLimits.BARCODE_CODE_MAX_LENGTH) {
            throw new IllegalArgumentException("Código de barcode: no puede superar "
                    + SupermarketLimits.BARCODE_CODE_MAX_LENGTH + " caracteres");
        }
        return normalizedCode;
    }

    private ResponseStatusException duplicateBarcodeAliasConflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un alias activo para ese código");
    }

    private ResponseStatusException duplicatePriceSourceConflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una fuente de precio con ese nombre");
    }

    private void applyInventoryConfiguration(SuperItem item, SuperItemRequest request) {
        validateHabitualObjective(request.habitualObjective());
        item.setUnit(trimToNull(request.unit()));
        item.setHabitualObjective(request.habitualObjective());
        item.setQuickQuantity(request.quickQuantity());
    }

    private void applyCommercialPresentation(SuperItem item, SuperItemRequest request) {
        String presentationLabel = trimToNull(request.commercialPresentationLabel());
        BigDecimal presentationQuantity = request.commercialPresentationQuantity();
        BigDecimal presentationPricePesos = request.commercialPresentationPricePesos();
        String presentationPriceSourceLabel = trimToNull(request.commercialPresentationPriceSourceLabel());
        LocalDate presentationPriceObservedDate = request.commercialPresentationPriceObservedDate();
        if (presentationLabel == null) {
            if (presentationPriceSourceLabel != null) {
                throw new IllegalArgumentException("Fuente del precio: requiere precio de presentación");
            }
            if (presentationPriceObservedDate != null) {
                throw new IllegalArgumentException("Fecha observada del precio: requiere precio de presentación");
            }
            if (presentationQuantity != null) {
                throw new IllegalArgumentException("Presentación comercial: la cantidad requiere una presentación");
            }
            if (presentationPricePesos != null) {
                throw new IllegalArgumentException("Precio de presentación: requiere una presentación comercial");
            }
            item.setCommercialPresentationLabel(null);
            item.setCommercialPresentationQuantity(null);
            item.setCommercialPresentationPricePesos(null);
            item.setCommercialPresentationPriceSourceLabel(null);
            item.setCommercialPresentationPriceObservedDate(null);
            return;
        }
        if (presentationQuantity != null) {
            validateCommercialPresentationQuantity(presentationQuantity);
            if (trimToNull(item.getUnit()) == null) {
                throw new IllegalArgumentException("Presentación comercial: la cantidad requiere una unidad de inventario");
            }
        }
        BigDecimal normalizedPresentationPricePesos = normalizeCommercialPresentationPrice(presentationPricePesos);
        String normalizedPresentationPriceSourceLabel = normalizeCommercialPresentationPriceSource(
                presentationPriceSourceLabel, normalizedPresentationPricePesos);
        LocalDate normalizedPresentationPriceObservedDate = normalizeCommercialPresentationPriceObservedDate(
                presentationPriceObservedDate, normalizedPresentationPricePesos);
        item.setCommercialPresentationLabel(presentationLabel);
        item.setCommercialPresentationQuantity(presentationQuantity);
        item.setCommercialPresentationPricePesos(normalizedPresentationPricePesos);
        item.setCommercialPresentationPriceSourceLabel(normalizedPresentationPriceSourceLabel);
        item.setCommercialPresentationPriceObservedDate(normalizedPresentationPriceObservedDate);
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

    private void validateCommercialPresentationQuantity(BigDecimal presentationQuantity) {
        if (presentationQuantity.signum() <= 0) {
            throw new IllegalArgumentException("Cantidad de presentación: debe ser mayor a 0");
        }
    }

    private BigDecimal normalizeCommercialPresentationPrice(BigDecimal presentationPricePesos) {
        if (presentationPricePesos == null) {
            return null;
        }
        if (presentationPricePesos.signum() <= 0) {
            throw new IllegalArgumentException("Precio de presentación: debe ser mayor a 0");
        }
        return presentationPricePesos.setScale(2);
    }

    private String normalizeCommercialPresentationPriceSource(String presentationPriceSourceLabel,
            BigDecimal normalizedPresentationPricePesos) {
        if (presentationPriceSourceLabel == null) {
            return null;
        }
        if (normalizedPresentationPricePesos == null) {
            throw new IllegalArgumentException("Fuente del precio: requiere precio de presentación");
        }
        return presentationPriceSourceLabel;
    }

    private LocalDate normalizeCommercialPresentationPriceObservedDate(LocalDate presentationPriceObservedDate,
            BigDecimal normalizedPresentationPricePesos) {
        if (presentationPriceObservedDate == null) {
            return null;
        }
        if (normalizedPresentationPricePesos == null) {
            throw new IllegalArgumentException("Fecha observada del precio: requiere precio de presentación");
        }
        if (presentationPriceObservedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Fecha observada del precio: no puede ser futura");
        }
        return presentationPriceObservedDate;
    }
}
