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
import org.springframework.mock.web.MockMultipartFile;

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
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanDatabase() {
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
    void updatePreservesConfirmedStatementPaymentMonthWhenRequestOmitsIt() {
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
                null
        );

        var response = statementService.update(statement.getId(), request);

        assertThat(response.paymentMonth()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(statementRepository.findById(statement.getId()).orElseThrow().getPaymentMonth())
                .isEqualTo(LocalDate.of(2026, 6, 1));
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
        CardStatement statement = saveStatement(CardBrand.VISA, LocalDate.of(2026, 6, 1));

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
