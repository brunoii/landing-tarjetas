package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gentleia.landingtarjetas.income.Income;
import com.gentleia.landingtarjetas.income.IncomeRepository;
import com.gentleia.landingtarjetas.income.IncomeType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class IncomeControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private IncomeRepository incomeRepository;

    @BeforeEach
    void cleanDatabase() {
        incomeRepository.deleteAll();
    }

    @Test
    void postCreatesIncome() throws Exception {
        mockMvc.perform(post("/api/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSalaryPayload("Fixture salary", "2026-06", null, "550000.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Fixture salary"))
                .andExpect(jsonPath("$.incomeType").value("SALARY"))
                .andExpect(jsonPath("$.amountPesos").value(550000.00))
                .andExpect(jsonPath("$.startMonth").value("2026-06"))
                .andExpect(jsonPath("$.recurringMonthly").value(true))
                .andExpect(jsonPath("$.projected").value(false));

        assertThat(incomeRepository.findAll()).singleElement().satisfies(saved -> {
            assertThat(saved.getDescription()).isEqualTo("Fixture salary");
            assertThat(saved.getStartMonth()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(saved.isRecurringMonthly()).isTrue();
        });
    }

    @Test
    void postReturnsSpanishValidationErrors() throws Exception {
        mockMvc.perform(post("/api/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": " ",
                                  "incomeType": null,
                                  "amountPesos": 0,
                                  "startMonth": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La validación de la solicitud falló"))
                .andExpect(jsonPath("$.details[?(@ == 'Descripción: es obligatoria')]").exists())
                .andExpect(jsonPath("$.details[?(@ == 'Tipo de ingreso: es obligatorio')]").exists())
                .andExpect(jsonPath("$.details[?(@ == 'Importe en pesos: debe ser mayor que cero')]").exists())
                .andExpect(jsonPath("$.details[?(@ == 'Mes de inicio: es obligatorio')]").exists());
    }

    @Test
    void postReturnsSpanishErrorForInvalidIncomeType() throws Exception {
        mockMvc.perform(post("/api/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Fixture salary",
                                  "incomeType": "UNKNOWN",
                                  "amountPesos": 500000.00,
                                  "startMonth": "2026-06"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El cuerpo de la solicitud no es válido"))
                .andExpect(jsonPath("$.details[?(@ == 'Tipo de ingreso: valor inválido')]").exists());
    }

    @Test
    void postReturnsSpanishErrorForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Fixture salary",
                                  "incomeType": "SALARY"
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El cuerpo de la solicitud no es válido"))
                .andExpect(jsonPath("$.details[?(@ == 'Revise que el JSON esté bien formado y que los valores sean válidos')]").exists());
    }

    @Test
    void getByMonthIncludesApplicableRecurringAndOneOffIncomes() throws Exception {
        saveIncome("Fixture salary", IncomeType.SALARY, "500000.00", LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 8, 1), true);
        saveIncome("Fixture bonus", IncomeType.VARIABLE, "90000.00", LocalDate.of(2026, 7, 1), null, false);
        saveIncome("Fixture other month", IncomeType.VARIABLE, "1.00", LocalDate.of(2026, 8, 1), null, false);

        mockMvc.perform(get("/api/incomes").param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Fixture salary"))
                .andExpect(jsonPath("$[0].projected").value(true))
                .andExpect(jsonPath("$[1].description").value("Fixture bonus"))
                .andExpect(jsonPath("$[1].projected").value(false));
    }

    @Test
    void getByMonthTreatsEndMonthAsInclusive() throws Exception {
        saveIncome("Fixture finite salary", IncomeType.SALARY, "450000.00", LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 8, 1), true);

        mockMvc.perform(get("/api/incomes").param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Fixture finite salary"));

        mockMvc.perform(get("/api/incomes").param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void putUpdatesIncome() throws Exception {
        Income income = saveIncome("Fixture income", IncomeType.VARIABLE, "70000.00", LocalDate.of(2026, 6, 1), null, false);

        mockMvc.perform(put("/api/incomes/{id}", income.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createVariablePayload("Fixture updated income", "2026-07", null, "85000.00", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Fixture updated income"))
                .andExpect(jsonPath("$.incomeType").value("VARIABLE"))
                .andExpect(jsonPath("$.amountPesos").value(85000.00))
                .andExpect(jsonPath("$.startMonth").value("2026-07"))
                .andExpect(jsonPath("$.recurringMonthly").value(false));

        assertThat(incomeRepository.findById(income.getId())).get().satisfies(saved -> {
            assertThat(saved.getDescription()).isEqualTo("Fixture updated income");
            assertThat(saved.getStartMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
        });
    }

    @Test
    void deleteRemovesIncome() throws Exception {
        Income income = saveIncome("Fixture disposable income", IncomeType.VARIABLE, "70000.00", LocalDate.of(2026, 6, 1), null, false);

        mockMvc.perform(delete("/api/incomes/{id}", income.getId()))
                .andExpect(status().isNoContent());

        assertThat(incomeRepository.existsById(income.getId())).isFalse();
    }

    @Test
    void futureMonthEditClosesCurrentVersionAndCreatesNextVersion() throws Exception {
        Income income = saveIncome("Fixture salary", IncomeType.SALARY, "500000.00", LocalDate.of(2026, 6, 1), null, true);

        mockMvc.perform(put("/api/incomes/{id}/from-month/{month}", income.getId(), "2026-08")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSalaryPayload("Fixture adjusted salary", "2026-08", null, "650000.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Fixture adjusted salary"))
                .andExpect(jsonPath("$.startMonth").value("2026-08"))
                .andExpect(jsonPath("$.parentIncomeId").value(income.getId()))
                .andExpect(jsonPath("$.recurringMonthly").value(true));

        assertThat(incomeRepository.findById(income.getId())).get()
                .satisfies(previous -> assertThat(previous.getEndMonth()).isEqualTo(LocalDate.of(2026, 7, 1)));

        mockMvc.perform(get("/api/incomes").param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Fixture salary"))
                .andExpect(jsonPath("$[0].amountPesos").value(500000.00));

        mockMvc.perform(get("/api/incomes").param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Fixture adjusted salary"))
                .andExpect(jsonPath("$[0].amountPesos").value(650000.00))
                .andExpect(jsonPath("$[0].projected").value(false));
    }

    @Test
    void futureMonthEditRejectsNonRecurringIncome() throws Exception {
        Income income = saveIncome("Fixture bonus", IncomeType.VARIABLE, "70000.00", LocalDate.of(2026, 6, 1), null, false);

        mockMvc.perform(put("/api/incomes/{id}/from-month/{month}", income.getId(), "2026-08")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createVariablePayload("Fixture changed bonus", "2026-08", null, "90000.00", true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Solo se pueden versionar ingresos recurrentes"));
    }

    @Test
    void futureMonthEditRejectsTargetBeforeOrEqualStartMonth() throws Exception {
        Income income = saveIncome("Fixture salary", IncomeType.SALARY, "500000.00", LocalDate.of(2026, 6, 1), null, true);

        mockMvc.perform(put("/api/incomes/{id}/from-month/{month}", income.getId(), "2026-06")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSalaryPayload("Fixture invalid salary", "2026-06", null, "650000.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El mes de vigencia debe ser posterior al mes de inicio del ingreso"));
    }

    @Test
    void futureMonthEditRejectsRequestStartMonthThatDoesNotMatchPathMonth() throws Exception {
        Income income = saveIncome("Fixture salary", IncomeType.SALARY, "500000.00", LocalDate.of(2026, 6, 1), null, true);

        mockMvc.perform(put("/api/incomes/{id}/from-month/{month}", income.getId(), "2026-08")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSalaryPayload("Fixture invalid salary", "2026-09", null, "650000.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El mes de inicio debe coincidir con el mes de vigencia de la URL"));

        assertThat(incomeRepository.findAll()).hasSize(1);
        assertThat(incomeRepository.findById(income.getId())).get()
                .satisfies(saved -> assertThat(saved.getEndMonth()).isNull());
    }

    @Test
    void futureMonthEditRejectsInvalidRequestStartMonthFormat() throws Exception {
        Income income = saveIncome("Fixture salary", IncomeType.SALARY, "500000.00", LocalDate.of(2026, 6, 1), null, true);

        mockMvc.perform(put("/api/incomes/{id}/from-month/{month}", income.getId(), "2026-08")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSalaryPayload("Fixture invalid salary", "2026/08", null, "650000.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Use el formato de mes YYYY-MM"));
    }

    private Income saveIncome(String description, IncomeType incomeType, String amountPesos, LocalDate startMonth,
                              LocalDate endMonth, boolean recurringMonthly) {
        Income income = new Income(description, incomeType, new BigDecimal(amountPesos), startMonth, recurringMonthly);
        income.setEndMonth(endMonth);
        return incomeRepository.save(income);
    }

    private String createSalaryPayload(String description, String startMonth, String endMonth, String amountPesos) {
        return createVariablePayload(description, startMonth, endMonth, amountPesos, true).replace("VARIABLE", "SALARY");
    }

    private String createVariablePayload(String description, String startMonth, String endMonth, String amountPesos,
                                         boolean recurringMonthly) {
        String endMonthValue = endMonth == null ? "null" : "\"" + endMonth + "\"";
        return """
                {
                  "description": "%s",
                  "incomeType": "VARIABLE",
                  "amountPesos": %s,
                  "startMonth": "%s",
                  "endMonth": %s,
                  "recurringMonthly": %s,
                  "notes": "Fixture notes"
                }
                """.formatted(description, amountPesos, startMonth, endMonthValue, recurringMonthly);
    }
}
