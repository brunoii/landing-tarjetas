package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import com.gentleia.landingtarjetas.category.Category;
import com.gentleia.landingtarjetas.category.CategoryRepository;
import com.gentleia.landingtarjetas.supermarket.SuperCategory;
import com.gentleia.landingtarjetas.supermarket.SuperCategoryRepository;
import com.gentleia.landingtarjetas.supermarket.SuperItem;
import com.gentleia.landingtarjetas.supermarket.SuperItemRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanDatabase() {
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
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(post("/api/super/items/uncheck-all"))
                .andExpect(status().isNoContent());

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isChecked()).isFalse();
                    assertThat(persisted.getUnit()).isEqualTo("kg");
                    assertThat(persisted.getHabitualObjective()).isEqualByComparingTo("2.000");
                    assertThat(persisted.isConfigured()).isTrue();
                });
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
