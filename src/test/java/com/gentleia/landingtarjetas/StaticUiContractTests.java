package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class StaticUiContractTests {

    private static final Path STATIC_ROOT = Path.of("src/main/resources/static");

    @Test
    void indexLinksExpectedStaticAssets() throws IOException {
        String index = readStatic("index.html");

        assertThat(index).contains("<link rel=\"stylesheet\" href=\"/css/styles.css?v=20260711-security-login\">");
        assertThat(index).doesNotContain("<link rel=\"stylesheet\" href=\"/css/styles.css\">");
        assertThat(index).contains("<script type=\"module\" src=\"/js/app.js?v=20260711-security-login\"></script>");
        assertThat(readStatic("js/app.js"))
                .contains("./api.js?v=20260712-security-hardening", "./categories.js", "./dashboard.js?v=20260709-stage-7-polish", "./incomes.js", "./manual-expenses.js", "./navigation.js", "./simulator.js?v=20260709-stage-7-polish", "./statements.js", "./transactions.js", "./utils.js")
                .doesNotContain("./api.js\";");
    }

    @Test
    void productionConfigFailsClosedForSecurityAndDatasource() throws IOException {
        String prod = Files.readString(Path.of("src/main/resources/application-prod.properties"), StandardCharsets.UTF_8);
        String springFactories = Files.readString(Path.of("src/main/resources/META-INF/spring.factories"), StandardCharsets.UTF_8);

        assertThat(prod).contains(
                "spring.datasource.url=${APP_DATASOURCE_URL}",
                "spring.datasource.username=${APP_DATASOURCE_USERNAME}",
                "spring.datasource.password=${APP_DATASOURCE_PASSWORD}",
                "app.security.enabled=true",
                "server.tomcat.accesslog.enabled=true",
                "server.tomcat.accesslog.pattern=%h %l %u %t \"%m %U %H\" %s %b %D"
        );
        assertThat(prod).doesNotContain(
                "jdbc:h2:file:./data/landing-tarjetas-prod",
                "APP_DATASOURCE_USERNAME:sa",
                "APP_DATASOURCE_PASSWORD:",
                "app.security.enabled=false",
                "spring.datasource.driver-class-name=org.h2.Driver"
        );
        assertThat(springFactories).contains("ProductionSafetyEnvironmentPostProcessor");
    }

    @Test
    void productionConfigLocksPublicHardeningProperties() throws IOException {
        Properties prod = readProdProperties();

        assertThat(prod.getProperty("spring.h2.console.enabled")).isEqualTo("false");
        assertThat(prod.getProperty("server.servlet.session.cookie.secure"))
                .isEqualTo("${APP_SESSION_COOKIE_SECURE:true}");
        assertThat(prod.getProperty("server.error.include-exception")).isEqualTo("false");
        assertThat(prod.getProperty("server.error.include-message")).isEqualTo("never");
        assertThat(prod.getProperty("server.error.include-stacktrace")).isEqualTo("never");
        assertThat(prod.getProperty("server.error.include-binding-errors")).isEqualTo("never");
        assertThat(prod.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
    }

    @Test
    void directApiImportsUseSecurityHardeningVersion() throws IOException {
        String approvedApiImport = "./api.js?v=20260712-security-hardening";
        Pattern directApiImport = Pattern.compile("(?:from\\s+|import\\(\\s*)[\"'](\\./api\\.js(?:\\?[^\"']*)?)[\"']");
        var imports = new java.util.ArrayList<String>();
        var offenders = new java.util.ArrayList<String>();

        try (var files = Files.walk(STATIC_ROOT.resolve("js"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".js")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                directApiImport.matcher(source).results().forEach(result -> {
                    String importPath = result.group(1);
                    imports.add(STATIC_ROOT.relativize(file) + " -> " + importPath);
                    if (!approvedApiImport.equals(importPath)) {
                        offenders.add(STATIC_ROOT.relativize(file) + " -> " + importPath);
                    }
                });
            }
        }

        assertThat(imports).isNotEmpty();
        assertThat(offenders).isEmpty();
    }

    @Test
    void stageOneTabsRedistributeExistingFrontendSectionsOnly() throws IOException {
        String index = readStatic("index.html");
        String styles = readStatic("css/styles.css");
        String app = readStatic("js/app.js");
        String navigation = readStatic("js/navigation.js");

        assertThat(index).containsSubsequence(
                "role=\"tablist\"",
                "Resumen",
                "Cargar Gastos",
                "Tabla Gastos",
                "Tabla Ingresos",
                "Cargar Ingresos",
                "Simulador",
                "Categorías"
        );
        assertThat(index).contains(
                "id=\"primary-tab-summary\" role=\"tab\"",
                "aria-selected=\"true\" data-tab-target=\"summary\" class=\"active\"",
                "data-tab-panel=\"summary\"",
                "id=\"tab-expenses-upload\" data-tab-panel=\"expenses-upload\"",
                "id=\"tab-expenses-table\" data-tab-panel=\"expenses-table\"",
                "id=\"tab-income-table\" data-tab-panel=\"income-table\"",
                "id=\"tab-income-upload\" data-tab-panel=\"income-upload\"",
                "id=\"tab-simulator\" data-tab-panel=\"simulator\"",
                "id=\"tab-categories\" data-tab-panel=\"categories\""
        );
        assertThat(index).doesNotContain("/api/simulator", "/api/simulador");
        assertThat(styles).contains(".primary-tabs", ".tab-section[hidden]", "overflow-x: auto");
        assertThat(app).contains("setupPrimaryTabs()");
        assertThat(navigation).contains(
                "DEFAULT_PRIMARY_TAB_ID = \"summary\"",
                "label: \"Resumen\"",
                "label: \"Cargar Gastos\"",
                "label: \"Tabla Gastos\"",
                "label: \"Tabla Ingresos\"",
                "label: \"Cargar Ingresos\"",
                "label: \"Simulador\"",
                "label: \"Categorías\""
        );
        assertThat(extractPrimaryTabTargets(index)).containsExactlyElementsOf(extractNavigationTabIds(navigation));
        assertThat(extractPrimaryTabLabels(index)).containsExactlyElementsOf(extractNavigationTabLabels(navigation));
    }

    @Test
    void simulatorUiUsesDashboardSummariesWithoutPersistence() throws IOException {
        String index = readStatic("index.html");
        String app = readStatic("js/app.js");
        String simulator = readStatic("js/simulator.js");
        String staticFiles = readAllStaticText();

        assertThat(index).contains(
                "id=\"simulator-form\"",
                "id=\"simulator-description\"",
                "id=\"simulator-total-amount\"",
                "id=\"simulator-installment-count\"",
                "id=\"simulator-installment-count\" type=\"number\" min=\"1\" max=\"60\" step=\"1\"",
                "id=\"simulator-start-month\"",
                "id=\"simulator-category\"",
                "id=\"clear-simulation\"",
                "id=\"simulation-results-table\"",
                "Ingresos del mes",
                "Deuda/gastos actuales del mes",
                "Nueva cuota simulada",
                "Saldo actual sin simulación",
                "Saldo final con simulación"
        );
        assertThat(app).contains(
                "setSimulatorApi(api)",
                "setupSimulator()",
                "setSimulatorCategories(state.categories)"
        );
        assertThat(simulator).contains(
                "apiClient.summary(month)",
                "MAX_SIMULATOR_INSTALLMENTS = 60",
                "Number(payload.installmentCount) > MAX_SIMULATOR_INSTALLMENTS",
                "calculateMonthlyInstallment(payload.totalAmount, payload.installmentCount)",
                "validateSimulationPayload",
                "clearSimulation",
                "No se guardó en la base de datos."
        );
        assertThat(staticFiles).doesNotContain("createSimulation", "saveSimulation", "risk", "recommendation", "viability", "/api/simulator", "/api/simulador");
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
                "/api/statements/${statementId}/transactions",
                "/api/transactions",
                "/api/categories"
        );
        assertThat(api).doesNotContain("/api/uploads", "/api/upload", "/api/parse", "/api/parsing", "/api/projections");
    }

    @Test
    void securityUiUsesCsrfAwareLoginLogoutAndFetchHelpers() throws IOException {
        String index = readStatic("index.html");
        String login = readStatic("login.html");
        String api = readStatic("js/api.js");
        String app = readStatic("js/app.js");
        String loginJs = readStatic("js/login.js");

        assertThat(index).contains(
                "id=\"logout-form\" method=\"post\" action=\"/logout\"",
                "Cerrar sesión"
        );
        assertThat(login).contains(
                "id=\"login-form\" method=\"post\" action=\"/login\"",
                "name=\"username\"",
                "name=\"password\"",
                "/js/login.js?v=20260711-security-login"
        );
        assertThat(app).contains("from \"./api.js?v=20260712-security-hardening\"")
                .doesNotContain("from \"./api.js\"");
        assertThat(loginJs).contains("from \"./api.js?v=20260712-security-hardening\"")
                .doesNotContain("from \"./api.js\"");
        assertThat(api).contains(
                "export function appendCsrfField(form)",
                "X-XSRF-TOKEN",
                "XSRF-TOKEN",
                "credentials: \"same-origin\""
        );
        assertThat(app).contains("appendCsrfField(document.querySelector(\"#logout-form\"))");
        assertThat(loginJs).contains("appendCsrfField(form)", "Usuario o contraseña inválidos.");
    }

    @Test
    void incomeUiUsesStageThreeApiHelpersAndSpanishContracts() throws IOException {
        String index = readStatic("index.html");
        String api = readStatic("js/api.js");
        String app = readStatic("js/app.js");
        String incomes = readStatic("js/incomes.js");
        String styles = readStatic("css/styles.css");

        assertThat(index).contains(
                "id=\"income-form\"",
                "id=\"income-description\"",
                "id=\"income-type\"",
                "id=\"income-amount\"",
                "id=\"income-start-month\"",
                "id=\"income-recurring-monthly\"",
                "id=\"income-notes\"",
                "id=\"income-filter-month\"",
                "id=\"incomes-table\"",
                "Estado real/proyectado",
                "Aplica desde",
                "Aplica hasta",
                "Crear ingreso",
                "Cargue ingresos manuales en pesos para calcular el saldo mensual del resumen."
        );
        assertThat(optionValues(index, "income-type")).isEqualTo(enumValues("src/main/java/com/gentleia/landingtarjetas/income/IncomeType.java"));
        assertThat(index).doesNotContain("/api/incomes");
        assertThat(api).contains(
                "incomes(filters = {})",
                "withQuery(\"/api/incomes\", filters)",
                "createIncome(payload)",
                "updateIncome(id, payload)",
                "updateIncomeFromMonth(id, yearMonth, payload)",
                "`/api/incomes/${id}/from-month/${yearMonth}`",
                "deleteIncome(id)"
        );
        assertThat(app).contains("setupIncomes({ onChanged: loadDashboard })", "await loadIncomes()");
        assertThat(incomes).contains(
                "Sueldo",
                "Variable",
                "Ingreso creado correctamente.",
                "No se pudieron cargar los ingresos",
                "Guardar desde mes",
                "incomeApi.updateIncomeFromMonth(id, effectiveMonth",
                "No se pudo versionar el ingreso recurrente",
                "notifyIncomeChanged"
        );
        assertThat(styles).contains(".income-form", ".income-table-wrap table", ".income-actions");
    }

    @Test
    void summaryDashboardIncludesIncomeExpenseAndMonthlyBalanceContracts() throws IOException {
        String index = readStatic("index.html");
        String dashboard = readStatic("js/dashboard.js");

        assertThat(index).contains(
                "Total ingresos",
                "id=\"monthly-income-total\"",
                "Total sueldos",
                "id=\"salary-income-total\"",
                "Total ingresos varios",
                "id=\"variable-income-total\"",
                "Ingresos proyectados",
                "id=\"projected-income-total\"",
                "Gastos del mes",
                "Saldo resultante",
                "Estimado",
                "id=\"monthly-balance-pesos\"",
                "id=\"monthly-balance-hint\"",
                "Resúmenes / filas / ingresos"
        );
        assertThat(dashboard).contains(
                "summary?.incomeTotalPesos",
                "summary?.salaryIncomeTotalPesos",
                "summary?.variableIncomeTotalPesos",
                "summary?.projectedIncomeTotalPesos",
                "summary?.estimated",
                "#monthly-income-total",
                "#salary-income-total",
                "#variable-income-total",
                "#projected-income-total",
                "#monthly-balance-pesos",
                "#monthly-balance-hint",
                "summary?.incomeCount"
        );
    }

    @Test
    void manualExpensesUiUsesStageFiveApiHelpersAndSpanishContracts() throws IOException {
        String index = readStatic("index.html");
        String api = readStatic("js/api.js");
        String app = readStatic("js/app.js");
        String manualExpenses = readStatic("js/manual-expenses.js");
        String styles = readStatic("css/styles.css");

        assertThat(index).contains(
                "Gastos manuales y préstamos",
                "id=\"manual-expense-form\"",
                "id=\"manual-expense-description\"",
                "id=\"manual-expense-type\"",
                "id=\"manual-expense-amount-pesos\"",
                "id=\"manual-expense-amount-usd\"",
                "id=\"manual-expense-start-month\"",
                "id=\"manual-expense-total-installments\"",
                "id=\"manual-expense-current-installment\"",
                "id=\"manual-expense-category\"",
                "id=\"manual-expenses-table\"",
                "Pesos y USD se mantienen separados, sin conversión"
        );
        assertThat(optionValues(index, "manual-expense-type")).isEqualTo(enumValues("src/main/java/com/gentleia/landingtarjetas/manualexpense/ManualExpenseType.java"));
        assertThat(index).doesNotContain("/api/manual-expenses");
        assertThat(api).contains(
                "manualExpenses(filters = {})",
                "withQuery(\"/api/manual-expenses\", filters)",
                "createManualExpense(payload)",
                "updateManualExpense(id, payload)",
                "deleteManualExpense(id)"
        );
        assertThat(app).contains(
                "setupManualExpenses({ onChanged: loadDashboard })",
                "api.manualExpenses({ month: state.month })",
                "renderManualExpenses(manualExpenses, state.month)",
                "setManualExpenseCategories(state.categories)"
        );
        assertThat(manualExpenses).contains(
                "Gasto manual creado correctamente.",
                "La cantidad de cuotas es obligatoria para cuotas y préstamos.",
                "La cuota actual no puede superar el total de cuotas.",
                "Préstamo",
                "Efectivo",
                "Proyectado"
        );
        assertThat(styles).contains(".manual-expense-form", ".manual-expense-table-wrap table");
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
                "Detalle de proyección de cuotas",
                "Los meses proyectados estiman las cuotas pendientes de resúmenes confirmados",
                "Pesos y USD se mantienen separados, sin conversión",
                "id=\"filters-summary\"",
                "id=\"month-detail-table\""
        );
        assertThat(dashboard).contains(
                "Mes solo proyectado",
                "No hay datos de resúmenes confirmados para",
                "Detalle del mes confirmado",
                "Falta ${target.title}",
                "Cargado",
                "Faltante",
                "Santander VISA",
                "Santander AMEX",
                "Naranja X"
        );
        assertThat(dashboard).contains("Datos reales de resúmenes confirmados. Las proyecciones de este mes se ocultan para evitar doble conteo.");
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
                "<html lang=\"es-AR\">",
                "id=\"clear-transaction-filters\"",
                "No hay transacciones confirmadas que coincidan con el mes actual, la tarjeta, la categoría, el tipo y la búsqueda.",
                "Aplicar filtros",
                "Limpiar filtros",
                "aria-live=\"polite\""
        );
        assertThat(index).doesNotContain("<html lang=\"en\">");
        assertThat(styles).contains("@media (max-width: 680px)", "overflow-x: auto", ".projection-row", ".actual-row");
        assertThat(styles).contains(
                "--surface-raised",
                "--table-min-width: 980px",
                "--income-table-min-width: 1580px",
                "min-width: var(--table-min-width)",
                "scroll-snap-type: inline proximity",
                ".metric-card.secondary",
                "grid-template-columns: repeat(12, minmax(0, 1fr))",
                "repeat(auto-fit, minmax(13rem, 1fr))",
                "@media (max-width: 520px)"
        );
        assertThat(styles).doesNotContain("--border-strong", ".placeholder-panel .empty-state", ".wide-field");
        assertThat(index).doesNotContain("class=\"wide-field\"");
        assertThat(app).contains("No se pudieron cargar los datos del panel", "No se pudieron cargar las transacciones", "setButtonBusy");
        assertThat(statements).contains("No se pudo completar la carga", "No se muestra texto del resumen ni contenido del PDF original", "parserDisplayLabel", "setButtonBusy");
        assertThat(transactions).contains(
                "resetTransactionFilters",
                "renderFilterSummary",
                "Mes: ${formatMonth(month)}",
                "Hay filas confirmadas cargadas, pero ninguna coincide con la búsqueda actual."
        );
        assertThat(utils).contains("export function setButtonBusy", "aria-busy", "Intl.DateTimeFormat(\"es-AR\"", "Intl.NumberFormat(\"en\"");
        assertThat(utils).doesNotContain("Intl.DateTimeFormat(\"en\"");
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
                "los PDFs originales se procesan en memoria",
                "sus bytes no se persisten",
                "Solo PDF. Máximo 1 MB por archivo y 5 MB por solicitud."
        );
        assertThat(api).contains("formData.append(\"files\", file)");
        assertThat(api).contains("No se expuso texto del resumen ni contenido del PDF");
        assertThat(statements).contains("MAX_PDF_SIZE_BYTES = 1_048_576");
        assertThat(statements).doesNotContain("extractedText", "rawText", "pdfText");
    }

    @Test
    void draftReviewUiKeepsDraftsSeparateUntilConfirmation() throws IOException {
        String index = readStatic("index.html");
        String api = readStatic("js/api.js");
        String app = readStatic("js/app.js");
        String statements = readStatic("js/statements.js");

        assertThat(index).contains(
                "Los borradores son visibles solo aquí",
                "Agregar transacción faltante",
                "Agregue una fila solo cuando a un resumen en borrador le falte un consumo",
                "Transacciones del borrador",
                "Mes de pago"
        );
        assertThat(api).contains("createStatementTransaction(statementId, payload)");
        assertThat(app).contains(
                "statement.status === \"CONFIRMED\"",
                "renderDraftStatementList(allStatements)"
        );
        assertThat(statements).contains(
                "El mes de pago y al menos un total del resumen son obligatorios antes de confirmar.",
                "filas de transacciones en borrador",
                "activeDraft.status !== \"DRAFT\"",
                "api.createStatementTransaction(intent.statementId",
                "api.updateTransaction",
                "api.deleteTransaction"
        );
    }

    @Test
    void staticUiUsesOnlyStatementScopedTransactionCreateEndpoint() throws IOException {
        String staticFiles = readAllStaticText();
        String api = readStatic("js/api.js");

        assertThat(staticFiles).contains("/api/statements/${statementId}/transactions");
        assertThat(api).doesNotContain("request(\"/api/transactions\", {\n            method: \"POST\"");
        assertThat(staticFiles).doesNotContain(
                "/api/statement-transactions",
                "/api/draft-transactions",
                "/api/transactions/create"
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

        assertThat(index).contains("USD se muestra por separado. No se aplica conversión.");
        assertThat(index).contains("Gastos del mes", "Total USD");
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


    private static Properties readProdProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(Files.readString(Path.of("src/main/resources/application-prod.properties"), StandardCharsets.UTF_8)));
        return properties;
    }

    private static Set<String> optionValues(String html, String selectId) {
        var selectMatcher = Pattern.compile("<select id=\\\"" + selectId + "\\\"[^>]*>(.*?)</select>", Pattern.DOTALL).matcher(html);
        assertThat(selectMatcher.find()).isTrue();

        return Pattern.compile("<option value=\\\"([^\\\"]+)\\\">")
                .matcher(selectMatcher.group(1))
                .results()
                .map(result -> result.group(1))
                .collect(Collectors.toSet());
    }

    private static List<String> extractPrimaryTabTargets(String index) {
        return Pattern.compile("<button[^>]*data-tab-target=\"([^\"]+)\"[^>]*>[^<]+</button>")
                .matcher(index)
                .results()
                .map(result -> result.group(1))
                .toList();
    }

    private static List<String> extractPrimaryTabLabels(String index) {
        return Pattern.compile("<button[^>]*data-tab-target=\"[^\"]+\"[^>]*>([^<]+)</button>")
                .matcher(index)
                .results()
                .map(result -> result.group(1))
                .toList();
    }

    private static List<String> extractNavigationTabIds(String navigation) {
        return Pattern.compile("\\{ id: \"([^\"]+)\", label: \"[^\"]+\" }")
                .matcher(navigation)
                .results()
                .map(result -> result.group(1))
                .toList();
    }

    private static List<String> extractNavigationTabLabels(String navigation) {
        return Pattern.compile("\\{ id: \"[^\"]+\", label: \"([^\"]+)\" }")
                .matcher(navigation)
                .results()
                .map(result -> result.group(1))
                .toList();
    }

    private static Set<String> enumValues(String enumName) throws IOException {
        Path enumPath = enumName.contains("/")
                ? Path.of(enumName)
                : Path.of("src/main/java/com/gentleia/landingtarjetas/shared", enumName + ".java");
        String enumBody = Files.readString(enumPath)
                .replaceAll("(?s).*?\\{", "")
                .replaceAll("(?s)\\}.*", "");

        return Arrays.stream(enumBody.split(","))
                .map(value -> value.replaceAll("[;\\s]", ""))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }
}
