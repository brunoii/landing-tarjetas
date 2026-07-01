package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.gentleia.landingtarjetas.category.Category;
import com.gentleia.landingtarjetas.category.CategoryRepository;
import com.gentleia.landingtarjetas.category.CategoryRequest;
import com.gentleia.landingtarjetas.category.CategoryService;
import com.gentleia.landingtarjetas.dashboard.CategoryBreakdownResponse;
import com.gentleia.landingtarjetas.dashboard.DashboardService;
import com.gentleia.landingtarjetas.projection.InstallmentProjection;
import com.gentleia.landingtarjetas.projection.InstallmentProjectionRepository;
import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.ParsingStatus;
import com.gentleia.landingtarjetas.shared.Provider;
import com.gentleia.landingtarjetas.shared.StatementStatus;
import com.gentleia.landingtarjetas.shared.TransactionType;
import com.gentleia.landingtarjetas.statement.CardStatement;
import com.gentleia.landingtarjetas.statement.CardStatementRepository;
import com.gentleia.landingtarjetas.statement.StatementService;
import com.gentleia.landingtarjetas.statement.StatementUpdateRequest;
import com.gentleia.landingtarjetas.statement.StatementUploadService;
import com.gentleia.landingtarjetas.statement.UploadedFileRepository;
import com.gentleia.landingtarjetas.transaction.StatementTransaction;
import com.gentleia.landingtarjetas.transaction.StatementTransactionRepository;
import com.gentleia.landingtarjetas.transaction.TransactionUpdateRequest;
import com.gentleia.landingtarjetas.transaction.TransactionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class DomainServiceValidationTests {

    @Autowired
    private StatementService statementService;
    @Autowired
    private StatementUploadService statementUploadService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private CardStatementRepository statementRepository;
    @Autowired
    private UploadedFileRepository uploadedFileRepository;
    @Autowired
    private StatementTransactionRepository transactionRepository;
    @Autowired
    private InstallmentProjectionRepository projectionRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanDatabase() {
        projectionRepository.deleteAll();
        transactionRepository.deleteAll();
        statementRepository.deleteAll();
        uploadedFileRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void confirmRejectsStatementWithoutPaymentMonth() {
        CardStatement statement = new CardStatement(Provider.MANUAL, CardBrand.VISA);
        statement.setTotalPesos(new BigDecimal("100.00"));
        statement = statementRepository.save(statement);

        Long statementId = statement.getId();
        assertThatThrownBy(() -> statementService.confirm(statementId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payment month");
    }

    @Test
    void confirmRejectsStatementWithoutAnyTotal() {
        CardStatement statement = new CardStatement(Provider.MANUAL, CardBrand.MASTERCARD);
        statement.setPaymentMonth(LocalDate.of(2026, 6, 1));
        statement = statementRepository.save(statement);

        Long statementId = statement.getId();
        assertThatThrownBy(() -> statementService.confirm(statementId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one total amount");
    }

    @Test
    void confirmKeepsPesosAndUsdSeparate() {
        CardStatement statement = new CardStatement(Provider.MANUAL, CardBrand.VISA);
        statement.setPaymentMonth(LocalDate.of(2026, 6, 1));
        statement.setTotalPesos(new BigDecimal("100.00"));
        statement.setTotalUsd(new BigDecimal("25.00"));
        statement = statementRepository.save(statement);

        var response = statementService.confirm(statement.getId());

        assertThat(response.status()).isEqualTo(StatementStatus.CONFIRMED);
        assertThat(response.totalPesos()).isEqualByComparingTo("100.00");
        assertThat(response.totalUsd()).isEqualByComparingTo("25.00");
    }

    @Test
    void confirmGeneratesFutureProjectionsForRemainingInstallments() {
        CardStatement statement = saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 7, 15));
        StatementTransaction transaction = saveTransaction(statement, "Fixture installment purchase", TransactionType.INSTALLMENT,
                new BigDecimal("120.00"), null, null, LocalDate.of(2026, 6, 20));
        transaction.setCurrentInstallment(3);
        transaction.setTotalInstallments(6);
        transactionRepository.save(transaction);

        statementService.confirm(statement.getId());

        assertThat(projectionRepository.findBySourceTransactionIdOrderByProjectedMonthAsc(transaction.getId()))
                .extracting(InstallmentProjection::getInstallmentNumber, InstallmentProjection::getProjectedMonth)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(4, LocalDate.of(2026, 8, 1)),
                        org.assertj.core.groups.Tuple.tuple(5, LocalDate.of(2026, 9, 1)),
                        org.assertj.core.groups.Tuple.tuple(6, LocalDate.of(2026, 10, 1))
                );
    }

    @Test
    void confirmingAgainReplacesProjectionRowsWithoutDuplicates() {
        CardStatement statement = saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 7, 1));
        StatementTransaction transaction = saveTransaction(statement, "Fixture idempotent installment", TransactionType.INSTALLMENT,
                new BigDecimal("80.00"), null, null, LocalDate.of(2026, 6, 20));
        transaction.setCurrentInstallment(1);
        transaction.setTotalInstallments(3);
        transactionRepository.save(transaction);

        statementService.confirm(statement.getId());
        statementService.confirm(statement.getId());

        assertThat(projectionRepository.findBySourceTransactionIdOrderByProjectedMonthAsc(transaction.getId()))
                .hasSize(2)
                .extracting(InstallmentProjection::getInstallmentNumber)
                .containsExactly(2, 3);
    }

    @Test
    void projectionGenerationKeepsPesosAndUsdSeparate() {
        CardStatement statement = saveDraftStatement(CardBrand.AMERICAN_EXPRESS, LocalDate.of(2026, 7, 1));
        StatementTransaction pesos = saveTransaction(statement, "Fixture pesos installment", TransactionType.INSTALLMENT,
                new BigDecimal("70.00"), null, null, LocalDate.of(2026, 6, 20));
        pesos.setCurrentInstallment(1);
        pesos.setTotalInstallments(2);
        StatementTransaction usd = saveTransaction(statement, "Fixture USD installment", TransactionType.INSTALLMENT,
                null, new BigDecimal("12.50"), null, LocalDate.of(2026, 6, 21));
        usd.setCurrentInstallment(1);
        usd.setTotalInstallments(2);
        transactionRepository.saveAll(List.of(pesos, usd));

        statementService.confirm(statement.getId());

        assertThat(projectionRepository.findActiveDetailByProjectedMonth(LocalDate.of(2026, 8, 1)))
                .hasSize(2)
                .satisfies(projections -> {
                    assertThat(projections).filteredOn(projection -> projection.getAmountPesos() != null)
                            .singleElement().satisfies(projection -> {
                                assertThat(projection.getAmountPesos()).isEqualByComparingTo("70.00");
                                assertThat(projection.getAmountUsd()).isNull();
                            });
                    assertThat(projections).filteredOn(projection -> projection.getAmountUsd() != null)
                            .singleElement().satisfies(projection -> {
                                assertThat(projection.getAmountPesos()).isNull();
                                assertThat(projection.getAmountUsd()).isEqualByComparingTo("12.50");
                            });
                });
    }

    @Test
    void updateRejectsConfirmedStatement() {
        CardStatement statement = new CardStatement(Provider.MANUAL, CardBrand.VISA);
        statement.setPaymentMonth(LocalDate.of(2026, 6, 1));
        statement.setTotalPesos(new BigDecimal("100.00"));
        statement.setStatus(StatementStatus.CONFIRMED);
        statement = statementRepository.save(statement);

        var request = new StatementUpdateRequest(
                Provider.MANUAL,
                CardBrand.VISA,
                "Fixture card",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 5, 24),
                LocalDate.of(2026, 6, 5),
                null,
                new BigDecimal("110.00"),
                null,
                null
        );

        Long statementId = statement.getId();
        assertThatThrownBy(() -> statementService.update(statementId, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Only draft statements can be modified");
                });
        assertThat(statementRepository.findById(statementId))
                .get()
                .satisfies(saved -> {
                    assertThat(saved.getPaymentMonth()).isEqualTo(LocalDate.of(2026, 6, 1));
                    assertThat(saved.getTotalPesos()).isEqualByComparingTo("100.00");
                });
    }

    @Test
    void updateStoresMinimumPaymentForDraftReview() {
        CardStatement statement = new CardStatement(Provider.SANTANDER, CardBrand.VISA);
        statement.setPaymentMonth(LocalDate.of(2026, 6, 1));
        statement.setTotalPesos(new BigDecimal("100.00"));
        statement = statementRepository.save(statement);

        var request = new StatementUpdateRequest(
                Provider.SANTANDER,
                CardBrand.VISA,
                "Fixture card",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 5, 24),
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 1),
                new BigDecimal("100.00"),
                null,
                new BigDecimal("25.00")
        );

        var response = statementService.update(statement.getId(), request);

        assertThat(response.minimumPaymentPesos()).isEqualByComparingTo("25.00");
        assertThat(statementRepository.findById(statement.getId()).orElseThrow().getMinimumPaymentPesos())
                .isEqualByComparingTo("25.00");
    }

    @Test
    void deleteRejectsConfirmedStatement() {
        CardStatement statement = saveStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));

        Long statementId = statement.getId();
        assertThatThrownBy(() -> statementService.delete(statementId))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Only draft statements can be modified");
                });
        assertThat(statementRepository.existsById(statementId)).isTrue();
    }

    @Test
    void deleteAllowsDraftStatement() {
        CardStatement statement = saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));

        statementService.delete(statement.getId());

        assertThat(statementRepository.existsById(statement.getId())).isFalse();
    }

    @Test
    void dashboardSummaryAggregatesOnlyRequestedMonthAndKeepsCurrenciesSeparate() {
        CardStatement juneVisa = saveStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));
        CardStatement juneMastercard = saveStatement(CardBrand.MASTERCARD, LocalDate.of(2026, 6, 1));
        CardStatement julyVisa = saveStatement(CardBrand.VISA, LocalDate.of(2026, 7, 1));
        saveTransaction(juneVisa, "Fixture pesos purchase", TransactionType.PURCHASE,
                new BigDecimal("100.00"), null, null, LocalDate.of(2026, 6, 10));
        saveTransaction(juneMastercard, "Fixture USD purchase", TransactionType.PURCHASE,
                null, new BigDecimal("25.00"), null, LocalDate.of(2026, 6, 11));
        saveTransaction(julyVisa, "Fixture other month purchase", TransactionType.PURCHASE,
                new BigDecimal("999.00"), new BigDecimal("99.00"), null, LocalDate.of(2026, 7, 10));

        var summary = dashboardService.summary("2026-06");

        assertThat(summary.paymentMonth()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(summary.totalPesos()).isEqualByComparingTo("100.00");
        assertThat(summary.totalUsd()).isEqualByComparingTo("25.00");
        assertThat(summary.statementCount()).isEqualTo(2);
        assertThat(summary.transactionCount()).isEqualTo(2);
    }

    @Test
    void dashboardSummaryExcludesDraftStatementsAndTransactions() {
        CardStatement confirmed = saveStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));
        CardStatement draft = new CardStatement(Provider.SANTANDER, CardBrand.VISA);
        draft.setStatus(StatementStatus.DRAFT);
        draft.setPaymentMonth(LocalDate.of(2026, 6, 1));
        draft.setTotalPesos(new BigDecimal("999.00"));
        draft = statementRepository.save(draft);
        saveTransaction(confirmed, "Fixture confirmed purchase", TransactionType.PURCHASE,
                new BigDecimal("100.00"), null, null, LocalDate.of(2026, 6, 10));
        saveTransaction(draft, "Fixture draft purchase", TransactionType.PURCHASE,
                new BigDecimal("999.00"), null, null, LocalDate.of(2026, 6, 11));

        var summary = dashboardService.summary("2026-06");

        assertThat(summary.totalPesos()).isEqualByComparingTo("100.00");
        assertThat(summary.statementCount()).isEqualTo(1);
        assertThat(summary.transactionCount()).isEqualTo(1);
    }

    @Test
    void dashboardMonthsAndDetailIncludeRealAndProjectedMonths() {
        CardStatement statement = saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 7, 1));
        statement.setCardAlias("Fixture Visa");
        statement = statementRepository.save(statement);
        Category category = categoryRepository.save(new Category("Fixture planning", "#123456"));
        StatementTransaction transaction = saveTransaction(statement, "Fixture projected purchase", TransactionType.INSTALLMENT,
                new BigDecimal("45.00"), null, category, LocalDate.of(2026, 6, 20));
        transaction.setCurrentInstallment(1);
        transaction.setTotalInstallments(3);
        transactionRepository.save(transaction);
        statementService.confirm(statement.getId());

        var months = dashboardService.months();
        var realMonth = dashboardService.monthDetail("2026-07");
        var projectedMonth = dashboardService.monthDetail("2026-08");

        assertThat(months).anySatisfy(month -> {
            assertThat(month.yearMonth()).isEqualTo("2026-07");
            assertThat(month.currentReal()).isTrue();
            assertThat(month.projectionOnly()).isFalse();
        });
        assertThat(months).anySatisfy(month -> {
            assertThat(month.yearMonth()).isEqualTo("2026-08");
            assertThat(month.currentReal()).isFalse();
            assertThat(month.projectionOnly()).isTrue();
        });
        assertThat(realMonth.currentReal()).isTrue();
        assertThat(realMonth.rows()).filteredOn(row -> row.kind().equals("ACTUAL")).singleElement()
                .satisfies(row -> assertThat(row.installmentNumber()).isEqualTo(1));
        assertThat(projectedMonth.projectionOnly()).isTrue();
        assertThat(projectedMonth.totalPesos()).isEqualByComparingTo("45.00");
        assertThat(projectedMonth.rows()).singleElement().satisfies(row -> {
            assertThat(row.kind()).isEqualTo("PROJECTION");
            assertThat(row.installmentNumber()).isEqualTo(2);
            assertThat(row.totalInstallments()).isEqualTo(3);
            assertThat(row.categoryName()).isEqualTo("Fixture planning");
            assertThat(row.estimatedFinishMonth()).isEqualTo(LocalDate.of(2026, 9, 1));
        });
        assertThat(projectedMonth.totalsByCard()).singleElement().satisfies(total -> {
            assertThat(total.cardBrand()).isEqualTo(CardBrand.VISA);
            assertThat(total.cardAlias()).isEqualTo("Fixture Visa");
            assertThat(total.totalPesos()).isEqualByComparingTo("45.00");
            assertThat(total.totalUsd()).isEqualByComparingTo("0");
        });
    }

    @Test
    void dashboardMonthDetailSuppressesOlderProjectionsWhenRealStatementExists() {
        CardStatement julyStatement = saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 7, 1));
        StatementTransaction julyPesos = saveTransaction(julyStatement, "Fixture projected pesos installment",
                TransactionType.INSTALLMENT, new BigDecimal("45.00"), null, null, LocalDate.of(2026, 6, 20));
        julyPesos.setCurrentInstallment(1);
        julyPesos.setTotalInstallments(2);
        StatementTransaction julyUsd = saveTransaction(julyStatement, "Fixture projected USD installment",
                TransactionType.INSTALLMENT, null, new BigDecimal("10.00"), null, LocalDate.of(2026, 6, 21));
        julyUsd.setCurrentInstallment(1);
        julyUsd.setTotalInstallments(2);
        transactionRepository.saveAll(List.of(julyPesos, julyUsd));
        statementService.confirm(julyStatement.getId());

        CardStatement augustStatement = saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 8, 1));
        StatementTransaction augustPesos = saveTransaction(augustStatement, "Fixture actual pesos installment",
                TransactionType.INSTALLMENT, new BigDecimal("45.00"), null, null, LocalDate.of(2026, 7, 20));
        augustPesos.setCurrentInstallment(2);
        augustPesos.setTotalInstallments(2);
        StatementTransaction augustUsd = saveTransaction(augustStatement, "Fixture actual USD installment",
                TransactionType.INSTALLMENT, null, new BigDecimal("10.00"), null, LocalDate.of(2026, 7, 21));
        augustUsd.setCurrentInstallment(2);
        augustUsd.setTotalInstallments(2);
        transactionRepository.saveAll(List.of(augustPesos, augustUsd));
        statementService.confirm(augustStatement.getId());

        var augustDetail = dashboardService.monthDetail("2026-08");

        assertThat(projectionRepository.findActiveDetailByProjectedMonth(LocalDate.of(2026, 8, 1))).hasSize(2);
        assertThat(augustDetail.currentReal()).isTrue();
        assertThat(augustDetail.projectionOnly()).isFalse();
        assertThat(augustDetail.totalPesos()).isEqualByComparingTo("45.00");
        assertThat(augustDetail.totalUsd()).isEqualByComparingTo("10.00");
        assertThat(augustDetail.rows()).hasSize(2);
        assertThat(augustDetail.rows()).allSatisfy(row -> assertThat(row.kind()).isEqualTo("ACTUAL"));
    }

    @Test
    void uploadPersistsFailedMetadataForNonPdfWithoutDraftStatement() {
        MockMultipartFile file = new MockMultipartFile("files", "synthetic.txt", "text/plain", "not a pdf".getBytes());

        var response = statementUploadService.upload(new MockMultipartFile[]{file});

        assertThat(response.files()).hasSize(1);
        assertThat(response.files().get(0).parsingStatus()).isEqualTo(ParsingStatus.FAILED);
        assertThat(response.files().get(0).draftStatement()).isNull();
        assertThat(uploadedFileRepository.findAll()).singleElement().satisfies(uploadedFile -> {
            assertThat(uploadedFile.getOriginalFilename()).isEqualTo("synthetic.txt");
            assertThat(uploadedFile.getParsingStatus()).isEqualTo(ParsingStatus.FAILED);
            assertThat(uploadedFile.getChecksumSha256()).isNull();
        });
    }

    @Test
    void dashboardCategoryBreakdownAggregatesByMonthAndCategory() {
        Category category = categoryRepository.save(new Category("Fixture services", "#123456"));
        CardStatement juneStatement = saveStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));
        CardStatement julyStatement = saveStatement(CardBrand.VISA, LocalDate.of(2026, 7, 1));
        saveTransaction(juneStatement, "Fixture service pesos", TransactionType.PURCHASE,
                new BigDecimal("40.00"), null, category, LocalDate.of(2026, 6, 1));
        saveTransaction(juneStatement, "Fixture service USD", TransactionType.PURCHASE,
                null, new BigDecimal("10.00"), category, LocalDate.of(2026, 6, 2));
        saveTransaction(juneStatement, "Fixture uncategorized", TransactionType.PURCHASE,
                new BigDecimal("5.00"), null, null, LocalDate.of(2026, 6, 3));
        saveTransaction(julyStatement, "Fixture other month", TransactionType.PURCHASE,
                new BigDecimal("999.00"), null, category, LocalDate.of(2026, 7, 1));

        List<CategoryBreakdownResponse> breakdown = dashboardService.categoryBreakdown("2026-06");

        assertThat(breakdown).hasSize(2);
        assertThat(breakdown)
                .filteredOn(item -> category.getId().equals(item.categoryId()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.categoryName()).isEqualTo("Fixture services");
                    assertThat(item.totalPesos()).isEqualByComparingTo("40.00");
                    assertThat(item.totalUsd()).isEqualByComparingTo("10.00");
                    assertThat(item.transactionCount()).isEqualTo(2);
                });
        assertThat(breakdown)
                .filteredOn(item -> item.categoryId() == null)
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.categoryName()).isEqualTo("Uncategorized");
                    assertThat(item.totalPesos()).isEqualByComparingTo("5.00");
                    assertThat(item.totalUsd()).isEqualByComparingTo("0");
                    assertThat(item.transactionCount()).isEqualTo(1);
                });
    }

    @Test
    void transactionRepositoryFiltersByMonthCardCategoryAndType() {
        Category targetCategory = categoryRepository.save(new Category("Fixture groceries", "#654321"));
        Category otherCategory = categoryRepository.save(new Category("Fixture transport", "#abcdef"));
        CardStatement juneVisa = saveStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));
        CardStatement juneMastercard = saveStatement(CardBrand.MASTERCARD, LocalDate.of(2026, 6, 1));
        CardStatement julyVisa = saveStatement(CardBrand.VISA, LocalDate.of(2026, 7, 1));
        StatementTransaction target = saveTransaction(juneVisa, "Fixture target purchase", TransactionType.PURCHASE,
                new BigDecimal("20.00"), null, targetCategory, LocalDate.of(2026, 6, 10));
        saveTransaction(juneVisa, "Fixture other category", TransactionType.PURCHASE,
                new BigDecimal("21.00"), null, otherCategory, LocalDate.of(2026, 6, 11));
        saveTransaction(juneVisa, "Fixture other type", TransactionType.PAYMENT,
                new BigDecimal("22.00"), null, targetCategory, LocalDate.of(2026, 6, 12));
        saveTransaction(juneMastercard, "Fixture other card", TransactionType.PURCHASE,
                new BigDecimal("23.00"), null, targetCategory, LocalDate.of(2026, 6, 13));
        saveTransaction(julyVisa, "Fixture other month", TransactionType.PURCHASE,
                new BigDecimal("24.00"), null, targetCategory, LocalDate.of(2026, 7, 10));

        var filtered = transactionRepository.findWithFilters(
                LocalDate.of(2026, 6, 1),
                CardBrand.VISA,
                targetCategory.getId(),
                TransactionType.PURCHASE
        );

        assertThat(filtered).extracting(StatementTransaction::getId).containsExactly(target.getId());
    }

    @Test
    void transactionServiceListsOnlyConfirmedTransactions() {
        CardStatement confirmed = saveStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));
        CardStatement draft = new CardStatement(Provider.SANTANDER, CardBrand.VISA);
        draft.setStatus(StatementStatus.DRAFT);
        draft.setPaymentMonth(LocalDate.of(2026, 6, 1));
        draft.setTotalPesos(new BigDecimal("999.00"));
        draft = statementRepository.save(draft);
        StatementTransaction confirmedTransaction = saveTransaction(confirmed, "Fixture confirmed purchase", TransactionType.PURCHASE,
                new BigDecimal("100.00"), null, null, LocalDate.of(2026, 6, 10));
        saveTransaction(draft, "Fixture draft purchase", TransactionType.PURCHASE,
                new BigDecimal("999.00"), null, null, LocalDate.of(2026, 6, 11));

        var transactions = transactionService.list("2026-06", CardBrand.VISA, null, TransactionType.PURCHASE);

        assertThat(transactions).extracting("id").containsExactly(confirmedTransaction.getId());
        assertThat(transactions).extracting("description").doesNotContain("Fixture draft purchase");
    }

    @Test
    void updateRejectsConfirmedStatementTransaction() {
        StatementTransaction transaction = saveTransaction(saveStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1)),
                "Fixture confirmed purchase", TransactionType.PURCHASE,
                new BigDecimal("50.00"), null, null, LocalDate.of(2026, 6, 10));

        var request = new TransactionUpdateRequest(
                LocalDate.of(2026, 6, 11),
                "Fixture changed purchase",
                TransactionType.PURCHASE,
                null,
                new BigDecimal("75.00"),
                null,
                null,
                null,
                "Changed note"
        );

        assertThatThrownBy(() -> transactionService.update(transaction.getId(), request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Only draft statement transactions can be modified");
                });
        assertThat(transactionRepository.findById(transaction.getId()))
                .get()
                .satisfies(saved -> {
                    assertThat(saved.getDescription()).isEqualTo("Fixture confirmed purchase");
                    assertThat(saved.getAmountPesos()).isEqualByComparingTo("50.00");
                    assertThat(saved.getNotes()).isNull();
                });
    }

    @Test
    void deleteRejectsConfirmedStatementTransaction() {
        StatementTransaction transaction = saveTransaction(saveStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1)),
                "Fixture confirmed purchase", TransactionType.PURCHASE,
                new BigDecimal("50.00"), null, null, LocalDate.of(2026, 6, 10));

        assertThatThrownBy(() -> transactionService.delete(transaction.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Only draft statement transactions can be modified");
                });
        assertThat(transactionRepository.existsById(transaction.getId())).isTrue();
    }

    @Test
    void updateAllowsDraftStatementTransaction() {
        StatementTransaction transaction = saveTransaction(saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1)),
                "Fixture draft purchase", TransactionType.PURCHASE,
                new BigDecimal("50.00"), null, null, LocalDate.of(2026, 6, 10));

        var request = new TransactionUpdateRequest(
                LocalDate.of(2026, 6, 11),
                "Fixture reviewed purchase",
                TransactionType.PURCHASE,
                null,
                new BigDecimal("75.00"),
                null,
                null,
                null,
                "Reviewed note"
        );

        var response = transactionService.update(transaction.getId(), request);

        assertThat(response.description()).isEqualTo("Fixture reviewed purchase");
        assertThat(response.amountPesos()).isEqualByComparingTo("75.00");
        assertThat(transactionRepository.findById(transaction.getId()))
                .get()
                .satisfies(saved -> {
                    assertThat(saved.getDescription()).isEqualTo("Fixture reviewed purchase");
                    assertThat(saved.getAmountPesos()).isEqualByComparingTo("75.00");
                    assertThat(saved.getNotes()).isEqualTo("Reviewed note");
                });
    }

    @Test
    void deleteAllowsDraftStatementTransaction() {
        StatementTransaction transaction = saveTransaction(saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1)),
                "Fixture draft purchase", TransactionType.PURCHASE,
                new BigDecimal("50.00"), null, null, LocalDate.of(2026, 6, 10));

        transactionService.delete(transaction.getId());

        assertThat(transactionRepository.existsById(transaction.getId())).isFalse();
    }

    @Test
    void createAllowsDraftStatementTransaction() {
        CardStatement statement = saveDraftStatement(CardBrand.AMERICAN_EXPRESS, LocalDate.of(2026, 7, 1));
        Category category = categoryRepository.save(new Category("Fixture manual category", "#123456"));

        var request = new TransactionUpdateRequest(
                LocalDate.of(2026, 6, 15),
                "Fixture missing transaction",
                TransactionType.PURCHASE,
                category.getId(),
                new BigDecimal("88.50"),
                null,
                null,
                null,
                "Added during draft review"
        );

        var response = transactionService.createForDraftStatement(statement.getId(), request);

        assertThat(response.id()).isNotNull();
        assertThat(response.statementId()).isEqualTo(statement.getId());
        assertThat(response.paymentMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.cardBrand()).isEqualTo(CardBrand.AMERICAN_EXPRESS);
        assertThat(response.category().id()).isEqualTo(category.getId());
        assertThat(response.description()).isEqualTo("Fixture missing transaction");
        assertThat(response.amountPesos()).isEqualByComparingTo("88.50");
        assertThat(transactionRepository.findById(response.id())).get().satisfies(saved -> {
            assertThat(saved.getStatement().getId()).isEqualTo(statement.getId());
            assertThat(saved.getNotes()).isEqualTo("Added during draft review");
        });
    }

    @Test
    void createRejectsConfirmedStatementTransaction() {
        CardStatement statement = saveStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));

        var request = new TransactionUpdateRequest(
                LocalDate.of(2026, 6, 10),
                "Fixture late transaction",
                TransactionType.PURCHASE,
                null,
                new BigDecimal("50.00"),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> transactionService.createForDraftStatement(statement.getId(), request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Only draft statement transactions can be created");
                });
    }

    @Test
    void createRejectsInstallmentWithoutCurrentAndTotalInstallments() {
        CardStatement statement = saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));

        var request = new TransactionUpdateRequest(
                LocalDate.of(2026, 6, 10),
                "Fixture missing installment values",
                TransactionType.INSTALLMENT,
                null,
                new BigDecimal("50.00"),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> transactionService.createForDraftStatement(statement.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("current and total installments");
    }

    @Test
    void createRejectsInstallmentWhenCurrentExceedsTotal() {
        CardStatement statement = saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));

        var request = new TransactionUpdateRequest(
                LocalDate.of(2026, 6, 10),
                "Fixture invalid installment values",
                TransactionType.INSTALLMENT,
                null,
                new BigDecimal("50.00"),
                null,
                4,
                3,
                null
        );

        assertThatThrownBy(() -> transactionService.createForDraftStatement(statement.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    void manuallyCreatedInstallmentParticipatesInConfirmProjectionGeneration() {
        CardStatement statement = saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 7, 1));

        var request = new TransactionUpdateRequest(
                LocalDate.of(2026, 6, 15),
                "Fixture manually added installment",
                TransactionType.INSTALLMENT,
                null,
                new BigDecimal("88.50"),
                null,
                1,
                3,
                "Added during draft review"
        );

        var created = transactionService.createForDraftStatement(statement.getId(), request);

        statementService.confirm(statement.getId());

        assertThat(projectionRepository.findBySourceTransactionIdOrderByProjectedMonthAsc(created.id()))
                .extracting(InstallmentProjection::getInstallmentNumber, InstallmentProjection::getProjectedMonth)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2, LocalDate.of(2026, 8, 1)),
                        org.assertj.core.groups.Tuple.tuple(3, LocalDate.of(2026, 9, 1))
                );
        assertThat(dashboardService.monthDetail("2026-08").rows())
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.kind()).isEqualTo("PROJECTION");
                    assertThat(row.sourceTransactionId()).isEqualTo(created.id());
                    assertThat(row.description()).isEqualTo("Fixture manually added installment");
                    assertThat(row.installmentNumber()).isEqualTo(2);
                    assertThat(row.totalInstallments()).isEqualTo(3);
                    assertThat(row.amountPesos()).isEqualByComparingTo("88.50");
                });
    }

    @Test
    void createRejectsInactiveCategoryAssignment() {
        Category inactive = new Category("Fixture inactive manual category", "#654321");
        inactive.setActive(false);
        inactive = categoryRepository.save(inactive);
        CardStatement statement = saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));

        var request = new TransactionUpdateRequest(
                LocalDate.of(2026, 6, 10),
                "Fixture inactive category transaction",
                TransactionType.PURCHASE,
                inactive.getId(),
                new BigDecimal("50.00"),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> transactionService.createForDraftStatement(statement.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactive category");
    }

    @Test
    void updateRejectsInstallmentWithoutCurrentAndTotalInstallments() {
        StatementTransaction transaction = saveTransaction(TransactionType.PURCHASE);

        var request = new TransactionUpdateRequest(
                LocalDate.of(2026, 6, 10),
                "Private fixture purchase",
                TransactionType.INSTALLMENT,
                null,
                new BigDecimal("50.00"),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> transactionService.update(transaction.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("current and total installments");
    }

    @Test
    void updateRejectsInstallmentWhenCurrentExceedsTotal() {
        StatementTransaction transaction = saveTransaction(TransactionType.PURCHASE);

        var request = new TransactionUpdateRequest(
                LocalDate.of(2026, 6, 10),
                "Private fixture purchase",
                TransactionType.INSTALLMENT,
                null,
                new BigDecimal("50.00"),
                null,
                4,
                3,
                null
        );

        assertThatThrownBy(() -> transactionService.update(transaction.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    void deletingCategoryInUseDeactivatesItInsteadOfHardDeleting() {
        Category category = categoryRepository.save(new Category("Private fixture category", "#123456"));
        StatementTransaction transaction = saveTransaction(TransactionType.PURCHASE);
        transaction.setCategory(category);
        transactionRepository.save(transaction);

        categoryService.deleteSafely(category.getId());

        Category savedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(savedCategory.isActive()).isFalse();
        assertThat(categoryRepository.existsById(category.getId())).isTrue();
    }

    @Test
    void listCategoriesHidesInactiveCategoriesByDefault() {
        Category active = categoryRepository.save(new Category("Fixture active category", "#123456"));
        Category inactive = new Category("Fixture inactive category", "#654321");
        inactive.setActive(false);
        categoryRepository.save(inactive);

        var categories = categoryService.listCategories();

        assertThat(categories).extracting("id").containsExactly(active.getId());
    }

    @Test
    void createRejectsUnsafeCategoryColorCssValues() {
        var request = new CategoryRequest("Fixture category", "url(https://example.test/pixel)", true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hex color");
    }

    @Test
    void createAcceptsHexCategoryColorAndEmptyColorOnly() {
        var hexResponse = categoryService.create(new CategoryRequest("Fixture hex category", "#38bdf8", true));
        var emptyResponse = categoryService.create(new CategoryRequest("Fixture empty color category", "", true));

        assertThat(hexResponse.color()).isEqualTo("#38bdf8");
        assertThat(emptyResponse.color()).isNull();
    }

    @Test
    void updateRejectsInactiveCategoryAssignment() {
        Category inactive = new Category("Fixture inactive category", "#654321");
        inactive.setActive(false);
        inactive = categoryRepository.save(inactive);
        StatementTransaction transaction = saveTransaction(TransactionType.PURCHASE);

        var request = new TransactionUpdateRequest(
                LocalDate.of(2026, 6, 10),
                "Fixture purchase",
                TransactionType.PURCHASE,
                inactive.getId(),
                new BigDecimal("50.00"),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> transactionService.update(transaction.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactive category");
    }

    private StatementTransaction saveTransaction(TransactionType type) {
        CardStatement statement = saveDraftStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));

        return saveTransaction(statement, "Private fixture purchase", type,
                new BigDecimal("50.00"), null, null, LocalDate.of(2026, 6, 10));
    }

    private CardStatement saveStatement(CardBrand cardBrand, LocalDate paymentMonth) {
        CardStatement statement = new CardStatement(Provider.MANUAL, cardBrand);
        statement.setPaymentMonth(paymentMonth);
        statement.setTotalPesos(new BigDecimal("100.00"));
        statement.setStatus(StatementStatus.CONFIRMED);
        return statementRepository.save(statement);
    }

    private CardStatement saveDraftStatement(CardBrand cardBrand, LocalDate paymentMonth) {
        CardStatement statement = new CardStatement(Provider.SANTANDER, cardBrand);
        statement.setPaymentMonth(paymentMonth);
        statement.setTotalPesos(new BigDecimal("100.00"));
        statement.setStatus(StatementStatus.DRAFT);
        return statementRepository.save(statement);
    }

    private StatementTransaction saveTransaction(CardStatement statement, String description, TransactionType type,
                                                 BigDecimal amountPesos, BigDecimal amountUsd, Category category,
                                                 LocalDate transactionDate) {
        StatementTransaction transaction = new StatementTransaction(statement, description, type);
        transaction.setTransactionDate(transactionDate);
        transaction.setAmountPesos(amountPesos);
        transaction.setAmountUsd(amountUsd);
        transaction.setCategory(category);
        return transactionRepository.save(transaction);
    }
}
