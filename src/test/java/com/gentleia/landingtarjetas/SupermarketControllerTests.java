package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.LockModeType;

import com.gentleia.landingtarjetas.category.Category;
import com.gentleia.landingtarjetas.category.CategoryRepository;
import com.gentleia.landingtarjetas.supermarket.SuperCategory;
import com.gentleia.landingtarjetas.supermarket.SuperCategoryRepository;
import com.gentleia.landingtarjetas.supermarket.SuperItem;
import com.gentleia.landingtarjetas.supermarket.SuperItemBarcodeAlias;
import com.gentleia.landingtarjetas.supermarket.SuperItemBarcodeAliasRepository;
import com.gentleia.landingtarjetas.supermarket.SuperItemPriceObservation;
import com.gentleia.landingtarjetas.supermarket.SuperItemRepository;
import com.gentleia.landingtarjetas.supermarket.SuperItemPriceObservationRepository;
import com.gentleia.landingtarjetas.supermarket.SuperItemStockMovement;
import com.gentleia.landingtarjetas.supermarket.SuperItemStockMovementRepository;
import com.gentleia.landingtarjetas.supermarket.SuperPriceSource;
import com.gentleia.landingtarjetas.supermarket.SuperPriceSourceRepository;
import com.gentleia.landingtarjetas.supermarket.SupermarketLimits;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
    private SuperItemPriceObservationRepository superItemPriceObservationRepository;
    @Autowired
    private SuperPriceSourceRepository superPriceSourceRepository;
    @Autowired
    private SuperItemStockMovementRepository superItemStockMovementRepository;
    @MockitoSpyBean
    private SuperItemBarcodeAliasRepository superItemBarcodeAliasRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanDatabase() {
        superItemBarcodeAliasRepository.deleteAll();
        superItemPriceObservationRepository.deleteAll();
        superPriceSourceRepository.deleteAll();
        superItemStockMovementRepository.deleteAll();
        superItemRepository.deleteAll();
        superCategoryRepository.deleteAll();
    }

    @AfterEach
    void resetSpies() {
        reset(superItemBarcodeAliasRepository);
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
    void legacyItemPayloadKeepsCommercialPresentationAbsentAcrossCreateAndList() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayload("Arroz", almacen.getId(), false, "Doble carolina")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commercialPresentationLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationQuantity").isEmpty());

        assertThat(superItemRepository.findAll()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getCommercialPresentationLabel()).isNull();
                    assertThat(item.getCommercialPresentationQuantity()).isNull();
                });

        mockMvc.perform(get("/api/super/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commercialPresentationLabel").isEmpty())
                .andExpect(jsonPath("$[0].commercialPresentationQuantity").isEmpty());
    }

    @Test
    void legacyItemPayloadKeepsCommercialPresentationPriceAbsentAcrossCreateAndList() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayload("Arroz", almacen.getId(), false, "Doble carolina")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commercialPresentationLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationQuantity").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPricePesos").isEmpty());

        assertThat(superItemRepository.findAll()).singleElement()
                .satisfies(item -> assertThat(item.getCommercialPresentationPricePesos()).isNull());

        mockMvc.perform(get("/api/super/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commercialPresentationLabel").isEmpty())
                .andExpect(jsonPath("$[0].commercialPresentationQuantity").isEmpty())
                .andExpect(jsonPath("$[0].commercialPresentationPricePesos").isEmpty());
    }

    @Test
    void legacyItemPayloadKeepsCommercialPresentationPriceSourceAbsentAcrossCreateAndList() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayload("Arroz", almacen.getId(), false, "Doble carolina")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commercialPresentationLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPricePesos").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").isEmpty());

        assertThat(superItemRepository.findAll()).singleElement()
                .satisfies(item -> assertThat(item.getCommercialPresentationPriceSourceLabel()).isNull());

        mockMvc.perform(get("/api/super/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commercialPresentationLabel").isEmpty())
                .andExpect(jsonPath("$[0].commercialPresentationPricePesos").isEmpty())
                .andExpect(jsonPath("$[0].commercialPresentationPriceSourceLabel").isEmpty());
    }

    @Test
    void legacyItemPayloadKeepsCommercialPresentationPriceObservedDateAbsentAcrossCreateAndList() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayload("Arroz", almacen.getId(), false, "Doble carolina")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commercialPresentationLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPricePesos").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPriceObservedDate").isEmpty());

        assertThat(superItemRepository.findAll()).singleElement()
                .satisfies(item -> assertThat(item.getCommercialPresentationPriceObservedDate()).isNull());

        mockMvc.perform(get("/api/super/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commercialPresentationLabel").isEmpty())
                .andExpect(jsonPath("$[0].commercialPresentationPricePesos").isEmpty())
                .andExpect(jsonPath("$[0].commercialPresentationPriceSourceLabel").isEmpty())
                .andExpect(jsonPath("$[0].commercialPresentationPriceObservedDate").isEmpty());
    }

    @Test
    void validCommercialPresentationIsTrimmedPersistedExposedUpdatedAndCleared() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "  Pack x 6  ", "6.000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Pack x 6"))
                .andExpect(jsonPath("$.commercialPresentationQuantity").value(6.0));

        SuperItem savedItem = superItemRepository.findAll().get(0);
        assertThat(savedItem.getCommercialPresentationLabel()).isEqualTo("Pack x 6");
        assertThat(savedItem.getCommercialPresentationQuantity()).isEqualByComparingTo("6.000");

        mockMvc.perform(get("/api/super/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commercialPresentationLabel").value("Pack x 6"))
                .andExpect(jsonPath("$[0].commercialPresentationQuantity").value(6.0));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "Botella", "1.250")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Botella"))
                .andExpect(jsonPath("$.commercialPresentationQuantity").value(1.25));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithBlankCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationQuantity").isEmpty());

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.getCommercialPresentationLabel()).isNull();
                    assertThat(persisted.getCommercialPresentationQuantity()).isNull();
                });
    }

    @Test
    void validCommercialPresentationPriceIsNormalizedPersistedExposedUpdatedAndCleared() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPrice(
                                "Yerba", almacen.getId(), false, "Suave", "  Pack x 6  ", "1250.5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Pack x 6"))
                .andExpect(jsonPath("$.commercialPresentationQuantity").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPricePesos").value(1250.5));

        SuperItem savedItem = superItemRepository.findAll().get(0);
        assertThat(savedItem.getCommercialPresentationLabel()).isEqualTo("Pack x 6");
        assertThat(savedItem.getCommercialPresentationQuantity()).isNull();
        assertThat(savedItem.getCommercialPresentationPricePesos()).isEqualByComparingTo("1250.50");
        assertThat(savedItem.getCommercialPresentationPricePesos().scale()).isEqualTo(2);

        mockMvc.perform(get("/api/super/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commercialPresentationLabel").value("Pack x 6"))
                .andExpect(jsonPath("$[0].commercialPresentationPricePesos").value(1250.5));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPrice(
                                "Yerba", almacen.getId(), false, "Suave", "Botella", "999.99")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Botella"))
                .andExpect(jsonPath("$.commercialPresentationPricePesos").value(999.99));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithBlankCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationQuantity").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPricePesos").isEmpty());

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.getCommercialPresentationLabel()).isNull();
                    assertThat(persisted.getCommercialPresentationQuantity()).isNull();
                    assertThat(persisted.getCommercialPresentationPricePesos()).isNull();
                });
    }

    @Test
    void validCommercialPresentationPriceSourceIsTrimmedPersistedExposedUpdatedAndCleared() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPriceSource(
                                "Yerba", almacen.getId(), false, "Suave", "  Pack x 6  ", "1250.5", "  Ticket mayorista  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Pack x 6"))
                .andExpect(jsonPath("$.commercialPresentationPricePesos").value(1250.5))
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").value("Ticket mayorista"));

        SuperItem savedItem = superItemRepository.findAll().get(0);
        assertThat(savedItem.getCommercialPresentationPricePesos()).isEqualByComparingTo("1250.50");
        assertThat(savedItem.getCommercialPresentationPriceSourceLabel()).isEqualTo("Ticket mayorista");

        mockMvc.perform(get("/api/super/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commercialPresentationPricePesos").value(1250.5))
                .andExpect(jsonPath("$[0].commercialPresentationPriceSourceLabel").value("Ticket mayorista"));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPriceSource(
                                "Yerba", almacen.getId(), false, "Suave", "Botella", "999.99", "Lista impresa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Botella"))
                .andExpect(jsonPath("$.commercialPresentationPricePesos").value(999.99))
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").value("Lista impresa"));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPriceAndBlankSource(
                                "Yerba", almacen.getId(), false, "Suave", "Botella", "999.99")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationPricePesos").value(999.99))
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").isEmpty());

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.getCommercialPresentationPricePesos()).isEqualByComparingTo("999.99");
                    assertThat(persisted.getCommercialPresentationPriceSourceLabel()).isNull();
                });
    }

    @Test
    void validCommercialPresentationPriceObservedDateIsPersistedExposedUpdatedAndCleared() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        String observedDate = LocalDate.now().minusDays(1).toString();
        String updatedObservedDate = LocalDate.now().toString();

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPriceSourceAndObservedDate(
                                "Yerba", almacen.getId(), false, "Suave", "  Pack x 6  ", "1250.5",
                                "  Ticket mayorista  ", observedDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Pack x 6"))
                .andExpect(jsonPath("$.commercialPresentationPricePesos").value(1250.5))
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").value("Ticket mayorista"))
                .andExpect(jsonPath("$.commercialPresentationPriceObservedDate").value(observedDate));

        SuperItem savedItem = superItemRepository.findAll().get(0);
        assertThat(savedItem.getCommercialPresentationPricePesos()).isEqualByComparingTo("1250.50");
        assertThat(savedItem.getCommercialPresentationPriceSourceLabel()).isEqualTo("Ticket mayorista");
        assertThat(savedItem.getCommercialPresentationPriceObservedDate()).isEqualTo(LocalDate.parse(observedDate));

        mockMvc.perform(get("/api/super/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commercialPresentationPricePesos").value(1250.5))
                .andExpect(jsonPath("$[0].commercialPresentationPriceSourceLabel").value("Ticket mayorista"))
                .andExpect(jsonPath("$[0].commercialPresentationPriceObservedDate").value(observedDate));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPriceSourceAndObservedDate(
                                "Yerba", almacen.getId(), false, "Suave", "Botella", "999.99", "Lista impresa",
                                updatedObservedDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Botella"))
                .andExpect(jsonPath("$.commercialPresentationPricePesos").value(999.99))
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").value("Lista impresa"))
                .andExpect(jsonPath("$.commercialPresentationPriceObservedDate").value(updatedObservedDate));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "Botella", "1.000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Botella"))
                .andExpect(jsonPath("$.commercialPresentationPricePesos").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPriceObservedDate").isEmpty());

        assertCommercialPresentationPriceSourceAndDate(savedItem.getId(), "Botella", null, null, null);
    }

    @Test
    void commercialPresentationPriceWithoutSourceRemainsValidAndExposesNullSource() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPrice(
                                "Yerba", almacen.getId(), false, "Suave", "Pack x 6", "1250.50")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commercialPresentationPricePesos").value(1250.5))
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPriceObservedDate").isEmpty());

        assertThat(superItemRepository.findAll()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getCommercialPresentationPricePesos()).isEqualByComparingTo("1250.50");
                    assertThat(item.getCommercialPresentationPriceSourceLabel()).isNull();
                    assertThat(item.getCommercialPresentationPriceObservedDate()).isNull();
                });
    }

    @Test
    void invalidCommercialPresentationPriceObservedDateRequestsAreRejectedAndDoNotModifyTheItem() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        String existingObservedDate = LocalDate.now().minusDays(2).toString();
        String futureObservedDate = LocalDate.now().plusDays(1).toString();
        SuperItem item = new SuperItem("Yerba", almacen);
        item.setCommercialPresentationLabel("Pack x 6");
        item.setCommercialPresentationPricePesos(new BigDecimal("1250.50"));
        item.setCommercialPresentationPriceSourceLabel("Ticket mayorista");
        item.setCommercialPresentationPriceObservedDate(LocalDate.parse(existingObservedDate));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPriceSourceAndObservedDate(
                                "Yerba", almacen.getId(), false, "Suave", "Pack x 6", "1250.50", "Lista impresa",
                                futureObservedDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Fecha observada del precio: no puede ser futura"));
        assertCommercialPresentationPriceSourceAndDate(savedItem.getId(), "Pack x 6", "1250.50", "Ticket mayorista",
                existingObservedDate);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialObservedDateOnly(
                                "Yerba", almacen.getId(), false, "Suave", "Pack x 6", LocalDate.now().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Fecha observada del precio: requiere precio de presentación"));
        assertCommercialPresentationPriceSourceAndDate(savedItem.getId(), "Pack x 6", "1250.50", "Ticket mayorista",
                existingObservedDate);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialObservedDateOnly(
                                "Yerba", almacen.getId(), false, "Suave", "   ", LocalDate.now().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Fecha observada del precio: requiere precio de presentación"));
        assertCommercialPresentationPriceSourceAndDate(savedItem.getId(), "Pack x 6", "1250.50", "Ticket mayorista",
                existingObservedDate);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPriceSourceAndObservedDate(
                                "Yerba", almacen.getId(), false, "Suave", "Pack x 6", "1250.50", "Lista impresa",
                                "18-07-2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El cuerpo de la solicitud no es válido"))
                .andExpect(jsonPath("$.details[?(@ == 'Fecha observada del precio: use el formato date-only YYYY-MM-DD')]").exists());
        assertCommercialPresentationPriceSourceAndDate(savedItem.getId(), "Pack x 6", "1250.50", "Ticket mayorista",
                existingObservedDate);
    }

    @Test
    void invalidCommercialPresentationPriceSourceRequestsAreRejectedAndDoNotModifyTheItem() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Yerba", almacen);
        item.setCommercialPresentationLabel("Pack x 6");
        item.setCommercialPresentationPricePesos(new BigDecimal("1250.50"));
        item.setCommercialPresentationPriceSourceLabel("Ticket mayorista");
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialSourceOnly(
                                "Yerba", almacen.getId(), false, "Suave", "Pack x 6", "Lista impresa")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Fuente del precio: requiere precio de presentación"));
        assertCommercialPresentationPriceSource(savedItem.getId(), "Pack x 6", "1250.50", "Ticket mayorista");

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPriceSource(
                                "Yerba", almacen.getId(), false, "Suave", "Pack x 6", "1250.50",
                                "x".repeat(SupermarketLimits.ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La validación de la solicitud falló"))
                .andExpect(jsonPath("$.details[?(@ == 'Fuente del precio: no puede superar 120 caracteres')]").exists());
        assertCommercialPresentationPriceSource(savedItem.getId(), "Pack x 6", "1250.50", "Ticket mayorista");
    }

    @Test
    void clearingCommercialPresentationPriceOrPresentationClearsPriceSource() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Yerba", almacen);
        item.setCommercialPresentationLabel("Pack x 6");
        item.setCommercialPresentationPricePesos(new BigDecimal("1250.50"));
        item.setCommercialPresentationPriceSourceLabel("Ticket mayorista");
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "Pack x 6", "6.000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Pack x 6"))
                .andExpect(jsonPath("$.commercialPresentationPricePesos").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").isEmpty());
        assertCommercialPresentationPriceSource(savedItem.getId(), "Pack x 6", null, null);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPriceSource(
                                "Yerba", almacen.getId(), false, "Suave", "Botella", "999.99", "Lista impresa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").value("Lista impresa"));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithBlankCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPricePesos").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").isEmpty());
        assertCommercialPresentationPriceSource(savedItem.getId(), null, null, null);
    }

    @Test
    void clearingCommercialPresentationPriceOrPresentationClearsPriceSourceAndObservedDate() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        String observedDate = LocalDate.now().toString();
        SuperItem item = new SuperItem("Yerba", almacen);
        item.setCommercialPresentationLabel("Pack x 6");
        item.setCommercialPresentationPricePesos(new BigDecimal("1250.50"));
        item.setCommercialPresentationPriceSourceLabel("Ticket mayorista");
        item.setCommercialPresentationPriceObservedDate(LocalDate.parse(observedDate));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "Pack x 6", "6.000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Pack x 6"))
                .andExpect(jsonPath("$.commercialPresentationPricePesos").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPriceObservedDate").isEmpty());
        assertCommercialPresentationPriceSourceAndDate(savedItem.getId(), "Pack x 6", null, null, null);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPriceSourceAndObservedDate(
                                "Yerba", almacen.getId(), false, "Suave", "Botella", "999.99", "Lista impresa",
                                observedDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").value("Lista impresa"))
                .andExpect(jsonPath("$.commercialPresentationPriceObservedDate").value(observedDate));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithBlankCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPricePesos").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").isEmpty())
                .andExpect(jsonPath("$.commercialPresentationPriceObservedDate").isEmpty());
        assertCommercialPresentationPriceSourceAndDate(savedItem.getId(), null, null, null, null);
    }

    @Test
    void commercialPresentationPriceSourceUpdateDoesNotMutateCheckedStockMovementsSuggestionsOrBarcodeAliases() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = configuredStockItem("Aceite", almacen, "litro", "5.000", "2.000");
        item.setChecked(true);
        item.setCommercialPresentationLabel("Botella");
        item.setCommercialPresentationPricePesos(new BigDecimal("1500.00"));
        item.setCommercialPresentationPriceSourceLabel("Ticket inicial");
        SuperItem savedItem = superItemRepository.saveAndFlush(item);
        SuperItemStockMovement movement = superItemStockMovementRepository.save(new SuperItemStockMovement(
                savedItem,
                SuperItemStockMovement.MovementType.ADJUSTMENT,
                null,
                new BigDecimal("2.000"),
                null,
                "Inicial",
                "MANUAL"
        ));
        SuperItemBarcodeAlias alias = superItemBarcodeAliasRepository.saveAndFlush(
                new SuperItemBarcodeAlias(savedItem, "0075012345678", "EAN_13"));

        mockMvc.perform(get("/api/super/suggested-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aceite"))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(3.0));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithoutCheckedWithCommercialPresentationPriceSource(
                                "Aceite", almacen.getId(), "Clásico", "litro", "5.000", "Bidón", "2.000", "1750", "  Lista impresa  ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.currentStock").value(2.0))
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Bidón"))
                .andExpect(jsonPath("$.commercialPresentationQuantity").value(2.0))
                .andExpect(jsonPath("$.commercialPresentationPricePesos").value(1750.0))
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").value("Lista impresa"));

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isChecked()).isTrue();
                    assertThat(persisted.getCurrentStock()).isEqualByComparingTo("2.000");
                    assertThat(persisted.getCommercialPresentationLabel()).isEqualTo("Bidón");
                    assertThat(persisted.getCommercialPresentationQuantity()).isEqualByComparingTo("2.000");
                    assertThat(persisted.getCommercialPresentationPricePesos()).isEqualByComparingTo("1750.00");
                    assertThat(persisted.getCommercialPresentationPriceSourceLabel()).isEqualTo("Lista impresa");
                });
        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(persisted -> assertThat(persisted.getId()).isEqualTo(movement.getId()));
        assertThat(superItemBarcodeAliasRepository.findById(alias.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isActive()).isTrue();
                    assertThat(persisted.getActiveCode()).isEqualTo("0075012345678");
                });

        mockMvc.perform(get("/api/super/suggested-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aceite"))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(3.0))
                .andExpect(jsonPath("$[0].checked").doesNotExist())
                .andExpect(jsonPath("$[0].commercialPresentationPricePesos").doesNotExist())
                .andExpect(jsonPath("$[0].commercialPresentationPriceSourceLabel").doesNotExist());
    }

    @Test
    void commercialPresentationPriceObservedDateUpdateDoesNotMutateCheckedStockMovementsSuggestionsOrBarcodeAliases() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        String updatedObservedDate = LocalDate.now().toString();
        SuperItem item = configuredStockItem("Aceite", almacen, "litro", "5.000", "2.000");
        item.setChecked(true);
        item.setCommercialPresentationLabel("Botella");
        item.setCommercialPresentationPricePesos(new BigDecimal("1500.00"));
        item.setCommercialPresentationPriceSourceLabel("Ticket inicial");
        item.setCommercialPresentationPriceObservedDate(LocalDate.now().minusDays(1));
        SuperItem savedItem = superItemRepository.saveAndFlush(item);
        SuperItemStockMovement movement = superItemStockMovementRepository.save(new SuperItemStockMovement(
                savedItem,
                SuperItemStockMovement.MovementType.ADJUSTMENT,
                null,
                new BigDecimal("2.000"),
                null,
                "Inicial",
                "MANUAL"
        ));
        SuperItemBarcodeAlias alias = superItemBarcodeAliasRepository.saveAndFlush(
                new SuperItemBarcodeAlias(savedItem, "0075012345678", "EAN_13"));

        mockMvc.perform(get("/api/super/suggested-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aceite"))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(3.0));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithoutCheckedWithCommercialPresentationPriceSourceAndObservedDate(
                                "Aceite", almacen.getId(), "Clásico", "litro", "5.000", "Bidón", "2.000", "1750",
                                "  Lista impresa  ", updatedObservedDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.currentStock").value(2.0))
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Bidón"))
                .andExpect(jsonPath("$.commercialPresentationQuantity").value(2.0))
                .andExpect(jsonPath("$.commercialPresentationPricePesos").value(1750.0))
                .andExpect(jsonPath("$.commercialPresentationPriceSourceLabel").value("Lista impresa"))
                .andExpect(jsonPath("$.commercialPresentationPriceObservedDate").value(updatedObservedDate));

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isChecked()).isTrue();
                    assertThat(persisted.getCurrentStock()).isEqualByComparingTo("2.000");
                    assertThat(persisted.getCommercialPresentationLabel()).isEqualTo("Bidón");
                    assertThat(persisted.getCommercialPresentationQuantity()).isEqualByComparingTo("2.000");
                    assertThat(persisted.getCommercialPresentationPricePesos()).isEqualByComparingTo("1750.00");
                    assertThat(persisted.getCommercialPresentationPriceSourceLabel()).isEqualTo("Lista impresa");
                    assertThat(persisted.getCommercialPresentationPriceObservedDate()).isEqualTo(LocalDate.parse(updatedObservedDate));
                });
        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(persisted -> assertThat(persisted.getId()).isEqualTo(movement.getId()));
        assertThat(superItemBarcodeAliasRepository.findById(alias.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isActive()).isTrue();
                    assertThat(persisted.getActiveCode()).isEqualTo("0075012345678");
                });

        mockMvc.perform(get("/api/super/suggested-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aceite"))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(3.0))
                .andExpect(jsonPath("$[0].checked").doesNotExist())
                .andExpect(jsonPath("$[0].commercialPresentationPricePesos").doesNotExist())
                .andExpect(jsonPath("$[0].commercialPresentationPriceSourceLabel").doesNotExist())
                .andExpect(jsonPath("$[0].commercialPresentationPriceObservedDate").doesNotExist());
    }

    @Test
    void invalidCommercialPresentationRequestsAreRejectedAndDoNotModifyTheItem() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Yerba", almacen);
        item.setUnit("kg");
        item.setHabitualObjective(new BigDecimal("1.500"));
        item.setCommercialPresentationLabel("Pack x 6");
        item.setCommercialPresentationQuantity(new BigDecimal("6.000"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "   ", "3.000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Presentación comercial: la cantidad requiere una presentación"));
        assertCommercialPresentation(savedItem.getId(), "Pack x 6", "6.000");

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "Caja", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ == 'Cantidad de presentación: debe ser mayor a 0')]").exists());
        assertCommercialPresentation(savedItem.getId(), "Pack x 6", "6.000");

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "  ", "1.500", "Caja", "3.000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Presentación comercial: la cantidad requiere una unidad de inventario"));
        assertCommercialPresentation(savedItem.getId(), "Pack x 6", "6.000");

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialQuantityOnly(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500", "3.000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Presentación comercial: la cantidad requiere una presentación"));
        assertCommercialPresentation(savedItem.getId(), "Pack x 6", "6.000");
    }

    @Test
    void invalidCommercialPresentationPriceRequestsAreRejectedAndDoNotModifyTheItem() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Yerba", almacen);
        item.setCommercialPresentationLabel("Pack x 6");
        item.setCommercialPresentationPricePesos(new BigDecimal("1250.50"));
        SuperItem savedItem = superItemRepository.save(item);

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPrice(
                                "Yerba", almacen.getId(), false, "Suave", "Caja", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La validación de la solicitud falló"))
                .andExpect(jsonPath("$.details[?(@ == 'Precio de presentación: debe ser mayor a 0')]").exists());
        assertCommercialPresentationPrice(savedItem.getId(), "Pack x 6", "1250.50");

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPrice(
                                "Yerba", almacen.getId(), false, "Suave", "Caja", "-1.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La validación de la solicitud falló"))
                .andExpect(jsonPath("$.details[?(@ == 'Precio de presentación: debe ser mayor a 0')]").exists());
        assertCommercialPresentationPrice(savedItem.getId(), "Pack x 6", "1250.50");

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPrice(
                                "Yerba", almacen.getId(), false, "Suave", "Caja", "1.234")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La validación de la solicitud falló"))
                .andExpect(jsonPath("$.details[?(@ == 'Precio de presentación: debe tener hasta 10 enteros y 2 decimales')]").exists());
        assertCommercialPresentationPrice(savedItem.getId(), "Pack x 6", "1250.50");

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPriceOnly(
                                "Yerba", almacen.getId(), false, "Suave", "999.99")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Precio de presentación: requiere una presentación comercial"));
        assertCommercialPresentationPrice(savedItem.getId(), "Pack x 6", "1250.50");
    }

    @Test
    void commercialPresentationUpdateDoesNotMutateCheckedStockMovementsSuggestionsOrBarcodeAliases() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = configuredStockItem("Aceite", almacen, "litro", "5.000", "2.000");
        item.setChecked(true);
        SuperItem savedItem = superItemRepository.saveAndFlush(item);
        SuperItemStockMovement movement = superItemStockMovementRepository.save(new SuperItemStockMovement(
                savedItem,
                SuperItemStockMovement.MovementType.ADJUSTMENT,
                null,
                new BigDecimal("2.000"),
                null,
                "Inicial",
                "MANUAL"
        ));
        SuperItemBarcodeAlias alias = superItemBarcodeAliasRepository.saveAndFlush(
                new SuperItemBarcodeAlias(savedItem, "0075012345678", "EAN_13"));

        mockMvc.perform(get("/api/super/suggested-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aceite"))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(3.0));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithoutCheckedWithCommercialPresentation(
                                "Aceite", almacen.getId(), "Clásico", "litro", "5.000", "Bidón", "2.000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.currentStock").value(2.0))
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Bidón"))
                .andExpect(jsonPath("$.commercialPresentationQuantity").value(2.0));

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isChecked()).isTrue();
                    assertThat(persisted.getCurrentStock()).isEqualByComparingTo("2.000");
                    assertThat(persisted.getCommercialPresentationLabel()).isEqualTo("Bidón");
                    assertThat(persisted.getCommercialPresentationQuantity()).isEqualByComparingTo("2.000");
                });
        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(persisted -> assertThat(persisted.getId()).isEqualTo(movement.getId()));
        assertThat(superItemBarcodeAliasRepository.findById(alias.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isActive()).isTrue();
                    assertThat(persisted.getActiveCode()).isEqualTo("0075012345678");
                });

        mockMvc.perform(get("/api/super/suggested-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aceite"))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(3.0))
                .andExpect(jsonPath("$[0].checked").doesNotExist());
    }

    @Test
    void commercialPresentationPriceUpdateDoesNotMutateCheckedStockMovementsSuggestionsOrBarcodeAliases() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = configuredStockItem("Aceite", almacen, "litro", "5.000", "2.000");
        item.setChecked(true);
        item.setCommercialPresentationLabel("Botella");
        item.setCommercialPresentationPricePesos(new BigDecimal("1500.00"));
        SuperItem savedItem = superItemRepository.saveAndFlush(item);
        SuperItemStockMovement movement = superItemStockMovementRepository.save(new SuperItemStockMovement(
                savedItem,
                SuperItemStockMovement.MovementType.ADJUSTMENT,
                null,
                new BigDecimal("2.000"),
                null,
                "Inicial",
                "MANUAL"
        ));
        SuperItemBarcodeAlias alias = superItemBarcodeAliasRepository.saveAndFlush(
                new SuperItemBarcodeAlias(savedItem, "0075012345678", "EAN_13"));

        mockMvc.perform(get("/api/super/suggested-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aceite"))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(3.0));

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithoutCheckedWithCommercialPresentationPrice(
                                "Aceite", almacen.getId(), "Clásico", "litro", "5.000", "Bidón", "2.000", "1750")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.currentStock").value(2.0))
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Bidón"))
                .andExpect(jsonPath("$.commercialPresentationQuantity").value(2.0))
                .andExpect(jsonPath("$.commercialPresentationPricePesos").value(1750.0));

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isChecked()).isTrue();
                    assertThat(persisted.getCurrentStock()).isEqualByComparingTo("2.000");
                    assertThat(persisted.getCommercialPresentationLabel()).isEqualTo("Bidón");
                    assertThat(persisted.getCommercialPresentationQuantity()).isEqualByComparingTo("2.000");
                    assertThat(persisted.getCommercialPresentationPricePesos()).isEqualByComparingTo("1750.00");
                    assertThat(persisted.getCommercialPresentationPricePesos().scale()).isEqualTo(2);
                });
        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(persisted -> assertThat(persisted.getId()).isEqualTo(movement.getId()));
        assertThat(superItemBarcodeAliasRepository.findById(alias.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isActive()).isTrue();
                    assertThat(persisted.getActiveCode()).isEqualTo("0075012345678");
                });

        mockMvc.perform(get("/api/super/suggested-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aceite"))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(3.0))
                .andExpect(jsonPath("$[0].checked").doesNotExist())
                .andExpect(jsonPath("$[0].commercialPresentationPricePesos").doesNotExist());
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
    void manualPriceObservationPersistsSnapshotAndDoesNotMutateInventoryState() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = configuredStockItem("Aceite", almacen, "litro", "5.000", "2.000");
        item.setChecked(true);
        item.setCommercialPresentationLabel("Botella");
        item.setCommercialPresentationQuantity(new BigDecimal("1.500"));
        item.setCommercialPresentationPricePesos(new BigDecimal("1500.00"));
        item.setCommercialPresentationPriceSourceLabel("Ticket inicial");
        item.setCommercialPresentationPriceObservedDate(LocalDate.now().minusDays(1));
        SuperItem savedItem = superItemRepository.saveAndFlush(item);
        SuperItemStockMovement movement = superItemStockMovementRepository.save(new SuperItemStockMovement(
                savedItem,
                SuperItemStockMovement.MovementType.ADJUSTMENT,
                null,
                new BigDecimal("2.000"),
                null,
                "Inicial",
                "MANUAL"
        ));
        SuperItemBarcodeAlias alias = superItemBarcodeAliasRepository.saveAndFlush(
                new SuperItemBarcodeAlias(savedItem, "0075012345678", "EAN_13"));
        String observedDate = LocalDate.now().toString();

        mockMvc.perform(post("/api/super/items/{id}/price-observations", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceObservationPayload("1699.9", "  Ticket proveedor  ", observedDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemId").value(savedItem.getId()))
                .andExpect(jsonPath("$.itemName").value("Aceite"))
                .andExpect(jsonPath("$.pricePesos").value(1699.9))
                .andExpect(jsonPath("$.sourceLabel").value("Ticket proveedor"))
                .andExpect(jsonPath("$.observedDate").value(observedDate))
                .andExpect(jsonPath("$.presentationLabelSnapshot").value("Botella"))
                .andExpect(jsonPath("$.presentationQuantitySnapshot").value(1.5))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(superItemPriceObservationRepository.findAll()).singleElement()
                .satisfies(observation -> {
                    assertThat(observation.getItem().getId()).isEqualTo(savedItem.getId());
                    assertThat(observation.getPricePesos()).isEqualByComparingTo("1699.90");
                    assertThat(observation.getPricePesos().scale()).isEqualTo(2);
                    assertThat(observation.getSourceLabel()).isEqualTo("Ticket proveedor");
                    assertThat(observation.getObservedDate()).isEqualTo(LocalDate.parse(observedDate));
                    assertThat(observation.getPresentationLabelSnapshot()).isEqualTo("Botella");
                    assertThat(observation.getPresentationQuantitySnapshot()).isEqualByComparingTo("1.500");
                    assertThat(observation.getCreatedAt()).isNotNull();
                });
        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isChecked()).isTrue();
                    assertThat(persisted.getCurrentStock()).isEqualByComparingTo("2.000");
                    assertThat(persisted.getCommercialPresentationPricePesos()).isEqualByComparingTo("1500.00");
                    assertThat(persisted.getCommercialPresentationPriceSourceLabel()).isEqualTo("Ticket inicial");
                });
        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(persisted -> assertThat(persisted.getId()).isEqualTo(movement.getId()));
        assertThat(superItemBarcodeAliasRepository.findById(alias.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.isActive()).isTrue());

        mockMvc.perform(get("/api/super/suggested-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aceite"))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(3.0));
    }

    @Test
    void priceObservationsListRecentGlobalAndByItemWithSafeLimits() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem yerba = itemWithCommercialPresentation("Yerba", almacen, "Pack x 6", "6.000");
        SuperItem savedYerba = superItemRepository.saveAndFlush(yerba);
        SuperItem aceite = itemWithCommercialPresentation("Aceite", almacen, "Botella", null);
        SuperItem savedAceite = superItemRepository.saveAndFlush(aceite);

        for (int index = 0; index < 105; index++) {
            mockMvc.perform(post("/api/super/items/{id}/price-observations", savedYerba.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(priceObservationPayload(String.valueOf(1000 + index), null, null)))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/super/items/{id}/price-observations", savedAceite.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceObservationPayload("2500", "Lista", null)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/super/price-observations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(50))
                .andExpect(jsonPath("$[0].itemId").value(savedAceite.getId()))
                .andExpect(jsonPath("$[0].itemName").value("Aceite"))
                .andExpect(jsonPath("$[0].pricePesos").value(2500.0))
                .andExpect(jsonPath("$[0].presentationLabelSnapshot").value("Botella"));

        mockMvc.perform(get("/api/super/price-observations")
                        .param("itemId", savedYerba.getId().toString())
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].itemId").value(savedYerba.getId()))
                .andExpect(jsonPath("$[0].pricePesos").value(1104.0))
                .andExpect(jsonPath("$[1].pricePesos").value(1103.0));

        mockMvc.perform(get("/api/super/price-observations")
                        .param("limit", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(100));

        mockMvc.perform(get("/api/super/price-observations")
                        .param("limit", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(50));
    }

    @Test
    void invalidPriceObservationRequestsAreRejectedWithoutMutatingItemOrObservations() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem activeWithoutPresentation = superItemRepository.saveAndFlush(new SuperItem("Arroz", almacen));
        SuperItem inactive = itemWithCommercialPresentation("Inactivo", almacen, "Caja", null);
        inactive.setActive(false);
        SuperItem savedInactive = superItemRepository.saveAndFlush(inactive);
        SuperItem valid = itemWithCommercialPresentation("Yerba", almacen, "Pack x 6", "6.000");
        valid.setChecked(true);
        SuperItem savedValid = superItemRepository.saveAndFlush(valid);

        mockMvc.perform(post("/api/super/items/{id}/price-observations", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceObservationPayload("1250.50", null, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No se encontró el producto del super"));
        mockMvc.perform(post("/api/super/items/{id}/price-observations", savedInactive.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceObservationPayload("1250.50", null, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No se encontró el producto del super"));
        mockMvc.perform(post("/api/super/items/{id}/price-observations", activeWithoutPresentation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceObservationPayload("1250.50", null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Observación de precio: requiere presentación comercial"));
        mockMvc.perform(post("/api/super/items/{id}/price-observations", savedValid.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceObservationPayload("0", null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ == 'Precio observado: debe ser mayor a 0')]").exists());
        mockMvc.perform(post("/api/super/items/{id}/price-observations", savedValid.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceObservationPayload("1250.50", "Lista", LocalDate.now().plusDays(1).toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Fecha observada de la observación: no puede ser futura"));

        assertThat(superItemPriceObservationRepository.findAll()).isEmpty();
        assertThat(superItemRepository.findById(savedValid.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isChecked()).isTrue();
                    assertThat(persisted.getCommercialPresentationLabel()).isEqualTo("Pack x 6");
                    assertThat(persisted.getCommercialPresentationQuantity()).isEqualByComparingTo("6.000");
                });
    }

    @Test
    void sourceLabelIsTrimmedAndRejectedWhenLongWithoutCollateralMutation() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = itemWithCommercialPresentation("Yerba", almacen, "Pack x 6", "6.000");
        item.setCurrentStock(new BigDecimal("4.000"));
        SuperItem savedItem = superItemRepository.saveAndFlush(item);
        SuperItemStockMovement movement = superItemStockMovementRepository.save(new SuperItemStockMovement(
                savedItem,
                SuperItemStockMovement.MovementType.ADJUSTMENT,
                null,
                new BigDecimal("4.000"),
                null,
                "Inicial",
                "MANUAL"
        ));

        mockMvc.perform(post("/api/super/items/{id}/price-observations", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceObservationPayload("1250.50", "  Ticket mayorista  ", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceLabel").value("Ticket mayorista"));

        assertThat(superItemPriceObservationRepository.findAll()).singleElement()
                .satisfies(observation -> assertThat(observation.getSourceLabel()).isEqualTo("Ticket mayorista"));

        mockMvc.perform(post("/api/super/items/{id}/price-observations", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceObservationPayload("1300.00", "x".repeat(SupermarketLimits.ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH + 1), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Fuente de la observación: no puede superar 120 caracteres"));

        assertThat(superItemPriceObservationRepository.findAll()).hasSize(1);
        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.getCurrentStock()).isEqualByComparingTo("4.000"));
        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(persisted -> assertThat(persisted.getId()).isEqualTo(movement.getId()));
    }

    @Test
    void priceSourcesCreateListTrimAndRejectDuplicateNormalizedNamesWithoutCommercialFields() throws Exception {
        mockMvc.perform(post("/api/super/price-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceSourcePayload("  Ticket proveedor  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ticket proveedor"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.location").doesNotExist())
                .andExpect(jsonPath("$.store").doesNotExist())
                .andExpect(jsonPath("$.commerce").doesNotExist())
                .andExpect(jsonPath("$.comparison").doesNotExist())
                .andExpect(jsonPath("$.priceMetric").doesNotExist());

        mockMvc.perform(post("/api/super/price-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceSourcePayload("ticket PROVEEDOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe una fuente de precio con ese nombre"));

        assertThat(superPriceSourceRepository.findAll()).singleElement()
                .satisfies(source -> {
                    assertThat(source.getName()).isEqualTo("Ticket proveedor");
                    assertThat(source.getNormalizedKey()).isEqualTo("ticket proveedor");
                    assertThat(source.isActive()).isTrue();
                });

        mockMvc.perform(get("/api/super/price-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Ticket proveedor"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].address").doesNotExist())
                .andExpect(jsonPath("$[0].location").doesNotExist())
                .andExpect(jsonPath("$[0].store").doesNotExist())
                .andExpect(jsonPath("$[0].commerce").doesNotExist())
                .andExpect(jsonPath("$[0].comparison").doesNotExist())
                .andExpect(jsonPath("$[0].priceMetric").doesNotExist());
    }

    @Test
    void priceSourcesListExcludesInactiveSources() throws Exception {
        superPriceSourceRepository.saveAndFlush(new SuperPriceSource("Activa"));
        SuperPriceSource inactiveSource = superPriceSourceRepository.saveAndFlush(new SuperPriceSource("Inactiva"));
        inactiveSource.setActive(false);
        superPriceSourceRepository.saveAndFlush(inactiveSource);

        mockMvc.perform(get("/api/super/price-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Activa"));
    }

    @Test
    void priceSourceBlankAndLongNamesAreRejectedWithoutPersistingSources() throws Exception {
        mockMvc.perform(post("/api/super/price-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceSourcePayload("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ == 'Nombre: es obligatorio')]").exists());

        mockMvc.perform(post("/api/super/price-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceSourcePayload("x".repeat(SupermarketLimits.PRICE_SOURCE_NAME_MAX_LENGTH + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ == 'Nombre: no puede superar 120 caracteres')]").exists());

        assertThat(superPriceSourceRepository.findAll()).isEmpty();
    }

    @Test
    void itemCreateUpdateAndPresentationClearingDoNotCreateOrMutatePriceObservations() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));

        mockMvc.perform(post("/api/super/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPriceSourceAndObservedDate(
                                "Yerba", almacen.getId(), false, "Suave", "Pack x 6", "1250.50", "Ticket",
                                LocalDate.now().toString())))
                .andExpect(status().isCreated());
        SuperItem savedItem = superItemRepository.findAll().get(0);
        assertThat(superItemPriceObservationRepository.findAll()).isEmpty();

        mockMvc.perform(post("/api/super/items/{id}/price-observations", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(priceObservationPayload("1250.50", "Ticket", LocalDate.now().toString())))
                .andExpect(status().isCreated());
        Long observationId = superItemPriceObservationRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithCommercialPresentationPriceSourceAndObservedDate(
                                "Yerba", almacen.getId(), false, "Suave", "Botella", "999.99", "Lista",
                                LocalDate.now().minusDays(1).toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").value("Botella"));
        mockMvc.perform(put("/api/super/items/{id}", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayloadWithBlankCommercialPresentation(
                                "Yerba", almacen.getId(), false, "Suave", "kg", "1.500")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPresentationLabel").isEmpty());

        assertThat(superItemPriceObservationRepository.findAll()).singleElement()
                .satisfies(observation -> {
                    assertThat(observation.getId()).isEqualTo(observationId);
                    assertThat(observation.getPresentationLabelSnapshot()).isEqualTo("Pack x 6");
                    assertThat(observation.getPricePesos()).isEqualByComparingTo("1250.50");
                    assertThat(observation.getSourceLabel()).isEqualTo("Ticket");
                });
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
    void suggestedListReturnsEligibleItemsWithSuggestedQuantityInListOrderingAndNoCheckedField() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperCategory verduleria = superCategoryRepository.save(new SuperCategory("Verdulería"));
        superItemRepository.save(configuredStockItem("Banana", verduleria, "kg", "5.000", "2.000"));
        superItemRepository.save(configuredStockItem("Arroz", almacen, "paquete", "4.000", "1.500"));

        mockMvc.perform(get("/api/super/suggested-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Arroz"))
                .andExpect(jsonPath("$[0].categoryName").value("Almacén"))
                .andExpect(jsonPath("$[0].unit").value("paquete"))
                .andExpect(jsonPath("$[0].habitualObjective").value(4.0))
                .andExpect(jsonPath("$[0].currentStock").value(1.5))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(2.5))
                .andExpect(jsonPath("$[0].checked").doesNotExist())
                .andExpect(jsonPath("$[1].name").value("Banana"))
                .andExpect(jsonPath("$[1].categoryName").value("Verdulería"))
                .andExpect(jsonPath("$[1].suggestedQuantity").value(3.0))
                .andExpect(jsonPath("$[1].checked").doesNotExist());
    }

    @Test
    void suggestedListExcludesInactiveUnconfiguredUnknownStockTargetMetAndOverTargetItems() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        superItemRepository.save(configuredStockItem("Arroz", almacen, "paquete", "4.000", "1.000"));
        SuperItem inactive = configuredStockItem("Yerba", almacen, "kg", "2.000", "1.000");
        inactive.setActive(false);
        superItemRepository.save(inactive);
        superItemRepository.save(configuredStockItem("Aceite", almacen, " ", "3.000", "1.000"));
        SuperItem withoutUnit = new SuperItem("Fideos", almacen);
        withoutUnit.setHabitualObjective(new BigDecimal("3.000"));
        withoutUnit.setCurrentStock(new BigDecimal("1.000"));
        superItemRepository.save(withoutUnit);
        SuperItem withoutObjective = new SuperItem("Harina", almacen);
        withoutObjective.setUnit("kg");
        withoutObjective.setCurrentStock(new BigDecimal("1.000"));
        superItemRepository.save(withoutObjective);
        SuperItem unknownStock = new SuperItem("Sal", almacen);
        unknownStock.setUnit("paquete");
        unknownStock.setHabitualObjective(new BigDecimal("2.000"));
        superItemRepository.save(unknownStock);
        superItemRepository.save(configuredStockItem("Azúcar", almacen, "kg", "2.000", "2.000"));
        superItemRepository.save(configuredStockItem("Café", almacen, "paquete", "2.000", "3.000"));

        mockMvc.perform(get("/api/super/suggested-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Arroz"))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(3.0));
    }

    @Test
    void suggestedListIsReadOnlyAndPreservesManualStockMovementBarcodeAliasAndUpdatedAt() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = configuredStockItem("Aceite", almacen, "litro", "5.000", "2.000");
        item.setChecked(true);
        SuperItem savedItem = superItemRepository.saveAndFlush(item);
        SuperItemStockMovement movement = superItemStockMovementRepository.save(new SuperItemStockMovement(
                savedItem,
                SuperItemStockMovement.MovementType.ADJUSTMENT,
                null,
                new BigDecimal("2.000"),
                null,
                "Inicial",
                "MANUAL"
        ));
        SuperItemBarcodeAlias alias = superItemBarcodeAliasRepository.saveAndFlush(
                new SuperItemBarcodeAlias(savedItem, "0075012345678", "EAN_13"));
        var itemUpdatedAtBeforeRequest = superItemRepository.findById(savedItem.getId()).orElseThrow().getUpdatedAt();
        var aliasUpdatedAtBeforeRequest = superItemBarcodeAliasRepository.findById(alias.getId()).orElseThrow().getUpdatedAt();

        mockMvc.perform(get("/api/super/suggested-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Aceite"))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(3.0))
                .andExpect(jsonPath("$[0].checked").doesNotExist());

        assertThat(superItemRepository.findById(savedItem.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isChecked()).isTrue();
                    assertThat(persisted.getCurrentStock()).isEqualByComparingTo("2.000");
                    assertThat(persisted.getUpdatedAt()).isEqualTo(itemUpdatedAtBeforeRequest);
                });
        assertThat(superItemStockMovementRepository.findAll()).singleElement()
                .satisfies(persisted -> assertThat(persisted.getId()).isEqualTo(movement.getId()));
        assertThat(superItemBarcodeAliasRepository.findById(alias.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isActive()).isTrue();
                    assertThat(persisted.getActiveCode()).isEqualTo("0075012345678");
                    assertThat(persisted.getUpdatedAt()).isEqualTo(aliasUpdatedAtBeforeRequest);
                });
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
    void barcodeLookupAttachSoftRemoveAndInactiveItemsUseTextCodes() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = superItemRepository.save(new SuperItem("Yerba", almacen));
        String codeWithLeadingZeros = "0075012345678";

        mockMvc.perform(get("/api/super/barcode-aliases")
                        .param("code", codeWithLeadingZeros))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.code").value(codeWithLeadingZeros));

        mockMvc.perform(post("/api/super/items/{itemId}/barcode-aliases", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(barcodeAliasPayload(codeWithLeadingZeros, "EAN_13")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(codeWithLeadingZeros))
                .andExpect(jsonPath("$.format").value("EAN_13"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.itemId").value(item.getId()));

        Long aliasId = superItemBarcodeAliasRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/super/barcode-aliases")
                        .param("code", codeWithLeadingZeros))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.code").value(codeWithLeadingZeros))
                .andExpect(jsonPath("$.aliasId").value(aliasId))
                .andExpect(jsonPath("$.format").value("EAN_13"))
                .andExpect(jsonPath("$.item.id").value(item.getId()))
                .andExpect(jsonPath("$.item.name").value("Yerba"));

        mockMvc.perform(delete("/api/super/items/{itemId}/barcode-aliases/{aliasId}", item.getId(), aliasId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/super/barcode-aliases")
                        .param("code", codeWithLeadingZeros))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.code").value(codeWithLeadingZeros));

        mockMvc.perform(post("/api/super/items/{itemId}/barcode-aliases", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(barcodeAliasPayload(codeWithLeadingZeros, "EAN_13")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(codeWithLeadingZeros))
                .andExpect(jsonPath("$.active").value(true));

        SuperItem inactiveItem = new SuperItem("Harina", almacen);
        inactiveItem.setActive(false);
        SuperItem savedInactiveItem = superItemRepository.save(inactiveItem);

        mockMvc.perform(post("/api/super/items/{itemId}/barcode-aliases", savedInactiveItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(barcodeAliasPayload("0999888777666", "EAN_13")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No se encontró el producto del super"));
    }

    @Test
    void barcodeLookupIgnoresAliasesAttachedToSoftDeletedItems() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = superItemRepository.save(new SuperItem("Yerba", almacen));
        String code = "0075012345678";

        mockMvc.perform(post("/api/super/items/{itemId}/barcode-aliases", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(barcodeAliasPayload(code, "EAN_13")))
                .andExpect(status().isCreated());
        Long aliasId = superItemBarcodeAliasRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/api/super/items/{itemId}", item.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/super/barcode-aliases")
                        .param("code", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.code").value(code));
        assertThat(superItemBarcodeAliasRepository.findById(aliasId)).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isActive()).isFalse();
                    assertThat(persisted.getActiveCode()).isNull();
                });
    }

    @Test
    void barcodeAliasReservedByInactiveItemCanBeReusedByActiveItem() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem inactiveItem = new SuperItem("Yerba", almacen);
        inactiveItem.setActive(false);
        SuperItem savedInactiveItem = superItemRepository.save(inactiveItem);
        SuperItem activeItem = superItemRepository.save(new SuperItem("Aceite", almacen));
        String code = "0075012345678";
        SuperItemBarcodeAlias orphanAlias = superItemBarcodeAliasRepository.saveAndFlush(
                new SuperItemBarcodeAlias(savedInactiveItem, code, "EAN_13"));

        mockMvc.perform(get("/api/super/barcode-aliases")
                        .param("code", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false));

        mockMvc.perform(post("/api/super/items/{itemId}/barcode-aliases", activeItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(barcodeAliasPayload(code, "EAN_13")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.itemId").value(activeItem.getId()));

        mockMvc.perform(get("/api/super/barcode-aliases")
                        .param("code", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.item.id").value(activeItem.getId()))
                .andExpect(jsonPath("$.item.name").value("Aceite"));

        assertThat(superItemBarcodeAliasRepository.findById(orphanAlias.getId())).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isActive()).isFalse();
                    assertThat(persisted.getActiveCode()).isNull();
                    assertThat(persisted.getDeactivatedAt()).isNotNull();
                });
    }

    @Test
    void barcodeOperationsPreserveInventoryStateAndMovementRows() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = new SuperItem("Aceite", almacen);
        item.setChecked(true);
        item.setCurrentStock(new BigDecimal("4.000"));
        SuperItem savedItem = superItemRepository.save(item);
        superItemStockMovementRepository.save(new SuperItemStockMovement(
                savedItem,
                SuperItemStockMovement.MovementType.ADJUSTMENT,
                null,
                new BigDecimal("4.000"),
                null,
                "Inicial",
                "MANUAL"
        ));

        mockMvc.perform(get("/api/super/barcode-aliases")
                        .param("code", "0075012345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false));
        assertInventoryState(savedItem.getId(), true, "4.000", 1);

        mockMvc.perform(post("/api/super/items/{itemId}/barcode-aliases", savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(barcodeAliasPayload("0075012345678", "EAN_13")))
                .andExpect(status().isCreated());
        assertInventoryState(savedItem.getId(), true, "4.000", 1);

        Long aliasId = superItemBarcodeAliasRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/super/barcode-aliases")
                        .param("code", "0075012345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true));
        assertInventoryState(savedItem.getId(), true, "4.000", 1);

        mockMvc.perform(delete("/api/super/items/{itemId}/barcode-aliases/{aliasId}", savedItem.getId(), aliasId))
                .andExpect(status().isNoContent());
        assertInventoryState(savedItem.getId(), true, "4.000", 1);
    }

    @Test
    void barcodeDuplicateActiveAliasesAreRejectedAndInactiveDuplicatesCanBeReused() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem yerba = superItemRepository.save(new SuperItem("Yerba", almacen));
        SuperItem aceite = superItemRepository.save(new SuperItem("Aceite", almacen));
        String code = "0075012345678";

        mockMvc.perform(post("/api/super/items/{itemId}/barcode-aliases", yerba.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(barcodeAliasPayload(code, "EAN_13")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/super/items/{itemId}/barcode-aliases", aceite.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(barcodeAliasPayload(code, "EAN_13")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe un alias activo para ese código"));

        SuperItemBarcodeAlias inactiveOne = new SuperItemBarcodeAlias(yerba, "0001112223334", "EAN_13");
        inactiveOne.deactivate();
        SuperItemBarcodeAlias inactiveTwo = new SuperItemBarcodeAlias(aceite, "0001112223334", "EAN_13");
        inactiveTwo.deactivate();
        superItemBarcodeAliasRepository.saveAndFlush(inactiveOne);
        superItemBarcodeAliasRepository.saveAndFlush(inactiveTwo);

        SuperItemBarcodeAlias activeAlias = new SuperItemBarcodeAlias(aceite, "0001112223334", "EAN_13");
        superItemBarcodeAliasRepository.saveAndFlush(activeAlias);

        assertThatThrownBy(() -> superItemBarcodeAliasRepository.saveAndFlush(new SuperItemBarcodeAlias(yerba, "0001112223334", "EAN_13")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void barcodeDataIntegrityRaceIsTranslatedToConflict() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = superItemRepository.save(new SuperItem("Yerba", almacen));
        doThrow(new DataIntegrityViolationException("duplicate active_code race"))
                .when(superItemBarcodeAliasRepository)
                .saveAndFlush(any(SuperItemBarcodeAlias.class));

        mockMvc.perform(post("/api/super/items/{itemId}/barcode-aliases", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(barcodeAliasPayload("0075012345678", "EAN_13")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe un alias activo para ese código"));
    }

    @Test
    void barcodeValidationUsesExplicitLimitsAndLabels() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem item = superItemRepository.save(new SuperItem("Yerba", almacen));

        mockMvc.perform(post("/api/super/items/{itemId}/barcode-aliases", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(barcodeAliasPayload("   ", "EAN_13")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ == 'Código de barcode: es obligatorio')]").exists());

        mockMvc.perform(post("/api/super/items/{itemId}/barcode-aliases", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(barcodeAliasPayload("1".repeat(SupermarketLimits.BARCODE_CODE_MAX_LENGTH + 1), "EAN_13")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ == 'Código de barcode: no puede superar "
                        + SupermarketLimits.BARCODE_CODE_MAX_LENGTH + " caracteres')]").exists());

        mockMvc.perform(post("/api/super/items/{itemId}/barcode-aliases", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(barcodeAliasPayload("0075012345678", "X".repeat(SupermarketLimits.BARCODE_FORMAT_MAX_LENGTH + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ == 'Formato de barcode: no puede superar "
                        + SupermarketLimits.BARCODE_FORMAT_MAX_LENGTH + " caracteres')]").exists());
    }

    @Test
    void barcodeDeleteRejectsAliasesOwnedByAnotherItem() throws Exception {
        SuperCategory almacen = superCategoryRepository.save(new SuperCategory("Almacén"));
        SuperItem yerba = superItemRepository.save(new SuperItem("Yerba", almacen));
        SuperItem aceite = superItemRepository.save(new SuperItem("Aceite", almacen));
        SuperItemBarcodeAlias alias = superItemBarcodeAliasRepository.save(new SuperItemBarcodeAlias(yerba, "0075012345678", "EAN_13"));

        mockMvc.perform(delete("/api/super/items/{itemId}/barcode-aliases/{aliasId}", aceite.getId(), alias.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No se encontró el alias de barcode para ese producto"));

        assertThat(superItemBarcodeAliasRepository.findById(alias.getId())).isPresent()
                .get()
                .satisfies(persisted -> assertThat(persisted.isActive()).isTrue());
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

    private void assertInventoryState(Long itemId, boolean expectedChecked, String expectedCurrentStock, int expectedMovementRows) {
        assertThat(superItemRepository.findById(itemId)).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.isChecked()).isEqualTo(expectedChecked);
                    assertThat(persisted.getCurrentStock()).isEqualByComparingTo(expectedCurrentStock);
                });
        assertThat(superItemStockMovementRepository.findAll()).hasSize(expectedMovementRows);
    }

    private void assertCommercialPresentation(Long itemId, String expectedLabel, String expectedQuantity) {
        assertThat(superItemRepository.findById(itemId)).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.getCommercialPresentationLabel()).isEqualTo(expectedLabel);
                    assertThat(persisted.getCommercialPresentationQuantity()).isEqualByComparingTo(expectedQuantity);
                });
    }

    private void assertCommercialPresentationPrice(Long itemId, String expectedLabel, String expectedPrice) {
        assertThat(superItemRepository.findById(itemId)).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.getCommercialPresentationLabel()).isEqualTo(expectedLabel);
                    assertThat(persisted.getCommercialPresentationPricePesos()).isEqualByComparingTo(expectedPrice);
                });
    }

    private void assertCommercialPresentationPriceSource(Long itemId, String expectedLabel, String expectedPrice, String expectedSource) {
        assertThat(superItemRepository.findById(itemId)).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.getCommercialPresentationLabel()).isEqualTo(expectedLabel);
                    if (expectedPrice == null) {
                        assertThat(persisted.getCommercialPresentationPricePesos()).isNull();
                    } else {
                        assertThat(persisted.getCommercialPresentationPricePesos()).isEqualByComparingTo(expectedPrice);
                    }
                    assertThat(persisted.getCommercialPresentationPriceSourceLabel()).isEqualTo(expectedSource);
                });
    }

    private void assertCommercialPresentationPriceSourceAndDate(Long itemId, String expectedLabel, String expectedPrice,
            String expectedSource, String expectedObservedDate) {
        assertThat(superItemRepository.findById(itemId)).isPresent()
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.getCommercialPresentationLabel()).isEqualTo(expectedLabel);
                    if (expectedPrice == null) {
                        assertThat(persisted.getCommercialPresentationPricePesos()).isNull();
                    } else {
                        assertThat(persisted.getCommercialPresentationPricePesos()).isEqualByComparingTo(expectedPrice);
                    }
                    assertThat(persisted.getCommercialPresentationPriceSourceLabel()).isEqualTo(expectedSource);
                    if (expectedObservedDate == null) {
                        assertThat(persisted.getCommercialPresentationPriceObservedDate()).isNull();
                    } else {
                        assertThat(persisted.getCommercialPresentationPriceObservedDate()).isEqualTo(LocalDate.parse(expectedObservedDate));
                    }
                });
    }

    private String barcodeAliasPayload(String code, String format) {
        return """
                {
                  "code": "%s",
                  "format": "%s"
                }
                """.formatted(code, format);
    }

    private SuperItem checkedItem(String name, SuperCategory category, boolean checked) {
        SuperItem item = new SuperItem(name, category);
        item.setChecked(checked);
        return item;
    }

    private SuperItem configuredStockItem(String name, SuperCategory category, String unit, String habitualObjective, String currentStock) {
        SuperItem item = new SuperItem(name, category);
        item.setUnit(unit);
        item.setHabitualObjective(new BigDecimal(habitualObjective));
        item.setCurrentStock(new BigDecimal(currentStock));
        return item;
    }

    private SuperItem itemWithCommercialPresentation(String name, SuperCategory category, String presentationLabel,
            String presentationQuantity) {
        SuperItem item = new SuperItem(name, category);
        item.setCommercialPresentationLabel(presentationLabel);
        if (presentationQuantity != null) {
            item.setCommercialPresentationQuantity(new BigDecimal(presentationQuantity));
        }
        return item;
    }

    private String priceObservationPayload(String pricePesos, String sourceLabel, String observedDate) {
        String sourceEntry = sourceLabel == null ? "" : """
                  "sourceLabel": "%s",
                """.formatted(sourceLabel);
        String observedDateEntry = observedDate == null ? "" : """
                  "observedDate": "%s",
                """.formatted(observedDate);
        return """
                {
                %s%s  "pricePesos": %s
                }
                """.formatted(sourceEntry, observedDateEntry, pricePesos);
    }

    private String priceSourcePayload(String name) {
        return """
                {
                  "name": "%s"
                }
                """.formatted(name);
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

    private String itemPayloadWithCommercialPresentation(String name, Long categoryId, boolean checked, String notes, String unit,
            String habitualObjective, String commercialPresentationLabel, String commercialPresentationQuantity) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "unit": "%s",
                  "habitualObjective": %s,
                  "commercialPresentationLabel": "%s",
                  "commercialPresentationQuantity": %s
                }
                """.formatted(name, categoryId, checked, notes, unit, habitualObjective,
                commercialPresentationLabel, commercialPresentationQuantity);
    }

    private String itemPayloadWithBlankCommercialPresentation(String name, Long categoryId, boolean checked, String notes, String unit,
            String habitualObjective) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "unit": "%s",
                  "habitualObjective": %s,
                  "commercialPresentationLabel": "   "
                }
                """.formatted(name, categoryId, checked, notes, unit, habitualObjective);
    }

    private String itemPayloadWithCommercialQuantityOnly(String name, Long categoryId, boolean checked, String notes, String unit,
            String habitualObjective, String commercialPresentationQuantity) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "unit": "%s",
                  "habitualObjective": %s,
                  "commercialPresentationQuantity": %s
                }
                """.formatted(name, categoryId, checked, notes, unit, habitualObjective, commercialPresentationQuantity);
    }

    private String itemPayloadWithCommercialPresentationPrice(String name, Long categoryId, boolean checked, String notes,
            String commercialPresentationLabel, String commercialPresentationPricePesos) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "commercialPresentationLabel": "%s",
                  "commercialPresentationPricePesos": %s
                }
                """.formatted(name, categoryId, checked, notes, commercialPresentationLabel, commercialPresentationPricePesos);
    }

    private String itemPayloadWithCommercialPresentationPriceSource(String name, Long categoryId, boolean checked, String notes,
            String commercialPresentationLabel, String commercialPresentationPricePesos, String commercialPresentationPriceSourceLabel) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "commercialPresentationLabel": "%s",
                  "commercialPresentationPricePesos": %s,
                  "commercialPresentationPriceSourceLabel": "%s"
                }
                """.formatted(name, categoryId, checked, notes, commercialPresentationLabel, commercialPresentationPricePesos,
                commercialPresentationPriceSourceLabel);
    }

    private String itemPayloadWithCommercialPresentationPriceAndBlankSource(String name, Long categoryId, boolean checked, String notes,
            String commercialPresentationLabel, String commercialPresentationPricePesos) {
        return itemPayloadWithCommercialPresentationPriceSource(name, categoryId, checked, notes, commercialPresentationLabel,
                commercialPresentationPricePesos, "   ");
    }

    private String itemPayloadWithCommercialPresentationPriceSourceAndObservedDate(String name, Long categoryId, boolean checked,
            String notes, String commercialPresentationLabel, String commercialPresentationPricePesos,
            String commercialPresentationPriceSourceLabel, String commercialPresentationPriceObservedDate) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "commercialPresentationLabel": "%s",
                  "commercialPresentationPricePesos": %s,
                  "commercialPresentationPriceSourceLabel": "%s",
                  "commercialPresentationPriceObservedDate": "%s"
                }
                """.formatted(name, categoryId, checked, notes, commercialPresentationLabel, commercialPresentationPricePesos,
                commercialPresentationPriceSourceLabel, commercialPresentationPriceObservedDate);
    }

    private String itemPayloadWithCommercialObservedDateOnly(String name, Long categoryId, boolean checked, String notes,
            String commercialPresentationLabel, String commercialPresentationPriceObservedDate) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "commercialPresentationLabel": "%s",
                  "commercialPresentationPriceObservedDate": "%s"
                }
                """.formatted(name, categoryId, checked, notes, commercialPresentationLabel, commercialPresentationPriceObservedDate);
    }

    private String itemPayloadWithCommercialSourceOnly(String name, Long categoryId, boolean checked, String notes,
            String commercialPresentationLabel, String commercialPresentationPriceSourceLabel) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "commercialPresentationLabel": "%s",
                  "commercialPresentationPriceSourceLabel": "%s"
                }
                """.formatted(name, categoryId, checked, notes, commercialPresentationLabel, commercialPresentationPriceSourceLabel);
    }

    private String itemPayloadWithCommercialPriceOnly(String name, Long categoryId, boolean checked, String notes,
            String commercialPresentationPricePesos) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "checked": %s,
                  "notes": "%s",
                  "commercialPresentationPricePesos": %s
                }
                """.formatted(name, categoryId, checked, notes, commercialPresentationPricePesos);
    }

    private String itemPayloadWithoutCheckedWithCommercialPresentation(String name, Long categoryId, String notes, String unit,
            String habitualObjective, String commercialPresentationLabel, String commercialPresentationQuantity) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "notes": "%s",
                  "unit": "%s",
                  "habitualObjective": %s,
                  "commercialPresentationLabel": "%s",
                  "commercialPresentationQuantity": %s
                }
                """.formatted(name, categoryId, notes, unit, habitualObjective,
                commercialPresentationLabel, commercialPresentationQuantity);
    }

    private String itemPayloadWithoutCheckedWithCommercialPresentationPrice(String name, Long categoryId, String notes, String unit,
            String habitualObjective, String commercialPresentationLabel, String commercialPresentationQuantity,
            String commercialPresentationPricePesos) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "notes": "%s",
                  "unit": "%s",
                  "habitualObjective": %s,
                  "commercialPresentationLabel": "%s",
                  "commercialPresentationQuantity": %s,
                  "commercialPresentationPricePesos": %s
                }
                """.formatted(name, categoryId, notes, unit, habitualObjective,
                commercialPresentationLabel, commercialPresentationQuantity, commercialPresentationPricePesos);
    }

    private String itemPayloadWithoutCheckedWithCommercialPresentationPriceSource(String name, Long categoryId, String notes, String unit,
            String habitualObjective, String commercialPresentationLabel, String commercialPresentationQuantity,
            String commercialPresentationPricePesos, String commercialPresentationPriceSourceLabel) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "notes": "%s",
                  "unit": "%s",
                  "habitualObjective": %s,
                  "commercialPresentationLabel": "%s",
                  "commercialPresentationQuantity": %s,
                  "commercialPresentationPricePesos": %s,
                  "commercialPresentationPriceSourceLabel": "%s"
                }
                """.formatted(name, categoryId, notes, unit, habitualObjective,
                commercialPresentationLabel, commercialPresentationQuantity, commercialPresentationPricePesos,
                commercialPresentationPriceSourceLabel);
    }

    private String itemPayloadWithoutCheckedWithCommercialPresentationPriceSourceAndObservedDate(String name, Long categoryId,
            String notes, String unit, String habitualObjective, String commercialPresentationLabel,
            String commercialPresentationQuantity, String commercialPresentationPricePesos, String commercialPresentationPriceSourceLabel,
            String commercialPresentationPriceObservedDate) {
        return """
                {
                  "name": "%s",
                  "categoryId": %d,
                  "notes": "%s",
                  "unit": "%s",
                  "habitualObjective": %s,
                  "commercialPresentationLabel": "%s",
                  "commercialPresentationQuantity": %s,
                  "commercialPresentationPricePesos": %s,
                  "commercialPresentationPriceSourceLabel": "%s",
                  "commercialPresentationPriceObservedDate": "%s"
                }
                """.formatted(name, categoryId, notes, unit, habitualObjective,
                commercialPresentationLabel, commercialPresentationQuantity, commercialPresentationPricePesos,
                commercialPresentationPriceSourceLabel, commercialPresentationPriceObservedDate);
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
