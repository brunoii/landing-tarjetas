package com.gentleia.landingtarjetas.manualexpense;

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
@RequestMapping("/api/manual-expenses")
public class ManualExpenseController {

    private final ManualExpenseService manualExpenseService;

    public ManualExpenseController(ManualExpenseService manualExpenseService) {
        this.manualExpenseService = manualExpenseService;
    }

    @GetMapping
    public List<ManualExpenseResponse> list(@RequestParam(required = false) String month) {
        return manualExpenseService.list(month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManualExpenseResponse create(@Valid @RequestBody ManualExpenseRequest request) {
        return manualExpenseService.create(request);
    }

    @PutMapping("/{id}")
    public ManualExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ManualExpenseRequest request) {
        return manualExpenseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        manualExpenseService.delete(id);
    }
}
