package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import jakarta.persistence.LockModeType;

import com.gentleia.landingtarjetas.category.Category;
import com.gentleia.landingtarjetas.category.CategoryRepository;
import com.gentleia.landingtarjetas.supermarket.SuperCategory;
import com.gentleia.landingtarjetas.supermarket.SuperCategoryRepository;
import com.gentleia.landingtarjetas.supermarket.SuperItem;
import com.gentleia.landingtarjetas.supermarket.SuperItemRepository;
import com.gentleia.landingtarjetas.supermarket.SuperItemStockMovement;
import com.gentleia.landingtarjetas.supermarket.SuperItemStockMovementRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SupermarketControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SuperCategoryRepository superCategoryRepository;
    @Autowired
    private SuperItemRepository superItemRepository;
    @Autowired
    private SuperItemStockMovementRepository superItemStockMovementRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanDatabase() {
        superItemStockMovementRepository.deleteAll();
        superItemRepository.deleteAll();
        superCategoryRepository.deleteAll();
    }

    @Test
    void categoryCrudUsesIndependentSuperCategories() throws Exception {
        String financialCategoryName = "Finanzas stage 5 super independence";
        categoryRepository.findByNameIgnoreCase(financialCategoryName)
                .orElseGet(() -> categoryRepository.save(new Category(financialCategoryName, "#38bdf8")));

        mockMvc.perform(post("/api/super/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Verdulería",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Verdulería"))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(superCategoryRepository.findAll()).singleElement()
                .satisfies(category -> assertThat(category.getName()).isEqualTo("Verdulería"));
        assertThat(categoryRepository.findByNameIgnoreCase(financialCategoryName)).isPresent();

        Long id = superCategoryRepository.findAll().get(0).getId();
        mockMvc.perform(put("/api/super/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Almacén",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Almacén"));

        mockMvc.perform(get("/api/super/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Almacén"));

        mockMvc.perform(delete("/api/super/categories/{id}", id))
                .andExpect(status().isNoContent());

        assertThat(superCategoryRepository.findAll()).isEmpty();
    }

    @Test
    void categoryDeleteBlocksWhenActiveItemsUseIt() throws Exception {
        SuperCategory category = superCategoryRepository.save(new SuperCategory("Lácteos"));
        superItemRepository.save(new SuperItem("Leche", category));

        mockMvc.perform(delete("/api/super/categories/{id}", category.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("No se puede eliminar la categoría porque tiene productos asociados"));

        assertThat(superCategoryRepository.existsById(category.getId())).isTrue();
    }

    @Test
    void categoryDeleteBlocksWhenInactiveItemsUseIt() throws Exception {
        SuperCategory category = superCategoryRepository.save(new SuperCategory("Perfumería"));
        SuperItem inactiveItem = new SuperItem("Jabón", category);
        inactiveItem.setActive(false);
        superItemRepository.save(inactiveItem);

        mockMvc.perform(delete("/api/super/categories/{id}", category.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("No se puede eliminar la categoría porque tiene productos asociados"));

        assertThat(superCategoryRepository.existsById(category.getId())).isTrue();
    }

    @Test
    void itemCrudCheckedAndUncheckAllWork() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperCategory verduleria = superCategoryRepository.save(new SuperCategory("Verdulería"));
        superItemRepository.save(checkedItem("Zanahoria", verduleria, true));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayload("Arroz", almacen.getId(), false, "Doble carolina")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Arroz"))
                .andExpect(jsonPath("$.categoryId").value(almacen.getId()))
                .andExpect(jsonPath("$.categoryName").value("Almacén"))
                .andExpect(jsonPath("$.checked").value(false))
                .andExpect(jsonPath("$.notes").value("Doble carolina"));

        Long arrozId = superItemRepository.findAll().stream()
                .filter(item -> item.getName().equals("Arroz"))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(patch("/api/super/items/{id}/checked", arrozId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checked": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(true));

        mockMvc.perform(put("/api/super/items/{id}", arrozId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayload("Banana", verduleria.getId(), true, "Madura", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Banana"))
                .andExpect(jsonPath("$.categoryName").value("Verdulería"))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(superItemRepository.findById(arrozId)).isPresent()
                .get()
                .extracting(SuperItem::isActive)
                .isEqualTo(true);

        mockMvc.perform(get("/api/super/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Banana"))
                .andExpect(jsonPath("$[1].name").value("Zanahoria"));

        mockMvc.perform(post("/api/super/items/uncheck-all"))
                .andExpect(status().isNoContent());

        assertThat(superItemRepository.findAll()).allSatisfy(item -> assertThat(item.isChecked()).isFalse());

        mockMvc.perform(delete("/api/super/items/{id}", arrozId))
                .andExpect(status().isNoContent());

        assertThat(superItemRepository.findById(arrozId)).isPresent()
                .get()
                .extracting(SuperItem::isActive)
                .isEqualTo(false);
    }

    @Test
    void oldItemPayloadCreatesPendingInventoryConfiguration() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayload("Arroz", almacen.getId(), true, "Doble carolina")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Arroz"))
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.unit").isEmpty())
                .andExpect(jsonPath("$.habitualObjective").isEmpty())
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    void extendedItemPayloadTrimsConfigurationAndReturnsConfiguredState() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuredItemPayload("Yerba", almacen.getId(), true, "Suave", "  kg  ", "1.500")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Yerba"))
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.unit").value("kg"))
                .andExpect(jsonPath("$.habitualObjective").value(1.5))
                .andExpect(jsonPath("$.configured").value(true));
    }

    @Test
    void itemWithoutStockRespondsWithUnknownCurrentStock() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuredItemPayload("Yerba", almacen.getId(), true, "Suave", "kg", "1.500")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentStock").isEmpty())
                .andExpect(jsonPath("$.quickQuantity").isEmpty());

        mockMvc.perform(get("/api/super/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentStock").isEmpty())
                .andExpect(jsonPath("$[0].quickQuantity").isEmpty());
    }

    @Test
    void validQuickQuantityIsPersistedAndExposed() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithQuickQuantity("Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "2.250")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quickQuantity").value(2.25));

        assertThat(superItemRepository.findAll()).singleElement()
                .satisfies(item -> assertThat(item.getQuickQuantity()).isEqualByComparingTo("2.250"));
    }

    @Test
    void successfulItemUpdateCanChangeQuickQuantity() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Yerba", almacen);
        item.setCurrentStock(new BigDecimal("4.000"));
        item.setQuickQuantity(new BigDecimal("1.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithQuickQuantity("Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "3.250")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(4.0))
                .andExpect(jsonPath("$.quickQuantity").value(3.25));

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.getCurrentStock()).isEqualByComparingTo("4.000");
                    assertThat(persisted.getQuickQuantity()).isEqualByComparingTo("3.250");
                });
    }

    @Test
    void invalidQuickQuantityIsRejectedAndDoesNotModifyTheItem() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Yerba", almacen);
        item.setQuickQuantity(new BigDecimal("2.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithQuickQuantity("Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La validación de la solicitud falló"))
                .andExpect(jsonPath("$.details[?(@ == 'Cantidad rápida: debe ser mayor a 0')]").exists());

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.getQuickQuantity()).isEqualByComparingTo("2.000"));
    }

    @Test
    void decimalPrecisionBeyondThreeFractionDigitsIsRejected() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Aceite", almacen);
        item.setCurrentStock(new BigDecimal("4.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithQuickQuantity("Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "1.2345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ == 'Cantidad rápida: debe tener hasta 7 enteros y 3 decimales')]").exists());

        mockMvc.perform(post("/api/super/items/{id}/stock-adjustments", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentStock": 1.2345
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ == 'Stock actual: debe tener hasta 7 enteros y 3 decimales')]").exists());

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.getCurrentStock()).isEqualByComparingTo("4.000"));
        assertThat(superItemStockMovementRepository.findAll()).isEmpty();
    }

    @Test
    void genericCreateAndUpdateRejectCurrentStockAndPreservePersistedStock() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Aceite", almacen);
        item.setCurrentStock(new BigDecimal("4.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCurrentStock("Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "3.000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede modificar el stock desde el contrato genérico del producto"));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCurrentStock("Aceite", almacen.getId(), false, "Extra", "litro", "2.000", "9.000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede modificar el stock desde el contrato genérico del producto"));

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.getCurrentStock()).isEqualByComparingTo("4.000");
                    assertThat(persisted.getName()).isEqualTo("Aceite");
                });
    }

    @Test
    void focusedStockAdjustmentSetsAbsoluteStockAndPersistsInternalMovementAtomically() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Aceite", almacen);
        item.setCurrentStock(new BigDecimal("4.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(post("/api/super/items/{id}/stock-adjustments", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentStock": 7.500
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(7.5));

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.getCurrentStock()).isEqualByComparingTo("7.500"));
        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(movement -> {
                    assertThat(movement.getItem().getId()).isEqualTo(savedItem.getId());
                    assertThat(movement.getMovementType()).isEqualTo(SuperItemStockMovement.MovementType.ADJUSTMENT);
                    assertThat(movement.getPreviousStock()).isEqualByComparingTo("4.000");
                    assertThat(movement.getResultingStock()).isEqualByComparingTo("7.500");
                });

        mockMvc.perform(post("/api/super/items/{id}/stock-adjustments", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentStock": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ == 'Stock actual: debe ser mayor o igual a 0')]").exists());

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.getCurrentStock()).isEqualByComparingTo("7.500"));
        assertThat(superItemStockMovementRepository.findAll()).hasSize(1);
    }

    @Test
    void focusedStockAdjustmentAcceptsZeroAsAValidAbsoluteStock() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Aceite", almacen);
        item.setCurrentStock(new BigDecimal("4.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(post("/api/super/items/{id}/stock-adjustments", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentStock": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(0));

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.getCurrentStock()).isEqualByComparingTo("0.000"));
        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(movement -> {
                    assertThat(movement.getPreviousStock()).isEqualByComparingTo("4.000");
                    assertThat(movement.getResultingStock()).isEqualByComparingTo("0.000");
                });
    }

    @Test
    void focusedStockAdjustmentFromUnknownStockRecordsNullPreviousStock() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem savedItem = superItemRepository.save(new SuperItem("Yerba", almacen));

        mockMvc.perform(post("/api/super/items/{id}/stock-adjustments", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentStock": 1.250
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(1.25));

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.getCurrentStock()).isEqualByComparingTo("1.250"));
        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(movement -> {
                    assertThat(movement.getPreviousStock()).isNull();
                    assertThat(movement.getResultingStock()).isEqualByComparingTo("1.250");
                });
    }

    @Test
    void stockMovementCommandsPersistQuantitiesAndExposeNewestHistoryWithOptionalItemFilter() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem yerba = new SuperItem("Yerba", almacen);
        yerba.setUnit("kg");
        yerba.setCurrentStock(new BigDecimal("5.000"));
        yerba.setQuickQuantity(new BigDecimal("0.750"));
        SuperItem savedYerba = superItemRepository.save(yerba);
        SuperItem aceite = new SuperItem("Aceite", almacen);
        aceite.setUnit("litro");
        aceite.setCurrentStock(new BigDecimal("2.000"));
        SuperItem savedAceite = superItemRepository.save(aceite);

        mockMvc.perform(post("/api/super/items/{id}/purchases", savedYerba.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 2.000,
                                  "notes": "Reposición"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(7.0));

        mockMvc.perform(post("/api/super/items/{id}/consumptions", savedYerba.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 1.250,
                                  "notes": "Merienda",
                                  "allowNegativeStock": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(5.75));

        mockMvc.perform(post("/api/super/items/{id}/quick-consumptions", savedYerba.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "allowNegativeStock": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(5.0));

        mockMvc.perform(post("/api/super/items/{id}/purchases", savedAceite.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 1.000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(3.0));

        assertThat(superItemStockMovementRepository.findAll())
                .filteredOn(movement -> movement.getItem().getId().equals(savedYerba.getId()))
                .hasSize(3)
                .allSatisfy(movement -> assertThat(movement.getQuantity()).isNotNull());

        mockMvc.perform(get("/api/super/movements")
                        .param("itemId", savedYerba.getId().toString())
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].movementType").value("QUICK_CONSUMPTION"))
                .andExpect(jsonPath("$[0].itemId").value(savedYerba.getId()))
                .andExpect(jsonPath("$[0].itemName").value("Yerba"))
                .andExpect(jsonPath("$[0].itemUnit").value("kg"))
                .andExpect(jsonPath("$[0].quantity").value(0.75))
                .andExpect(jsonPath("$[0].previousStock").value(5.75))
                .andExpect(jsonPath("$[0].resultingStock").value(5.0))
                .andExpect(jsonPath("$[0].source").value("QUICK"))
                .andExpect(jsonPath("$[1].movementType").value("CONSUMPTION"))
                .andExpect(jsonPath("$[1].itemUnit").value("kg"))
                .andExpect(jsonPath("$[1].quantity").value(1.25))
                .andExpect(jsonPath("$[1].notes").value("Merienda"))
                .andExpect(jsonPath("$[1].source").value("MANUAL"))
                .andExpect(jsonPath("$[2].movementType").value("PURCHASE"))
                .andExpect(jsonPath("$[2].itemUnit").value("kg"))
                .andExpect(jsonPath("$[2].quantity").value(2.0))
                .andExpect(jsonPath("$[2].notes").value("Reposición"))
                .andExpect(jsonPath("$[2].source").value("MANUAL"));
    }

    @Test
    void focusedStockAdjustmentPersistsAdjustmentWithNullQuantity() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Aceite", almacen);
        item.setCurrentStock(new BigDecimal("4.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(post("/api/super/items/{id}/stock-adjustments", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentStock": 7.500
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(7.5));

        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(movement -> {
                    assertThat(movement.getMovementType()).isEqualTo(SuperItemStockMovement.MovementType.ADJUSTMENT);
                    assertThat(movement.getQuantity()).isNull();
                    assertThat(movement.getPreviousStock()).isEqualByComparingTo("4.000");
                    assertThat(movement.getResultingStock()).isEqualByComparingTo("7.500");
                });

        mockMvc.perform(get("/api/super/movements")
                        .param("itemId", savedItem.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].movementType").value("ADJUSTMENT"))
                .andExpect(jsonPath("$[0].quantity").isEmpty())
                .andExpect(jsonPath("$[0].source").value("MANUAL"));
    }

    @Test
    void stockMovementHistoryUsesDefaultAndMaximumLimit() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Yerba", almacen);
        item.setCurrentStock(new BigDecimal("200.000"));
        SuperItem savedItem = superItemRepository.save(item);

        for (int index = 0; index < 105; index++) {
            superItemStockMovementRepository.save(new SuperItemStockMovement(
                    savedItem,
                    SuperItemStockMovement.MovementType.PURCHASE,
                    BigDecimal.valueOf(index),
                    BigDecimal.valueOf(index + 1L),
                    BigDecimal.ONE,
                    null,
                    "MANUAL"
            ));
        }

        mockMvc.perform(get("/api/super/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(50));

        mockMvc.perform(get("/api/super/movements")
                        .param("limit", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(100));
    }

    @Test
    void stockCommandsUsePessimisticWriteLockRepositoryMethod() throws Exception {
        Method method = SuperItemRepository.class.getMethod("findActiveByIdForStockCommand", Long.class);

        assertThat(method.getAnnotation(Lock.class)).isNotNull()
                .extracting(Lock::value)
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void stockMovementValidationRejectsUnknownStockInvalidQuantityAndMissingQuickQuantityWithoutMutation() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem unknownStock = superItemRepository.save(new SuperItem("Yerba", almacen));
        SuperItem knownStock = new SuperItem("Aceite", almacen);
        knownStock.setCurrentStock(new BigDecimal("4.000"));
        SuperItem savedKnownStock = superItemRepository.save(knownStock);

        mockMvc.perform(post("/api/super/items/{id}/purchases", unknownStock.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 1.000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Inicialice el stock con un ajuste antes de registrar movimientos"));

        mockMvc.perform(post("/api/super/items/{id}/consumptions", unknownStock.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 1.000,
                                  "allowNegativeStock": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Inicialice el stock con un ajuste antes de registrar movimientos"));

        mockMvc.perform(post("/api/super/items/{id}/quick-consumptions", unknownStock.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "allowNegativeStock": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Inicialice el stock con un ajuste antes de registrar movimientos"));

        mockMvc.perform(post("/api/super/items/{id}/purchases", savedKnownStock.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ == 'Cantidad: debe ser mayor a 0')]").exists());

        mockMvc.perform(post("/api/super/items/{id}/quick-consumptions", savedKnownStock.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "allowNegativeStock": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Configure una cantidad rápida positiva antes de usar consumo rápido"));

        assertThat(superItemRepository.findById(unknownStock.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.getCurrentStock()).isNull());
        assertThat(superItemRepository.findById(savedKnownStock.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.getCurrentStock()).isEqualByComparingTo("4.000"));
        assertThat(superItemStockMovementRepository.findAll()).isEmpty();
    }

    @Test
    void negativeConsumptionConflictPreservesStockAndMovementHistoryUntilExplicitlyAllowed() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Yerba", almacen);
        item.setCurrentStock(new BigDecimal("1.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(post("/api/super/items/{id}/consumptions", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 2.000,
                                  "allowNegativeStock": false
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El consumo dejaría stock negativo. Confirme para continuar."))
                .andExpect(jsonPath("$.details[?(@ == 'Reintente con allowNegativeStock=true para confirmar.')]").exists())
                .andExpect(jsonPath("$.itemId").value(savedItem.getId()))
                .andExpect(jsonPath("$.itemName").value("Yerba"))
                .andExpect(jsonPath("$.currentStock").value(1.0))
                .andExpect(jsonPath("$.quantity").value(2.0))
                .andExpect(jsonPath("$.resultingStock").value(-1.0))
                .andExpect(jsonPath("$.movementType").value("CONSUMPTION"))
                .andExpect(jsonPath("$.allowNegativeStock").value(false));

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.getCurrentStock()).isEqualByComparingTo("1.000"));
        assertThat(superItemStockMovementRepository.findAll()).isEmpty();

        mockMvc.perform(post("/api/super/items/{id}/consumptions", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 2.000,
                                  "allowNegativeStock": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(-1.0));

        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(movement -> {
                    assertThat(movement.getMovementType()).isEqualTo(SuperItemStockMovement.MovementType.CONSUMPTION);
                    assertThat(movement.getQuantity()).isEqualByComparingTo("2.000");
                    assertThat(movement.getPreviousStock()).isEqualByComparingTo("1.000");
                    assertThat(movement.getResultingStock()).isEqualByComparingTo("-1.000");
                });
    }

    @Test
    void negativeQuickConsumptionConflictPreservesStockAndMovementHistoryUntilExplicitlyAllowed() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Arroz", almacen);
        item.setCurrentStock(new BigDecimal("1.000"));
        item.setQuickQuantity(new BigDecimal("2.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(post("/api/super/items/{id}/quick-consumptions", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "allowNegativeStock": false
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El consumo dejaría stock negativo. Confirme para continuar."))
                .andExpect(jsonPath("$.details[?(@ == 'Reintente con allowNegativeStock=true para confirmar.')]").exists())
                .andExpect(jsonPath("$.itemId").value(savedItem.getId()))
                .andExpect(jsonPath("$.itemName").value("Arroz"))
                .andExpect(jsonPath("$.currentStock").value(1.0))
                .andExpect(jsonPath("$.quantity").value(2.0))
                .andExpect(jsonPath("$.resultingStock").value(-1.0))
                .andExpect(jsonPath("$.movementType").value("QUICK_CONSUMPTION"))
                .andExpect(jsonPath("$.allowNegativeStock").value(false));

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.getCurrentStock()).isEqualByComparingTo("1.000"));
        assertThat(superItemStockMovementRepository.findAll()).isEmpty();

        mockMvc.perform(post("/api/super/items/{id}/quick-consumptions", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "allowNegativeStock": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(-1.0));

        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(movement -> {
                    assertThat(movement.getMovementType()).isEqualTo(SuperItemStockMovement.MovementType.QUICK_CONSUMPTION);
                    assertThat(movement.getQuantity()).isEqualByComparingTo("2.000");
                    assertThat(movement.getPreviousStock()).isEqualByComparingTo("1.000");
                    assertThat(movement.getResultingStock()).isEqualByComparingTo("-1.000");
                    assertThat(movement.getSource()).isEqualTo("QUICK");
                });
    }

    @Test
    void stockMovementValidationUsesSpanishLabelsForQuantityAndAllowNegativeStock() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Yerba", almacen);
        item.setCurrentStock(new BigDecimal("3.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(post("/api/super/items/{id}/consumptions", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": -1,
                                  "allowNegativeStock": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La validación de la solicitud falló"))
                .andExpect(jsonPath("$.details[?(@ == 'Cantidad: debe ser mayor a 0')]").exists());

        mockMvc.perform(post("/api/super/items/{id}/quick-consumptions", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "allowNegativeStock": "not-a-boolean"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ == 'Permitir stock negativo: valor inválido')]").exists());
    }

    @Test
    void checkedEndpointPreservesCurrentStockAndDoesNotCreateMovement() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Aceite", almacen);
        item.setCurrentStock(new BigDecimal("4.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(patch("/api/super/items/{id}/checked", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checked": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.currentStock").value(4.0));

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isChecked()).isTrue();
                    assertThat(persisted.getCurrentStock()).isEqualByComparingTo("4.000");
                });
        assertThat(superItemStockMovementRepository.findAll()).isEmpty();
    }

    @Test
    void itemWithOnlyUnitRemainsPendingInventoryConfiguration() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unitOnlyItemPayload("Yerba", almacen.getId(), false, "Suave", "kg")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Yerba"))
                .andExpect(jsonPath("$.unit").value("kg"))
                .andExpect(jsonPath("$.habitualObjective").isEmpty())
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    void itemWithOnlyHabitualObjectiveRemainsPendingInventoryConfiguration() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectiveOnlyItemPayload("Lavandina", almacen.getId(), false, "Concentrada", "2.000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Lavandina"))
                .andExpect(jsonPath("$.unit").isEmpty())
                .andExpect(jsonPath("$.habitualObjective").value(2.0))
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    void invalidHabitualObjectiveIsRejectedAndDoesNotModifyTheItem() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperCategory verduleria = superCategoryRepository.save(new SuperCategory("Verdulería"));
        SuperItem item = new SuperItem("Aceite", almacen);
        item.setChecked(true);
        item.setNotes("Extra virgen");
        item.setUnit("litro");
        item.setHabitualObjective(new BigDecimal("2.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuredItemPayload("Aceite vencido", verduleria.getId(), false, "Debe cambiar", "botella", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La validación de la solicitud falló"))
                .andExpect(jsonPath("$.details[?(@ == 'Objetivo habitual: debe ser mayor a 0')]").exists());

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.getName()).isEqualTo("Aceite");
                    assertThat(persisted.isChecked()).isTrue();
                    assertThat(persisted.getUnit()).isEqualTo("litro");
                    assertThat(persisted.getNotes()).isEqualTo("Extra virgen");
                    assertThat(persisted.getCategory().getId()).isEqualTo(almacen.getId());
                    assertThat(persisted.getHabitualObjective()).isEqualByComparingTo("2.000");
                    assertThat(persisted.isConfigured()).isTrue();
                });
    }

    @Test
    void configuringAnItemWithoutCheckedPreservesManualPurchaseIntent() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = checkedItem("Fideos", almacen, true);
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithoutChecked("Fideos", almacen.getId(), "Tirabuzón", "paquete", "3")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.unit").value("paquete"))
                .andExpect(jsonPath("$.habitualObjective").value(3))
                .andExpect(jsonPath("$.configured").value(true));
    }

    @Test
    void uncheckAllPreservesInventoryConfiguration() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = checkedItem("Azúcar", almacen, true);
        item.setUnit("kg");
        item.setHabitualObjective(new BigDecimal("2.000"));
        item.setQuickQuantity(new BigDecimal("1.000"));
        item.setCurrentStock(new BigDecimal("3.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(post("/api/super/items/uncheck-all"))
                .andExpect(status().isNoContent());

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isChecked()).isFalse();
                    assertThat(persisted.getUnit()).isEqualTo("kg");
                    assertThat(persisted.getHabitualObjective()).isEqualByComparingTo("2.000");
                    assertThat(persisted.getQuickQuantity()).isEqualByComparingTo("1.000");
                    assertThat(persisted.getCurrentStock()).isEqualByComparingTo("3.000");
                    assertThat(persisted.isConfigured()).isTrue();
                });
        assertThat(superItemStockMovementRepository.findAll()).isEmpty();
    }

    @Test
    void deletingAnItemMarksItInactiveAndHidesItFromTheActiveList() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem savedItem = superItemRepository.save(new SuperItem("Harina", almacen));

        mockMvc.perform(delete("/api/super/items/{id}", savedItem.getId()))
                .andExpect(status().isNoContent());

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .extracting(SuperItem::isActive)
                .isEqualTo(false);

        mockMvc.perform(get("/api/super/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void itemValidationUsesSpanishFieldLabels() throws Exception {
        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "categoryId": null,
                                  "notes": "ok"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La validación de la solicitud falló"))
                .andExpect(jsonPath("$.details[?(@ == 'Nombre: es obligatorio')]").exists())
                .andExpect(jsonPath("$.details[?(@ == 'Categoría: es obligatoria')]").exists());
    }

    @Test
    void unitValidationUsesSpanishFieldLabelAndMaxLength() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuredItemPayload("Arroz", almacen.getId(), false, "", "x".repeat(41), "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La validación de la solicitud falló"))
                .andExpect(jsonPath("$.details[?(@ == 'Unidad: no puede superar 40 caracteres')]").exists());
    }

    private SuperItem checkedItem(String name, SuperCategory category, boolean checked) {
        SuperItem item = new SuperItem(name, category);
        item.setChecked(checked);
        return item;
    }

    private String itemPayload(String name, Long categoryId, boolean checked, String notes) {
        return itemPayload(name, categoryId, checked, notes, true);
    }

    private String itemPayload(String name, Long categoryId, boolean checked, String notes, boolean active) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "active": %s
                }
                """.formatted(name, categoryId, checked, notes, active);
    }

    private String configuredItemPayload(String name, Long categoryId, boolean checked, String notes, String unit, String habitualObjective) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "unit": "%s",
                  "habitualObjective": %s
                }
                """.formatted(name, categoryId, checked, notes, unit, habitualObjective);
    }

    private String itemPayloadWithQuickQuantity(String name, Long categoryId, boolean checked, String notes, String unit,
            String habitualObjective, String quickQuantity) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "unit": "%s",
                  "habitualObjective": %s,
                  "quickQuantity": %s
                }
                """.formatted(name, categoryId, checked, notes, unit, habitualObjective, quickQuantity);
    }

    private String itemPayloadWithCurrentStock(String name, Long categoryId, boolean checked, String notes, String unit,
            String habitualObjective, String currentStock) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "unit": "%s",
                  "habitualObjective": %s,
                  "currentStock": %s
                }
                """.formatted(name, categoryId, checked, notes, unit, habitualObjective, currentStock);
    }

    private String unitOnlyItemPayload(String name, Long categoryId, boolean checked, String notes, String unit) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "unit": "%s"
                }
                """.formatted(name, categoryId, checked, notes, unit);
    }

    private String objectiveOnlyItemPayload(String name, Long categoryId, boolean checked, String notes, String habitualObjective) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "habitualObjective": %s
                }
                """.formatted(name, categoryId, checked, notes, habitualObjective);
    }

    private String itemPayloadWithoutChecked(String name, Long categoryId, String notes, String unit, String habitualObjective) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "notes": "%s",
                  "unit": "%s",
                  "habitualObjective": %s
                }
                """.formatted(name, categoryId, notes, unit, habitualObjective);
    }
}
