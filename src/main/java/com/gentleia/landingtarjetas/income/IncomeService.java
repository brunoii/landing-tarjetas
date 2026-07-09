package com.gentleia.landingtarjetas.income;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IncomeService {

    private final IncomeRepository incomeRepository;

    public IncomeService(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    @Transactional(readOnly = true)
    public List<IncomeResponse> list(String month) {
        if (month == null || month.isBlank()) {
            return incomeRepository.findAllByOrderByStartMonthAscIdAsc().stream()
                    .map(income -> IncomeResponse.from(income, false))
                    .toList();
        }
        LocalDate targetMonth = parseMonth(month);
        return incomeRepository.findAllByOrderByStartMonthAscIdAsc().stream()
                .filter(income -> appliesToMonth(income, targetMonth))
                .map(income -> IncomeResponse.from(income, isProjectedForMonth(income, targetMonth)))
                .toList();
    }

    @Transactional
    public IncomeResponse create(IncomeRequest request) {
        Income income = new Income(
                request.description().trim(),
                request.incomeType(),
                request.amountPesos(),
                parseMonth(request.startMonth()),
                Boolean.TRUE.equals(request.recurringMonthly())
        );
        applyOptionalFields(income, request);
        validateRange(income);
        return IncomeResponse.from(incomeRepository.save(income), false);
    }

    @Transactional
    public IncomeResponse update(Long id, IncomeRequest request) {
        Income income = getIncome(id);
        income.setDescription(request.description().trim());
        income.setIncomeType(request.incomeType());
        income.setAmountPesos(request.amountPesos());
        income.setStartMonth(parseMonth(request.startMonth()));
        income.setRecurringMonthly(Boolean.TRUE.equals(request.recurringMonthly()));
        applyOptionalFields(income, request);
        validateRange(income);
        return IncomeResponse.from(income, false);
    }

    @Transactional
    public void delete(Long id) {
        Income income = getIncome(id);
        incomeRepository.delete(income);
    }

    @Transactional
    public IncomeResponse updateFromMonth(Long id, String month, IncomeRequest request) {
        Income current = getIncome(id);
        LocalDate targetMonth = parseMonth(month);
        LocalDate requestStartMonth = parseMonth(request.startMonth());
        if (!requestStartMonth.equals(targetMonth)) {
            throw new IllegalArgumentException("El mes de inicio debe coincidir con el mes de vigencia de la URL");
        }
        if (!current.isRecurringMonthly()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden versionar ingresos recurrentes");
        }
        if (!targetMonth.isAfter(current.getStartMonth())) {
            throw new IllegalArgumentException("El mes de vigencia debe ser posterior al mes de inicio del ingreso");
        }
        if (current.getEndMonth() != null && targetMonth.isAfter(current.getEndMonth())) {
            throw new IllegalArgumentException("El mes de vigencia debe estar dentro del período recurrente actual");
        }
        if (Boolean.FALSE.equals(request.recurringMonthly())) {
            throw new IllegalArgumentException("El ingreso versionado debe ser recurrente");
        }

        current.setEndMonth(targetMonth.minusMonths(1));

        Income next = new Income(
                request.description().trim(),
                request.incomeType(),
                request.amountPesos(),
                requestStartMonth,
                true
        );
        next.setEndMonth(parseOptionalMonth(request.endMonth()));
        next.setParentIncomeId(current.getId());
        next.setNotes(trimToNull(request.notes()));
        validateRange(next);
        return IncomeResponse.from(incomeRepository.save(next), false);
    }

    private Income getIncome(Long id) {
        return incomeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el ingreso"));
    }

    private void applyOptionalFields(Income income, IncomeRequest request) {
        income.setEndMonth(parseOptionalMonth(request.endMonth()));
        income.setParentIncomeId(request.parentIncomeId());
        income.setNotes(trimToNull(request.notes()));
    }

    private boolean appliesToMonth(Income income, LocalDate targetMonth) {
        if (!income.isRecurringMonthly()) {
            return income.getStartMonth().equals(targetMonth);
        }
        return !income.getStartMonth().isAfter(targetMonth)
                && (income.getEndMonth() == null || !income.getEndMonth().isBefore(targetMonth));
    }

    private boolean isProjectedForMonth(Income income, LocalDate targetMonth) {
        return income.isRecurringMonthly() && targetMonth.isAfter(income.getStartMonth());
    }

    private void validateRange(Income income) {
        if (income.getEndMonth() != null && income.getEndMonth().isBefore(income.getStartMonth())) {
            throw new IllegalArgumentException("El mes de fin no puede ser anterior al mes de inicio");
        }
    }

    private LocalDate parseMonth(String value) {
        return YearMonth.parse(value.trim()).atDay(1);
    }

    private LocalDate parseOptionalMonth(String value) {
        String month = trimToNull(value);
        return month == null ? null : parseMonth(month);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
