package com.gentleia.landingtarjetas.income;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incomes")
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @GetMapping
    public List<IncomeResponse> list(@RequestParam(required = false) String month) {
        return incomeService.list(month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncomeResponse create(@Valid @RequestBody IncomeRequest request) {
        return incomeService.create(request);
    }

    @PutMapping("/{id}")
    public IncomeResponse update(@PathVariable Long id, @Valid @RequestBody IncomeRequest request) {
        return incomeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        incomeService.delete(id);
    }

    @PutMapping("/{id}/from-month/{month}")
    public IncomeResponse updateFromMonth(@PathVariable Long id, @PathVariable String month,
                                          @Valid @RequestBody IncomeRequest request) {
        return incomeService.updateFromMonth(id, month, request);
    }
}
