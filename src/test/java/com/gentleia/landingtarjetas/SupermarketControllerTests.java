package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

        assertThat(superItemRepository.existsById(arrozId)).isFalse();
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
}
