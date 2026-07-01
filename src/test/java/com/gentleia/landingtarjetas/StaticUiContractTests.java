package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class StaticUiContractTests {

    private static final Path STATIC_ROOT = Path.of("src/main/resources/static");

    @Test
    void indexLinksExpectedStaticAssets() throws IOException {
        String index = readStatic("index.html");

        assertThat(index).contains("<link rel=\"stylesheet\" href=\"/css/styles.css\">");
        assertThat(index).contains("<script type=\"module\" src=\"/js/app.js\"></script>");
        assertThat(readStatic("js/app.js"))
                .contains("./api.js", "./categories.js", "./dashboard.js", "./statements.js", "./transactions.js", "./utils.js");
    }

    @Test
    void apiModuleReferencesEtapaFourUploadReviewEndpoints() throws IOException {
        String api = readStatic("js/api.js");

        assertThat(api).contains(
                "/api/dashboard/summary",
                "/api/dashboard/months",
                "/api/dashboard/months/${yearMonth}",
                "/api/statements",
                "/api/statements/upload",
                "/api/statements/${id}/confirm",
                "/api/transactions",
                "/api/categories"
        );
        assertThat(api).doesNotContain("/api/uploads", "/api/upload", "/api/parse", "/api/parsing", "/api/projections");
    }

    @Test
    void staticUiDoesNotReferenceUnsupportedParsingOrStandaloneProjectionApiEndpoints() throws IOException {
        String staticFiles = readAllStaticText();

        assertThat(staticFiles).doesNotContainPattern("/api/[^\\\"']*(parse|parsing|projection|projections)");
        assertThat(staticFiles).doesNotContain("/api/uploads", "/api/upload");
    }

    @Test
    void projectionUiUsesDashboardMonthEndpointsAndProjectionCopy() throws IOException {
        String index = readStatic("index.html");
        String api = readStatic("js/api.js");
        String app = readStatic("js/app.js");
        String dashboard = readStatic("js/dashboard.js");

        assertThat(api).contains("dashboardMonths()", "dashboardMonthDetail(yearMonth)");
        assertThat(app).contains("api.dashboardMonths()", "api.dashboardMonthDetail(state.month)");
        assertThat(index).contains(
                "Installment projection detail",
                "Projected months estimate remaining installments from confirmed statements",
                "Pesos and USD stay separate with no conversion",
                "id=\"filters-summary\"",
                "id=\"month-detail-table\""
        );
        assertThat(dashboard).contains(
                "Projection-only month",
                "No confirmed statement data is available for",
                "Actual month detail",
                "Missing ${target.title}",
                "Santander VISA",
                "Santander AMEX",
                "Naranja X"
        );
        assertThat(dashboard).contains("Actual confirmed statement data. Projections for this month are suppressed to avoid double-counting.");
        assertThat(dashboard).doesNotContain("Mixed", "Includes confirmed transactions and installment projections");
    }

    @Test
    void polishUiIncludesResponsiveEmptyFilterAndLoadingContracts() throws IOException {
        String index = readStatic("index.html");
        String styles = readStatic("css/styles.css");
        String app = readStatic("js/app.js");
        String statements = readStatic("js/statements.js");
        String transactions = readStatic("js/transactions.js");
        String utils = readStatic("js/utils.js");

        assertThat(index).contains(
                "id=\"clear-transaction-filters\"",
                "No confirmed transactions match the current month, card, category, type, and search filters.",
                "aria-live=\"polite\""
        );
        assertThat(styles).contains("@media (max-width: 680px)", "overflow-x: auto", ".projection-row", ".actual-row");
        assertThat(app).contains("Dashboard data could not be loaded", "Transactions could not be loaded", "setButtonBusy");
        assertThat(statements).contains("Upload could not be completed", "No statement text or raw PDF content is shown", "setButtonBusy");
        assertThat(transactions).contains("resetTransactionFilters", "renderFilterSummary", "Confirmed rows loaded, but none match the current text search.");
        assertThat(utils).contains("export function setButtonBusy", "aria-busy");
    }

    @Test
    void staticUiBehaviorContractsPassWithoutBrowserAutomation() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("node", "src/test/resources/static-ui-contract-tests.mjs")
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(process.waitFor()).as(output).isZero();
    }

    @Test
    void uploadUiUsesExpectedMultipartFieldAndPrivacyCopy() throws IOException {
        String index = readStatic("index.html");
        String api = readStatic("js/api.js");
        String statements = readStatic("js/statements.js");

        assertThat(index).contains(
                "id=\"statement-files\" name=\"files\" type=\"file\"",
                "raw PDFs are processed in memory",
                "raw PDF bytes are not persisted",
                "PDF only. Maximum 1 MB per file and 5 MB per request."
        );
        assertThat(api).contains("formData.append(\"files\", file)");
        assertThat(api).contains("No statement text or PDF content was exposed");
        assertThat(statements).contains("MAX_PDF_SIZE_BYTES = 1_048_576");
        assertThat(statements).doesNotContain("extractedText", "rawText", "pdfText");
    }

    @Test
    void draftReviewUiKeepsDraftsSeparateUntilConfirmation() throws IOException {
        String index = readStatic("index.html");
        String app = readStatic("js/app.js");
        String statements = readStatic("js/statements.js");

        assertThat(index).contains(
                "Drafts are visible only here",
                "Creating missing transactions is a follow-up",
                "Payment month"
        );
        assertThat(app).contains(
                "statement.status === \"CONFIRMED\"",
                "renderDraftStatementList(allStatements)"
        );
        assertThat(statements).contains(
                "Payment month and at least one statement total are required before confirmation.",
                "api.updateTransaction",
                "api.deleteTransaction"
        );
    }

    @Test
    void filterOptionValuesMatchBackendEnums() throws IOException {
        String index = readStatic("index.html");

        assertThat(optionValues(index, "filter-card")).isEqualTo(enumValues("CardBrand"));
        assertThat(optionValues(index, "filter-type")).isEqualTo(enumValues("TransactionType"));
    }

    @Test
    void categoryColorContractUsesSafeHexOnlyPolicy() throws IOException {
        String index = readStatic("index.html");
        String categories = readStatic("js/categories.js");
        String utils = readStatic("js/utils.js");

        assertThat(index).contains("pattern=\"#[0-9A-Fa-f]{6}\"");
        assertThat(categories).contains("pattern=\"#[0-9A-Fa-f]{6}\"", "safeHexColor(category.color)");
        assertThat(categories).doesNotContain("style.background = category.color");
        assertThat(utils).contains("/^#[0-9A-Fa-f]{6}$/");
    }

    @Test
    void currencyCopyKeepsPesosAndUsdSeparateWithoutConversion() throws IOException {
        String index = readStatic("index.html");

        assertThat(index).contains("USD is shown separately. No conversion is applied.");
        assertThat(index).contains("Total pesos", "Total USD");
    }

    private static String readStatic(String relativePath) throws IOException {
        return Files.readString(STATIC_ROOT.resolve(relativePath));
    }

    private static String readAllStaticText() throws IOException {
        try (var files = Files.walk(STATIC_ROOT)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".html") || path.toString().endsWith(".css") || path.toString().endsWith(".js"))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .collect(Collectors.joining("\n"));
        }
    }

    private static Set<String> optionValues(String html, String selectId) {
        var selectMatcher = Pattern.compile("<select id=\\\"" + selectId + "\\\">(.*?)</select>", Pattern.DOTALL).matcher(html);
        assertThat(selectMatcher.find()).isTrue();

        return Pattern.compile("<option value=\\\"([^\\\"]+)\\\">")
                .matcher(selectMatcher.group(1))
                .results()
                .map(result -> result.group(1))
                .collect(Collectors.toSet());
    }

    private static Set<String> enumValues(String enumName) throws IOException {
        Path enumPath = Path.of("src/main/java/com/gentleia/landingtarjetas/shared", enumName + ".java");
        String enumBody = Files.readString(enumPath)
                .replaceAll("(?s).*?\\{", "")
                .replaceAll("(?s)\\}.*", "");

        return Arrays.stream(enumBody.split(","))
                .map(value -> value.replaceAll("[;\\s]", ""))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }
}
