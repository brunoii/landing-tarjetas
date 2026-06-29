package com.gentleia.landingtarjetas.statement;

import java.util.List;

import com.gentleia.landingtarjetas.shared.CardBrand;

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
@RequestMapping("/api/statements")
public class StatementController {

    private final StatementService statementService;
    public StatementController(StatementService statementService) {
        this.statementService = statementService;
    }

    @GetMapping
    public List<StatementSummaryResponse> list(@RequestParam(required = false) String month,
                                               @RequestParam(required = false, name = "card") CardBrand cardBrand) {
        return statementService.list(month, cardBrand);
    }

    @GetMapping("/{id}")
    public StatementDetailResponse get(@PathVariable Long id) {
        return statementService.get(id);
    }

    @PutMapping("/{id}")
    public StatementDetailResponse update(@PathVariable Long id, @Valid @RequestBody StatementUpdateRequest request) {
        return statementService.update(id, request);
    }

    @PostMapping("/{id}/confirm")
    public StatementDetailResponse confirm(@PathVariable Long id) {
        return statementService.confirm(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        statementService.delete(id);
    }
}
