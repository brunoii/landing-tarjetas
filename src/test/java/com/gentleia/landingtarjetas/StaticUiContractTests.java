package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class StaticUiContractTests {

    private static final Path STATIC_ROOT = Path.of("src/main/resources/static");
    private static final String FRESH_STATIC_TOKEN = "20260713-pending-main";
    private static final String STAGE5_API_TOKEN = "20260716-super-inventory-stage5-api";
    private static final String STAGE5_UI_TOKEN = "20260716-super-inventory-stage5-ui";
    private static final String STAGE8_UI_TOKEN = "20260716-super-inventory-stage8-price-source-ui";
    private static final String STAGE9_UI_TOKEN = "20260718-super-inventory-stage9-price-observed-date-ui";
    private static final String STAGE10_API_TOKEN = "20260718-super-inventory-stage10-price-observations-api";
    private static final String STAGE10_UI_TOKEN = "20260718-super-inventory-stage10-price-observations-ui";
    private static final String STAGE11_API_TOKEN = "20260718-super-inventory-stage11-price-sources-api";
    private static final String STAGE11_UI_TOKEN = "20260718-super-inventory-stage11-price-sources-ui";
    private static final String STAGE15_API_TOKEN = "20260725-super-inventory-stage15-ticket-ocr-ui-api";
    private static final String STAGE15_UI_TOKEN = "20260725-super-inventory-stage15-ticket-ocr-ui";
    private static final String FOUNDATION_API_TOKEN = "20260727-mobile-scanner-ocr-pwa-foundation-api";
    private static final String FOUNDATION_UI_TOKEN = "20260727-mobile-scanner-ocr-pwa-foundation-ui";
    private static final String APP_SHELL_UI_TOKEN = "20260730-app-shell-domain-navigation-ui";
    private static final String MOBILE_CONTROL_CSS_TOKEN = "20260731-mobile-control-width";
    private static final String STALE_API_TOKEN = "20260712-security-hardening";

    @Test
    void indexLinksExpectedStaticAssets() throws IOException {
        String index = readStatic("index.html");
        String login = readStatic("login.html");
        String app = readStatic("js/app.js");

        assertThat(index).contains("<link rel=\"stylesheet\" href=\"/css/styles.css?v=" + MOBILE_CONTROL_CSS_TOKEN + "\">");
        assertThat(index).doesNotContain("<link rel=\"stylesheet\" href=\"/css/styles.css\">");
        assertThat(index).contains("<script type=\"module\" src=\"/js/app.js?v=" + APP_SHELL_UI_TOKEN + "\"></script>");
        assertThat(index).doesNotContain("/css/styles.css?v=20260711-security-login", "/js/app.js?v=20260711-security-login");
        assertThat(login).contains("<link rel=\"stylesheet\" href=\"/css/styles.css?v=" + FRESH_STATIC_TOKEN + "\">")
                .contains("/js/login.js?v=" + FRESH_STATIC_TOKEN)
                .doesNotContain("/css/styles.css?v=20260711-security-login", "/js/login.js?v=20260711-security-login");
        assertThat(app)
                .contains("./api.js?v=" + FOUNDATION_API_TOKEN, "./categories.js", "./dashboard.js?v=" + FRESH_STATIC_TOKEN, "./incomes.js?v=" + FRESH_STATIC_TOKEN, "./manual-expenses.js?v=" + FRESH_STATIC_TOKEN, "./navigation.js?v=" + APP_SHELL_UI_TOKEN, "./simulator.js?v=" + FRESH_STATIC_TOKEN, "./statements.js?v=" + FRESH_STATIC_TOKEN, "./supermarket.js?v=" + APP_SHELL_UI_TOKEN, "./transactions.js?v=" + FRESH_STATIC_TOKEN, "./utils.js")
                .doesNotContain("super-inventory-stage17-session-shell")
                .doesNotContain("./api.js\";")
                .doesNotContain("./statements.js\";", "20260709-stage-7-polish", "20260710-mobile-slice-2", "20260711-mobile-simulator", "20260711-mobile-draft-responsive", "20260711-mobile-supermarket");
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
                .isEqualTo("${APP_SESSION_COOKIE_SECURE:false}");
        assertThat(prod.getProperty("server.servlet.session.tracking-modes")).isEqualTo("cookie");
        assertThat(prod.getProperty("server.error.include-exception")).isEqualTo("false");
        assertThat(prod.getProperty("server.error.include-message")).isEqualTo("never");
        assertThat(prod.getProperty("server.error.include-stacktrace")).isEqualTo("never");
        assertThat(prod.getProperty("server.error.include-binding-errors")).isEqualTo("never");
        assertThat(prod.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
    }

    @Test
    void directApiImportsUseExpectedCacheVersions() throws IOException {
        Map<String, String> expectedApiImports = Map.of(
                "js/app.js", "./api.js?v=" + FOUNDATION_API_TOKEN,
                "js/supermarket.js", "./api.js?v=" + FOUNDATION_API_TOKEN,
                "js/incomes.js", "./api.js?v=" + FRESH_STATIC_TOKEN,
                "js/login.js", "./api.js?v=" + FRESH_STATIC_TOKEN,
                "js/statements.js", "./api.js?v=" + FRESH_STATIC_TOKEN
        );
        Pattern directApiImport = Pattern.compile("(?:from\\s+|import\\(\\s*)[\"'](\\./api\\.js(?:\\?[^\"']*)?)[\"']");
        var imports = new java.util.ArrayList<String>();
        var offenders = new java.util.ArrayList<String>();

        try (var files = Files.walk(STATIC_ROOT.resolve("js"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".js")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                directApiImport.matcher(source).results().forEach(result -> {
                    String importPath = result.group(1);
                    imports.add(STATIC_ROOT.relativize(file) + " -> " + importPath);
                    String fileName = STATIC_ROOT.relativize(file).toString().replace('\\', '/');
                    if (!expectedApiImports.getOrDefault(fileName, "").equals(importPath)) {
                        offenders.add(STATIC_ROOT.relativize(file) + " -> " + importPath);
                    }
                });
            }
        }

        assertThat(imports).hasSize(expectedApiImports.size());
        assertThat(offenders).isEmpty();
        assertThat(imports).noneMatch(importPath -> importPath.contains(STALE_API_TOKEN));
    }

    @Test
    void appShellDrawerDefinesRouteBackedHomeAndStockFoundation() throws IOException {
        String index = readStatic("index.html");
        String app = readStatic("js/app.js");
        String navigation = readStatic("js/navigation.js");

        assertThat(index).contains(
                "id=\"app-shell-menu-button\"",
                "aria-controls=\"app-shell-drawer\"",
                "aria-expanded=\"false\"",
                "id=\"app-shell-drawer\"",
                "aria-label=\"Secciones de la aplicación\"",
                "id=\"app-shell-drawer-close\"",
                "aria-label=\"Cerrar navegación\"",
                "id=\"summary-home-title\" tabindex=\"-1\"",
                "id=\"supermarket-title\" tabindex=\"-1\"",
                "href=\"#monthly/summary\" data-shell-route=\"#monthly/summary\">Inicio</a>",
                "href=\"#monthly/expenses-upload\" data-shell-route=\"#monthly/expenses-upload\">Cargar gastos</a>",
                "href=\"#monthly/expenses-table\" data-shell-route=\"#monthly/expenses-table\">Tabla de gastos</a>",
                "href=\"#monthly/income-upload\" data-shell-route=\"#monthly/income-upload\">Cargar ingresos</a>",
                "href=\"#monthly/income-table\" data-shell-route=\"#monthly/income-table\">Tabla de ingresos</a>",
                "href=\"#monthly/simulator\" data-shell-route=\"#monthly/simulator\">Simulador</a>",
                "href=\"#monthly/categories\" data-shell-route=\"#monthly/categories\">Categorías financieras</a>",
                "href=\"#stock/list\" data-shell-route=\"#stock/list\">Lista</a>",
                "href=\"#stock/barcode\" data-shell-route=\"#stock/barcode\">Códigos de barra</a>",
                "href=\"#stock/tickets\" data-shell-route=\"#stock/tickets\">Tickets</a>",
                "href=\"#stock/categories\" data-shell-route=\"#stock/categories\">Categorías de stock</a>"
        );
        assertThat(index).doesNotContain("id=\"primary-tab-summary\"", "id=\"primary-tab-supermarket\"");
        assertThat(app).contains("setupPrimaryTabs()", "syncPrimaryRouteFromLocation({ replace: true })", "addEventListener?.(\"hashchange\"");
        assertThat(navigation).contains(
                "DEFAULT_ROUTE_HASH = \"#monthly/summary\"",
                "function parseHash(hash = \"\")",
                "domain === \"stock\"",
                "replaceState?.(null, \"\", route.hash)",
                "DEFAULT_PRIMARY_TAB_ID = \"summary\"",
                "DEFAULT_SUPERMARKET_TAB_ID = \"list\""
        );
        assertDesktopSidebarCssContract(readStatic("css/styles.css"));
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
                "class=\"simulator-results-table\"",
                "class=\"simulation-month-column\"",
                "class=\"simulation-amount-column\"",
                "class=\"simulation-month-cell\" scope=\"col\"",
                "class=\"simulation-amount-cell\" scope=\"col\"",
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
    void simulatorResultsTableKeepsDesktopColumnsAndMobileReadableCards() throws IOException {
        String index = readStatic("index.html");
        String styles = readStatic("css/styles.css");
        String simulator = readStatic("js/simulator.js");

        assertThat(index).contains("class=\"table-wrap simulator-table-wrap responsive-card-table\"");
        assertThat(cssRule(styles, ".simulator-table-wrap table")).contains(
                "min-width: var(--simulator-table-min-width);",
                "table-layout: fixed;"
        );
        assertThat(cssRule(styles, ".simulator-results-table .simulation-month-column")).contains("width: 14%;");
        assertThat(cssRule(styles, ".simulator-results-table .simulation-amount-column")).contains("width: 17.2%;");
        assertThat(cssRule(styles, ".simulator-results-table .simulation-month-cell")).contains("text-align: left;");
        assertThat(cssRule(styles, ".simulator-results-table .simulation-amount-cell")).contains("text-align: right;");
        assertThat(cssRule(styles, ".simulator-results-table th")).contains(
                "overflow: hidden;",
                "text-overflow: ellipsis;"
        );
        assertThat(cssRule(styles, ".simulator-results-table td")).contains(
                "overflow: hidden;",
                "text-overflow: ellipsis;"
        );
        assertThat(cssRule(styles, ".simulator-results-table .amount")).contains("font-variant-numeric: tabular-nums;");
        assertThat(cssRule(styles, ".table-wrap")).contains("overflow-x: auto;");
        assertThat(styles).doesNotContain(".simulation-result-card");
        assertThat(simulator).contains(
                "class=\"simulation-month-cell\"",
                "class=\"amount simulation-amount-cell\""
        );
        assertCssRuleHasDeclarations(styles, ".simulator-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertDataLabels(simulator, List.of("Mes", "Ingresos del mes", "Deuda/gastos actuales del mes", "Nueva cuota simulada", "Saldo actual sin simulación", "Saldo final con simulación"));
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
                "/api/categories",
                "/api/super/categories",
                "/api/super/items"
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
                "aria-label=\"Cerrar sesión\"",
                "class=\"logout-label-full\">Cerrar sesión</span>",
                "class=\"logout-label-compact\" aria-hidden=\"true\">Salir</span>"
        );
        assertThat(login).contains(
                "id=\"login-form\" method=\"post\" action=\"/login\"",
                "name=\"username\"",
                "name=\"password\"",
                "/js/login.js?v=" + FRESH_STATIC_TOKEN
        );
        assertThat(app).contains("from \"./api.js?v=" + FOUNDATION_API_TOKEN + "\"")
                .doesNotContain(STALE_API_TOKEN)
                .doesNotContain("from \"./api.js\"");
        assertThat(loginJs).contains("from \"./api.js?v=" + FRESH_STATIC_TOKEN + "\"")
                .doesNotContain(STALE_API_TOKEN)
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
                "Estado",
                "Aplica desde",
                "Aplica hasta",
                "id=\"income-edit-modal\"",
                "id=\"income-edit-form\"",
                "aria-describedby=\"income-edit-help income-edit-feedback\"",
                "id=\"income-table-feedback\"",
                "id=\"income-edit-feedback\"",
                "id=\"income-edit-save\"",
                "id=\"income-edit-save-from-month\"",
                "Aplicar cambios desde",
                "Use “Guardar cambios desde el mes seleccionado” para aplicar la edición desde ese mes y conservar sin cambios los meses anteriores.",
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
                "Ingreso vario",
                "Ingreso creado correctamente.",
                "No se pudieron cargar los ingresos",
                "Guardar cambios desde el mes seleccionado",
                "showIncomeEditFeedback",
                "showIncomeTableFeedback",
                "incomeApi.updateIncomeFromMonth(id, effectiveMonth",
                "No se pudo versionar el ingreso recurrente",
                "notifyIncomeChanged",
                "openIncomeEditModal",
                "¿Seguro que desea eliminar este ingreso?"
        );
        assertThat(styles).contains(".income-form", ".income-table-wrap table", ".income-actions");
        assertThat(styles).doesNotContain(".inline-edit-field");
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
                "Resúmenes / Transacciones",
                "Registros cargados desde la API REST local."
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
    void dashboardCardLoadingAndHierarchyPreserveExistingContracts() throws IOException {
        String index = readStatic("index.html");
        String app = readStatic("js/app.js");
        String dashboard = readStatic("js/dashboard.js");
        String styles = readStatic("css/styles.css");

        assertThat(index).contains(
                "id=\"card-detail-grid\"",
                "id=\"card-detail-skeleton-template\"",
                "class=\"card-detail card-detail-skeleton\"",
                "id=\"app-status\"",
                "aria-live=\"polite\""
        );
        assertThat(app).contains(
                "setDashboardLoading(true)",
                "setDashboardLoading(false)",
                "#card-detail-skeleton-template",
                "#card-detail-grid",
                "aria-busy"
        );
        assertThat(dashboard).contains(
                "class=\"card-detail-title\"",
                "class=\"card-total\"",
                "class=\"card-total-value\"",
                "class=\"card-detail-meta\"",
                "status-chip ${isLoaded ? \"loaded\" : \"empty\"}"
        );
        assertThat(styles).contains(
                "--surface-card",
                "--text-strong",
                ".card-detail-skeleton",
                ".card-total-value",
                ".card-detail-meta",
                "@media (max-width: 960px)",
                "@media (max-width: 680px)"
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
    void supermarketUiUsesIndependentSuperApisAndGeneratedListActions() throws IOException {
        String index = readStatic("index.html");
        String api = readStatic("js/api.js");
        String app = readStatic("js/app.js");
        String supermarket = readStatic("js/supermarket.js");
        String styles = readStatic("css/styles.css");
        Map<String, Integer> expectedSupermarketLimits = supermarketLimits();

        assertThat(index).contains(
                "Lista del super",
                "id=\"tab-supermarket\"",
                "class=\"supermarket-card super-suggested-card\"",
                "id=\"super-suggested-title\"",
                "Lista sugerida",
                "id=\"super-suggested-summary\"",
                "id=\"super-suggested-list\"",
                "id=\"super-suggested-empty\"",
                "Todavía no hay sugerencias de compra.",
                "id=\"super-items-table\"",
                "id=\"super-generate-list\"",
                "Generar lista",
                "id=\"super-uncheck-all\"",
                "Desmarcar todos",
                "id=\"super-barcode-form\"",
                "id=\"super-barcode-code\" type=\"text\" data-super-limit=\"barcodeCode\"",
                "id=\"super-barcode-item\"",
                "id=\"super-barcode-attach\"",
                "id=\"super-barcode-remove\"",
                "id=\"super-barcode-scan-start\" type=\"button\"",
                "id=\"super-barcode-scan-stop\" type=\"button\" disabled",
                "id=\"super-barcode-scanner\" hidden",
                "id=\"super-barcode-scanner-preview\" playsinline muted hidden",
                "id=\"super-barcode-scanner-status\" role=\"status\" aria-live=\"polite\"",
                "id=\"super-barcode-actions\" hidden",
                "id=\"super-barcode-purchase\" data-super-barcode-stock-action=\"purchase\"",
                "id=\"super-barcode-consume\" data-super-barcode-stock-action=\"consume\"",
                "Buscar código local",
                "Escanear código",
                "Detener escaneo",
                "La cámara es opcional. Si no está disponible, ingresá el código manualmente.",
                "El escaneo solo resuelve el producto. Enviá compra o consumo a la sesión para revisarlo antes de confirmar stock.",
                "Preparar compra",
                "Preparar consumo",
                "Asociar a producto existente",
                "Quitar alias",
                "Copiar",
                "Descargar TXT",
                "Compartir por WhatsApp",
                "No hay productos marcados para comprar.",
                "id=\"super-item-form\"",
                "Nombre del producto",
                "id=\"super-item-name\" type=\"text\" data-super-limit=\"itemName\"",
                "Unidad opcional",
                "id=\"super-item-unit\" type=\"text\" data-super-limit=\"itemUnit\"",
                "id=\"super-item-presentation-label\" type=\"text\" data-super-limit=\"presentationLabel\"",
                "id=\"super-item-presentation-quantity\" type=\"number\" min=\"0.001\" step=\"0.001\" inputmode=\"decimal\"",
                "id=\"super-item-presentation-price-pesos\" type=\"number\" min=\"0.01\" step=\"0.01\" inputmode=\"decimal\"",
                "id=\"super-item-presentation-price-source\" name=\"commercialPresentationPriceSourceId\"",
                "id=\"super-item-presentation-price-source-label\" type=\"text\" name=\"commercialPresentationPriceSourceLabel\" data-super-limit=\"priceSourceLabel\"",
                "id=\"super-item-presentation-price-observed-date\" type=\"date\" name=\"commercialPresentationPriceObservedDate\"",
                "Presentación comercial opcional",
                "Cantidad por presentación opcional",
                "Precio ref. opcional",
                "Fuente opcional del precio ref.",
                "Fuente reutilizable opcional del precio ref.",
                "Fuente manual opcional para el precio ref.",
                "Use una fuente reutilizable o una manual. Nunca ambas.",
                "Fecha observada opcional del precio ref.",
                "Fecha manual opcional en formato YYYY-MM-DD.",
                "Objetivo habitual opcional",
                "id=\"super-item-objective\" type=\"number\" min=\"0.001\" step=\"0.001\" inputmode=\"decimal\"",
                "id=\"super-item-quick-quantity\" type=\"number\" min=\"0.001\" step=\"0.001\" inputmode=\"decimal\"",
                "id=\"super-item-current-stock\" type=\"number\" min=\"0\" step=\"0.001\" inputmode=\"decimal\"",
                "id=\"super-item-notes\" type=\"text\" data-super-limit=\"itemNotes\"",
                "id=\"super-category-form\"",
                "id=\"super-category-name\" type=\"text\" data-super-limit=\"categoryName\"",
                "id=\"super-category-toggle\"",
                "aria-expanded=\"false\"",
                "aria-controls=\"super-category-table-wrap\"",
                "id=\"super-category-table-wrap\" hidden",
                "class=\"super-category-table\"",
                "<th>Configuración</th>",
                "<th>Presentación</th>",
                "<th>Precio ref.</th>",
                "<th>Stock</th>",
                "<th>Cantidad rápida</th>",
                "<th>Categoría</th>"
        );
        assertThat(index.indexOf("id=\"super-items-table\"")).isLessThan(index.indexOf("id=\"super-category-form\""));
        assertThat(index.indexOf("id=\"super-generated-list\"")).isLessThan(index.indexOf("id=\"super-category-form\""));
        assertThat(supermarketFrontendLimits(supermarket)).containsExactlyInAnyOrderEntriesOf(expectedSupermarketLimits);
        assertThat(supermarketDataLimitKeys(index)).containsExactlyInAnyOrderElementsOf(expectedSupermarketLimits.keySet());
        assertThat(index).doesNotContainPattern("id=\"super-(?:item-name|item-notes|category-name)\"[^>]+maxlength=");
        assertThat(api).contains(
                "superCategories()",
                "request(\"/api/super/categories\")",
                "createSuperCategory(payload)",
                "updateSuperCategory(id, payload)",
                "deleteSuperCategory(id)",
                "superItems()",
                "request(\"/api/super/items\")",
                "superSuggestedList()",
                "request(\"/api/super/suggested-list\")",
                "createSuperItem(payload)",
                "updateSuperItem(id, payload)",
                "updateSuperItemChecked(id, checked)",
                "`/api/super/items/${id}/checked`",
                "adjustSuperItemStock(id, currentStock)",
                "`/api/super/items/${id}/stock-adjustments`",
                "purchaseSuperItem(id, payload)",
                "`/api/super/items/${id}/purchases`",
                "consumeSuperItem(id, payload)",
                "`/api/super/items/${id}/consumptions`",
                "quickConsumeSuperItem(id, payload)",
                "`/api/super/items/${id}/quick-consumptions`",
                "superPriceSources()",
                "request(\"/api/super/price-sources\")",
                "createSuperPriceSource(payload)",
                "request(\"/api/super/price-sources\", {",
                "createSuperItemPriceObservation(id, payload)",
                "`/api/super/items/${id}/price-observations`",
                "superPriceObservations(filters = {})",
                "withQuery(\"/api/super/price-observations\", filters)",
                "superStockMovements(filters = {})",
                "withQuery(\"/api/super/movements\", filters)",
                "lookupSuperItemBarcodeAlias(code)",
                "withQuery(\"/api/super/barcode-aliases\", { code })",
                "attachSuperItemBarcodeAlias(id, payload)",
                "`/api/super/items/${id}/barcode-aliases`",
                "removeSuperItemBarcodeAlias(itemId, aliasId)",
                "`/api/super/items/${itemId}/barcode-aliases/${aliasId}`",
                "error.status = response.status",
                "error.body = body",
                "uncheckAllSuperItems()",
                "request(\"/api/super/items/uncheck-all\""
        ).doesNotContain("/api/super/product-price-sources", "createSuperItemPriceSource(payload)");
        assertThat(app).contains("setupSupermarket({ apiClient: api })");
        assertThat(supermarket).contains(
                "generatedSuperListText",
                "renderSuperSuggestedItems",
                "superSuggestedItemText",
                "groupSuperItems",
                "superItemConfigurationLabel",
                "SUPER_FIELD_LIMITS",
                "applySupermarketFieldLimits",
                "habitualObjective",
                "quickQuantity",
                "currentStock",
                "superItemStockLabel",
                "superItemQuickQuantityLabel",
                "superItemCommercialPresentationLabel",
                "superItemCommercialPresentationPriceLabel",
                "superItemCommercialPresentationPriceSourceLabel",
                "superItemCommercialPresentationPriceObservedDateLabel",
                "superItemCommercialPresentationPriceHtml",
                "commercialPresentationPriceSourceId",
                "renderSuperItemPriceSourceOptions",
                "refreshSuperItemPriceSourceOptions",
                "syncSuperItemPriceSourceInputs",
                "superPriceSourcePayloadFromValues",
                "validateSuperPriceSourcePayload",
                "superPriceObservationPayloadFromValues",
                "validateSuperPriceObservationPayload",
                "superPriceObservationPresentationLabel",
                "superPriceObservationRowHtml",
                "submitSuperPriceObservationForm",
                "loadSuperPriceObservations",
                "commercialPresentationLabel",
                "commercialPresentationQuantity",
                "commercialPresentationPricePesos",
                "commercialPresentationPriceSourceId",
                "commercialPresentationPriceSourceLabel",
                "commercialPresentationPriceObservedDate",
                "selectedPriceSourceId",
                "priceSourceLabel",
                "Observado: ",
                "superMovementTypeLabel",
                "superMovementSummary",
                "data-super-action=\"purchase\"",
                "data-super-action=\"consume\"",
                "data-super-action=\"quick-consume\"",
                "data-super-action=\"history\"",
                "data-super-action=\"price-history\"",
                "selectedPriceObservationItem",
                "resetSuperPriceObservationContext",
                "submitSuperMovementForm",
                "allowNegativeStock: true",
                "superMovementHistoryPanel",
                "adjustSuperItemStock",
                "super-configuration-badge",
                "super-stock-value",
                "https://wa.me/?text=${encodeURIComponent(text)}",
                "lista-super-${date}.txt",
                "¿Seguro que querés eliminar este producto de la lista del super?",
                "¿Querés desmarcar todos los productos?",
                "handleSuperCategoryAction",
                "superCategoryDisplayRowHtml",
                "data-super-category-action=\"edit\"",
                "data-super-category-action=\"delete\"",
                "Mostrar categorías",
                "Ocultar categorías",
                "No hay productos marcados para comprar."
        );
        assertThat(supermarket).contains(
                "supermarketApi.superSuggestedList()",
                "renderSuperSuggestedItems(suggestedItems)",
                "#super-suggested-list",
                "#super-suggested-empty",
                "#super-suggested-summary"
        );
        assertThat(supermarket).contains(
                "normalizeSuperBarcodeCode",
                "superBarcodePayloadFromValues",
                "validateSuperBarcodeLookup",
                "submitSuperBarcodeLookup",
                "attachSuperBarcodeAlias",
                "removeSuperBarcodeAlias",
                "super-item-barcode-match",
                "superBarcodeAliasLabel"
        );
        assertThat(supermarket).doesNotContain("Number(code)", "parseInt(code", "parseFloat(code");
        assertThat(supermarket).contains("BarcodeDetector", "getUserMedia", "visibilitychange", "pagehide");
        assertThat(supermarket).doesNotContain("commercialPresentationPriceObservedAt", "observedAt", "ObservedAt", "datetime", "timestamp");
        assertThat(supermarket).doesNotContain("priceHistory", "price history", "historial de precios", "historial del precio");
        assertThat(supermarket).contains("data-super-action=\"history\"", "super-movement-history");
        assertThat(index).contains(
                "id=\"super-movement-modal\"",
                "id=\"super-movement-form\"",
                "id=\"super-movement-quantity\" type=\"number\" min=\"0.001\" step=\"0.001\"",
                "id=\"super-movement-allow-negative\" type=\"checkbox\"",
                "class=\"super-movement-negative-field\"",
                "id=\"super-movement-history\"",
                "id=\"super-movement-history-table\"",
                "id=\"super-price-observation-form\"",
                "id=\"super-price-observation-item\"",
                "id=\"super-price-observation-price-pesos\" type=\"number\" min=\"0.01\" step=\"0.01\"",
                "id=\"super-price-observation-price-source\"",
                "id=\"super-price-observation-source-label\" type=\"text\" data-super-limit=\"priceSourceLabel\"",
                "id=\"super-price-source-form\"",
                "id=\"super-price-source-name\" type=\"text\" data-super-limit=\"priceSourceLabel\"",
                "id=\"super-price-source-feedback\"",
                "id=\"super-price-observation-observed-date\" type=\"date\"",
                "id=\"super-price-observation-table\"",
                "id=\"super-price-observation-context-summary\"",
                "id=\"super-price-observation-global-reset\"",
                "Registrar observación de precio",
                "Crear fuente de precio",
                "Cantidad",
                "Confirmar stock negativo"
        );
        assertThat(supermarket).contains(
                "document.querySelector(\".super-movement-negative-field\")",
                "negativeField.hidden = type !== \"consume\""
        );
        assertThat(styles).contains(".supermarket-layout", ".super-item-form", ".super-items-table-wrap table", ".super-generated-list", ".super-suggested-card", ".super-suggested-list", ".super-suggested-item", ".super-suggested-quantity", ".super-category-table", ".super-category-actions", ".super-configuration-badge", ".super-configuration-badge.configured", ".super-configuration-badge.pending", ".super-stock-value", ".super-stock-value.unknown", ".super-movement-form", ".super-movement-history", ".super-movement-conflict", ".super-barcode-card", ".super-barcode-form", ".super-item-barcode-match", ".super-barcode-current-alias", ".super-barcode-scan-controls", ".super-barcode-scanner", ".super-barcode-scanner-preview", ".super-barcode-scanner-status", ".super-barcode-action-group", ".super-barcode-action-buttons");
        assertCssRuleHasDeclarations(styles, ".super-configuration-badge", Map.of("display", "inline-flex", "white-space", "normal"));
        assertThat(index).contains(
                "class=\"table-wrap super-items-table-wrap responsive-card-table\"",
                "class=\"table-wrap super-category-table-wrap responsive-card-table\""
        );
        assertCssRuleHasDeclarations(styles, ".super-items-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".super-category-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".super-generated-list", Map.of(
                "white-space", "pre-wrap",
                "overflow-wrap", "anywhere"
        ));
        assertResponsiveCardTableMobileCssContract(styles);
        assertDataLabels(supermarket, List.of("Estado", "Producto", "Categoría", "Configuración", "Presentación", "Precio ref.", "Stock", "Cantidad rápida", "Notas", "Acciones"));
        String supermarketUnsupportedScan = supermarket
                .replace("super-item-presentation-price-pesos", "")
                .replace("super-item-presentation-price-source", "")
                .replace("super-item-presentation-price-source-label", "")
                .replace("super-item-presentation-price-observed-date", "")
                .replace("commercialPresentationPricePesos", "")
                .replace("commercialPresentationPriceSourceId", "")
                .replace("commercialPresentationPriceSourceLabel", "")
                .replace("commercialPresentationPriceObservedDate", "")
                .replace("selectedPriceSourceId", "")
                .replace("priceSourceLabel", "")
                .replace("superItemCommercialPresentationPriceLabel", "")
                .replace("superItemCommercialPresentationPriceSourceLabel", "")
                .replace("superItemCommercialPresentationPriceObservedDateLabel", "")
                .replace("superItemCommercialPresentationPriceHtml", "")
                .replace("super-price-observation-form", "")
                .replace("super-price-observation-item", "")
                .replace("super-price-observation-price-pesos", "")
                .replace("super-price-observation-price-source", "")
                .replace("super-price-observation-source-label", "")
                .replace("super-price-source-form", "")
                .replace("super-price-source-name", "")
                .replace("super-price-source-feedback", "")
                .replace("super-price-observation-observed-date", "")
                .replace("super-price-observation-sync-current-reference-price", "")
                .replace("super-price-observation-table", "")
                .replace("super-price-observation-empty", "")
                .replace("super-price-observation-feedback", "")
                .replace("super-price-observation-title", "")
                .replace("super-price-observation-context-summary", "")
                .replace("super-price-observation-global-reset", "")
                .replace("price-history", "")
                .replace("selectedPriceObservationItem", "")
                .replace("renderSuperPriceObservationContext", "")
                .replace("resetSuperPriceObservationContext", "")
                .replace("superPriceObservationPayloadFromValues", "")
                .replace("validateSuperPriceObservationPayload", "")
                .replace("superPriceSourcePayloadFromValues", "")
                .replace("validateSuperPriceSourcePayload", "")
                .replace("renderSuperItemPriceSourceOptions", "")
                .replace("refreshSuperItemPriceSourceOptions", "")
                .replace("syncSuperItemPriceSourceInputs", "")
                .replace("superPriceObservationPresentationLabel", "")
                .replace("superPriceObservationRowHtml", "")
                .replace("submitSuperPriceSourceForm", "")
                .replace("loadSuperPriceSources", "")
                .replace("renderSuperPriceSources", "")
                .replace("submitSuperPriceObservationForm", "")
                .replace("loadSuperPriceObservations", "")
                .replace("renderSuperPriceObservations", "")
                .replace("renderSuperPriceObservationItemOptions", "")
                .replace("prefillSuperPriceObservationForm", "")
                .replace("createSuperItemPriceObservation", "")
                .replace("createSuperPriceSource", "")
                .replace("superPriceSources", "")
                .replace("superPriceObservations", "")
                .replace("syncCurrentReferencePrice", "")
                .replace("priceSourceId", "")
                .replace("priceSource", "")
                .replace("SuperPriceSource", "")
                .replace("pricePesos", "")
                .replace("20260718-super-inventory-stage10-price-observations-api", "")
                .replace("20260718-super-inventory-stage10-price-observations-ui", "")
                .replace("20260718-super-inventory-stage11-price-sources-api", "")
                .replace("20260718-super-inventory-stage11-price-sources-ui", "")
                .replace("20260725-super-inventory-stage15-ticket-ocr-ui-api", "")
                .replace("20260725-super-inventory-stage15-ticket-ocr-ui", "")
                .replace("super-ticket-ocr", "")
                .replace("TicketOcr", "")
                .replace("submitSuperTicketOcrUploadForm", "")
                .replace("submitSuperTicketOcrConfirmForm", "")
                .replace("renderSuperTicketOcrReview", "")
                .replace("clearSuperTicketOcrReview", "")
                .replace("uploadSuperTicketOcrCandidates", "")
                .replace("ticketOcr", "")
                .replace("OCR", "")
                .replace("ocr", "")
                .replace("ticket image", "")
                .replace("ticket", "")
                .replace("price-observations", "")
                .replace("price-source", "")
                .replace("price-sources", "")
                .replace("/api/super/price-observations", "")
                .replace("/api/super/price-sources", "")
                .replace("Precio", "")
                .replace("Precio ref.", "");
        assertThat(supermarketUnsupportedScan).doesNotContain(
                "OpenFoodFacts", "Tesseract",
                "store", "shop", "shops", "commerce", "comparison", "chart", "charts", "scraping", "automation", "total",
                "presentations", "multiplePresentations", "externalLookup", "autoPurchase", "purchaseAutomation",
                "persistSuggestion", "suggestionPersistence", "saveSuggestion"
        );
    }

    @Test
    void supermarketSessionPanelOwnsInteractiveDraftReviewContracts() throws IOException {
        String index = readStatic("index.html");
        String api = readStatic("js/api.js");
        String supermarket = readStatic("js/supermarket.js");
        String styles = readStatic("css/styles.css");

        assertThat(index).contains(
                "id=\"super-session-panel\"",
                "aria-labelledby=\"super-session-title\"",
                "id=\"super-session-title\"",
                "Revisar sesión antes de confirmar",
                "id=\"super-session-summary\"",
                "id=\"super-session-status\" role=\"status\" aria-live=\"polite\"",
                "id=\"super-session-lines\"",
                "id=\"super-session-empty\"",
                "id=\"super-session-draft-form\"",
                "id=\"super-session-item-name\" type=\"text\" readonly",
                "id=\"super-session-draft-type\"",
                "id=\"super-session-draft-quantity\" type=\"number\" min=\"0.001\" step=\"0.001\"",
                "id=\"super-session-draft-notes\"",
                "id=\"super-session-draft-allow-negative\" type=\"checkbox\"",
                "id=\"super-session-cancel-draft\"",
                "id=\"super-session-clear\" disabled",
                "id=\"super-session-save-draft\"",
                "id=\"super-session-confirm\""
        );
        assertThat(index).contains("La sesión revisa borradores no mutantes. El modal directo sigue disponible solo desde la tabla de productos.");
        assertThat(index.indexOf("id=\"super-items-table\"")).isLessThan(index.indexOf("id=\"super-session-panel\""));
        assertThat(api).contains(
                "activeSuperScanSession()",
                "createActiveSuperScanSession()",
                "queueSuperScanSessionResolvedItem(sessionId, payload)",
                "createSuperScanSessionDraft(sessionId, payload)",
                "updateSuperScanSessionDraft(sessionId, draftId, payload)",
                "deleteSuperScanSessionDraft(sessionId, draftId)",
                "confirmSuperScanSession(sessionId)",
                "`/api/super/scan-sessions/${sessionId}/resolved-items`",
                "`/api/super/scan-sessions/${sessionId}/drafts/${draftId}`",
                "`/api/super/scan-sessions/${sessionId}/confirm`"
        );
        assertThat(index).contains(
                "aria-controls=\"super-session-panel\"",
                "aria-controls=\"super-session-draft-form\"",
                "aria-controls=\"super-session-lines\""
        );
        assertThat(supermarket).contains(
                "activeSuperScanSession",
                "queueSuperScanSessionResolvedItem",
                "createSuperScanSessionDraft",
                "updateSuperScanSessionDraft",
                "deleteSuperScanSessionDraft",
                "confirmSuperScanSession",
                "#super-session-lines",
                "#super-session-save-draft",
                "#super-session-confirm"
        );
        assertThat(styles).contains(".super-session-panel", ".super-session-summary", ".super-session-table-wrap table", ".super-session-draft-form", ".super-session-actions");
    }

    @Test
    void supermarketSubtabsLiveMarkupDefinesSingleActiveAriaTabPanelSet() throws IOException {
        String index = readStatic("index.html");

        assertThat(index).contains(
                "role=\"tablist\"",
                "aria-label=\"Secciones de stock\"",
                "id=\"super-tablist\"",
                "id=\"super-tab-list\"",
                "id=\"super-tab-barcode\"",
                "id=\"super-tab-tickets\"",
                "id=\"super-tab-categories\"",
                "aria-controls=\"super-panel-list\"",
                "aria-controls=\"super-panel-barcode\"",
                "aria-controls=\"super-panel-tickets\"",
                "aria-controls=\"super-panel-categories\"",
                "data-super-tab-target=\"list\"",
                "data-super-tab-target=\"barcode\"",
                "data-super-tab-target=\"tickets\"",
                "data-super-tab-target=\"categories\"",
                "role=\"tabpanel\"",
                "aria-labelledby=\"super-tab-list\"",
                "aria-labelledby=\"super-tab-barcode\"",
                "aria-labelledby=\"super-tab-tickets\"",
                "aria-labelledby=\"super-tab-categories\""
        );
        assertThat(countMatches(index, "aria-selected=\"true\"")).isEqualTo(1);
        assertThat(countMatches(index, "id=\"super-tab-")).isEqualTo(4);
        assertThat(countMatches(index, "data-super-tab-panel=")).isEqualTo(4);
        assertThat(index).doesNotContain("supermarket-subtabs-contract", "super-mobile-shell-nav", "super-mobile-shell-link");
    }

    @Test
    void supermarketSubtabsLiveStylesKeepMobileTabsDirectlyReachableWithoutOverflowOnlyAccess() throws IOException {
        String styles = readStatic("css/styles.css");

        assertCssRuleHasDeclarations(styles, ".supermarket-subtabs", Map.of(
                "display", "flex",
                "flex-wrap", "wrap",
                "width", "100%",
                "max-width", "100%",
                "min-width", "0",
                "padding", "0.25rem 0 0.35rem"
        ));
        assertCssRuleHasDeclarations(styles, ".supermarket-subtabs [role=\"tab\"]", Map.of(
                "min-height", "var(--tap-target-min)",
                "white-space", "normal",
                "flex", "1 1 10rem"
        ));
        assertCssMediaRuleHasDeclarations(styles, "@media (max-width: 680px)", ".supermarket-subtabs [role=\"tab\"]", Map.of(
                "flex", "1 1 calc(50% - 0.5rem)"
        ));
        assertNoCssDeclaration(styles, List.of(".supermarket-subtabs"), "overflow-x", "auto");
        assertNoCssDeclaration(styles, List.of(".supermarket-subtabs"), "overflow", "hidden");
        assertCssRuleHasDeclarations(styles, ".supermarket-subtab-panel[hidden]", Map.of("display", "none"));
        assertThat(styles).contains(".supermarket-subtab-panel").doesNotContain(".super-mobile-shell-nav", ".super-mobile-shell-link");
    }

    @Test
    void supermarketSubtabsLivePanelsKeepStableControlsGroupedBySection() throws IOException {
        String index = readStatic("index.html");
        assertThat(index).contains(
                "id=\"super-panel-list\"",
                "id=\"super-panel-barcode\"",
                "id=\"super-panel-tickets\"",
                "id=\"super-panel-categories\"",
                "id=\"super-generate-list\"",
                "id=\"super-uncheck-all\"",
                "id=\"super-items-table\"",
                "id=\"super-generated-list\"",
                "id=\"super-barcode-form\"",
                "id=\"super-barcode-actions\"",
                "id=\"super-session-panel\"",
                "id=\"super-ticket-ocr-form\"",
                "id=\"super-ticket-ocr-review-panel\"",
                "id=\"super-item-form\"",
                "id=\"super-category-form\"",
                "id=\"super-category-table-wrap\""
        );
        assertThat(index.indexOf("id=\"super-panel-list\"")).isLessThan(index.indexOf("id=\"super-generate-list\""));
        assertThat(index.indexOf("id=\"super-generate-list\"")).isLessThan(index.indexOf("id=\"super-panel-barcode\""));
        assertThat(index.indexOf("id=\"super-panel-barcode\"")).isLessThan(index.indexOf("id=\"super-barcode-form\""));
        assertThat(index.indexOf("id=\"super-session-panel\"")).isLessThan(index.indexOf("id=\"super-panel-tickets\""));
        assertThat(index.indexOf("id=\"super-panel-tickets\"")).isLessThan(index.indexOf("id=\"super-ticket-ocr-form\""));
        assertThat(index.indexOf("id=\"super-ticket-ocr-review-panel\"")).isLessThan(index.indexOf("id=\"super-panel-categories\""));
        assertThat(index.indexOf("id=\"super-panel-categories\"")).isLessThan(index.indexOf("id=\"super-item-form\""));
        assertThat(index.indexOf("id=\"super-item-form\"")).isLessThan(index.indexOf("id=\"super-category-form\""));
    }

    @Test
    void superPriceObservationSyncControlIsExplicitUncheckedAndObservationScoped() throws IOException {
        String index = readStatic("index.html");

        assertThat(index).contains(
                "id=\"super-price-observation-form\"",
                "id=\"super-price-observation-sync-current-reference-price\" type=\"checkbox\"",
                "Sincronizar como precio actual/de referencia",
                "Opcional: también actualiza el precio de referencia del producto con esta observación. Si no se marca, solo se guarda historial."
        );
        assertThat(index).doesNotContain("id=\"super-price-observation-sync-current-reference-price\" type=\"checkbox\" checked");
        assertThat(htmlSection(index, "super-price-observation-form", "</form>")).contains("super-price-observation-sync-current-reference-price");
        assertThat(htmlSection(index, "super-item-form", "</form>")).doesNotContain("super-price-observation-sync-current-reference-price", "syncCurrentReferencePrice");
    }

    @Test
    void superPriceObservationSyncPayloadRefreshFeedbackAndExcludedScopeStayStatic() throws IOException {
        String supermarket = readStatic("js/supermarket.js");
        String api = readStatic("js/api.js");
        String staticFiles = readAllStaticText();

        assertThat(supermarket).contains(
                "syncCurrentReferencePrice: values?.syncCurrentReferencePrice === true || values?.syncCurrentReferencePrice === \"true\" || values?.syncCurrentReferencePrice === \"on\"",
                "delete payload.syncCurrentReferencePrice",
                "document.querySelector(\"#super-price-observation-sync-current-reference-price\")?.checked",
                "await loadSupermarket()",
                "Observación registrada y precio actual/de referencia actualizado.",
                "Observación de precio registrada."
        );
        assertThat(api).contains("createSuperItemPriceObservation(id, payload)");
        assertThat(api).doesNotContain("syncCurrentReferencePrice");
        assertThat(staticFiles).doesNotContain(
                "source admin", "comparison", "Tesseract", "Stage 15", "multiple prices"
        );
    }

    @Test
    void ticketOcrUiUsesTransientUploadReviewAndExistingConfirmationContracts() throws IOException {
        String index = readStatic("index.html");
        String api = readStatic("js/api.js");
        String supermarket = readStatic("js/supermarket.js");
        String styles = readStatic("css/styles.css");

        assertThat(index).contains(
                "id=\"super-ticket-ocr-form\"",
                "id=\"super-ticket-ocr-file\" name=\"file\" type=\"file\" accept=\"image/png,image/jpeg,.png,.jpg,.jpeg\"",
                "OCR de ticket",
                "id=\"super-ticket-ocr-summary\"",
                "id=\"super-ticket-ocr-warning-list\"",
                "id=\"super-ticket-ocr-debug-details\"",
                "id=\"super-ticket-ocr-debug-summary\"",
                "id=\"super-ticket-ocr-debug-list\"",
                "id=\"super-ticket-ocr-table\"",
                "id=\"super-ticket-ocr-empty\"",
                "id=\"super-ticket-ocr-confirm-form\"",
                "id=\"super-ticket-ocr-line-index\" type=\"hidden\"",
                "id=\"super-ticket-ocr-selected-line\"",
                "id=\"super-ticket-ocr-description\"",
                "id=\"super-ticket-ocr-price-pesos\" type=\"number\" min=\"0.01\" step=\"0.01\"",
                "id=\"super-ticket-ocr-product\"",
                "id=\"super-ticket-ocr-date-candidate\"",
                "id=\"super-ticket-ocr-date\" type=\"date\"",
                "id=\"super-ticket-ocr-source-candidate\"",
                "id=\"super-ticket-ocr-source-label\" type=\"text\" data-super-limit=\"priceSourceLabel\"",
                "id=\"super-ticket-ocr-sync-current-reference-price\" type=\"checkbox\"",
                "id=\"super-ticket-ocr-confirm-feedback\"",
                "id=\"super-ticket-ocr-discard\"",
                "Código",
                "Cantidad",
                "Precio unitario",
                "Total línea",
                "IVA",
                "Extraer candidatos",
                "Confirmar observación",
                "Descartar revisión",
                "Detalle OCR y ruido oculto",
                "Los candidatos se descartan al refrescar o descartar esta revisión.",
                "Nada se guarda hasta confirmar una fila.",
                "Usá JPG locales autorizados solo para verificación manual. No los copies, persistas ni los compartas."
        );
        assertThat(index).doesNotContain("sessionStorage", "localStorage", "BarcodeDetector", "getUserMedia");
        assertThat(api).contains(
                "uploadSuperTicketOcrCandidates(file)",
                "formData.append(\"file\", file)",
                "uploadRequest(\"/api/super/ticket-ocr/candidates\""
        ).doesNotContain("/api/super/ticket-ocr/confirm", "/api/super/ticket-ocr/save");
        assertThat(supermarket).contains(
                "submitSuperTicketOcrUploadForm",
                "submitSuperTicketOcrConfirmForm",
                "renderSuperTicketOcrReview",
                "renderSuperTicketOcrDebugLines",
                "createSuperItemPriceObservation",
                "data-super-ticket-ocr-action=\"select\"",
                "RUNTIME_UNAVAILABLE",
                "EMPTY_EXTRACTION",
                "INVALID_FILE",
                "DECODE_FAILED",
                "debugLines",
                "barcodeOrStoreCode",
                "unitPricePesos",
                "lineTotalPesos",
                "taxPesos"
        ).doesNotContain("localStorage", "sessionStorage");
        assertThat(styles).contains(
                ".super-ticket-ocr-card",
                ".super-ticket-ocr-upload-form",
                ".super-ticket-ocr-review-grid",
                ".super-ticket-ocr-warning-list",
                ".super-ticket-ocr-selected-warnings",
                ".super-ticket-ocr-table-wrap table",
                ".super-ticket-ocr-debug-details",
                ".super-ticket-ocr-debug-list"
        );
        assertThat(index).doesNotContain("id=\"super-ticket-ocr-debug-details\" open");
    }

    @Test
    void privacySafePwaShellPublishesManifestWorkerOfflineAndSafeIcons() throws IOException {
        String index = readStatic("index.html");
        String manifest = readStatic("manifest.webmanifest");
        String worker = readStatic("service-worker.js");
        String offline = readStatic("offline.html");
        String icon192 = readStatic("icons/icon-192.svg");
        String icon512 = readStatic("icons/icon-512.svg");

        assertThat(index).contains(
                "<link rel=\"manifest\" href=\"/manifest.webmanifest\">",
                "navigator.serviceWorker.register(\"/service-worker.js\", { scope: \"/\" })",
                "window.isSecureContext",
                "serviceWorker\" in navigator"
        );
        assertThat(index).doesNotContain("localStorage", "sessionStorage", "archivosJPG");

        assertThat(manifest).contains(
                "\"name\": \"Landing Tarjetas\"",
                "\"short_name\": \"Tarjetas\"",
                "\"start_url\": \"/\"",
                "\"display\": \"standalone\"",
                "\"src\": \"/icons/icon-192.svg\"",
                "\"src\": \"/icons/icon-512.svg\""
        );
        assertThat(manifest).doesNotContain("ticket", "ocr", "api", "auth", "archivosJPG");

        assertThat(worker).contains(
                "const SHELL_CACHE_NAME = \"privacy-safe-shell-v3\";",
                "const OFFLINE_DOCUMENT_URL = \"/offline.html\";",
                "const CACHEABLE_SHELL_URLS = new Set([",
                "const NETWORK_ONLY_PATTERNS = [",
                "self.addEventListener(\"fetch\"",
                "request.method !== \"GET\"",
                "url.pathname.startsWith(\"/api/\")",
                "url.pathname === \"/login\"",
                "pathname.includes(\"ticket\")",
                "pathname.includes(\"upload\")",
                "pathname.includes(\"private\")",
                "pathname.endsWith(\".pdf\")",
                "/css/styles.css?v=" + MOBILE_CONTROL_CSS_TOKEN,
                "/js/app.js?v=" + APP_SHELL_UI_TOKEN,
                "/js/navigation.js?v=" + APP_SHELL_UI_TOKEN,
                "/js/supermarket.js?v=" + APP_SHELL_UI_TOKEN,
                "cache.put(cacheKey, networkResponse.clone())"
        );
        assertThat(worker).doesNotContain("localStorage", "sessionStorage", "indexedDB", "archivosJPG");

        assertThat(offline).contains(
                "Sin conexión",
                "solo conserva una copia mínima del shell público",
                "Volver a intentar"
        );
        assertThat(offline).doesNotContain("ticket", "ocr", "api", "auth", "pdf", "archivosJPG");

        assertThat(icon192).contains("<svg", "Landing Tarjetas").doesNotContain("ticket", "ocr");
        assertThat(icon512).contains("<svg", "Landing Tarjetas").doesNotContain("ticket", "ocr");
    }

    @Test
    void unifiedExpensesTableUsesDashboardMonthDetailRows() throws IOException {
        String index = readStatic("index.html");
        String api = readStatic("js/api.js");
        String app = readStatic("js/app.js");
        String dashboard = readStatic("js/dashboard.js");
        String transactions = readStatic("js/transactions.js");

        assertThat(api).contains("dashboardMonths()", "dashboardMonthDetail(yearMonth)");
        assertThat(app).contains("api.dashboardMonths()", "api.dashboardMonthDetail(state.month)");
        assertThat(index).contains(
                "Tabla de gastos reales y proyectados",
                "Detalle unificado de gastos reales y proyecciones que impactan el mes seleccionado",
                "Pesos y USD se mantienen separados, sin conversión",
                "id=\"filters-summary\"",
                "id=\"filter-month\"",
                "id=\"filter-origin\"",
                "Resumen origen",
                "No hay gastos reales ni proyectados para este mes."
        );
        assertThat(index).doesNotContain("id=\"month-detail-table\"", "Detalle de proyección de cuotas");
        assertThat(dashboard).contains(
                "Falta ${target.title}",
                "Cargado",
                "Faltante",
                "Santander VISA",
                "Santander AMEX",
                "Naranja X"
        );
        assertThat(dashboard).contains("Datos reales de resúmenes confirmados. Las proyecciones de este mes se ocultan para evitar doble conteo.");
        assertThat(transactions).contains(
                "No hay gastos reales ni proyectados para este mes.",
                "No hay gastos que coincidan con los filtros seleccionados.",
                "originBadges",
                "Editar origen"
        );
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
                "No hay gastos reales ni proyectados para este mes.",
                "Aplicar filtros",
                "Limpiar filtros",
                "aria-live=\"polite\""
        );
        assertThat(index).doesNotContain("<html lang=\"en\">");
        assertThat(styles).contains("@media (max-width: 680px)", "overflow-x: auto", ".projection-row", ".actual-row");
        assertThat(styles).contains(
                "--surface-raised",
                "--table-min-width: 980px",
                "--income-table-min-width: 1180px",
                "min-width: var(--table-min-width)",
                "scroll-snap-type: inline proximity",
                ".metric-card.secondary",
                "grid-template-columns: repeat(auto-fit, minmax(min(100%, 18rem), 1fr))",
                "text-overflow: ellipsis",
                "white-space: nowrap",
                ".row-actions:not(.income-actions)",
                "repeat(auto-fit, minmax(13rem, 1fr))",
                "@media (max-width: 520px)"
        );
        assertThat(styles).contains("--border-strong");
        assertThat(styles).doesNotContain(".placeholder-panel .empty-state", ".wide-field");
        assertThat(index).doesNotContain("class=\"wide-field\"");
        assertThat(app).contains("No se pudieron cargar los datos del panel", "No se pudieron cargar los gastos", "setButtonBusy");
        assertThat(statements).contains("No se pudo completar la carga", "No se muestra texto del resumen ni contenido del PDF original", "parserDisplayLabel", "setButtonBusy");
        assertThat(transactions).contains(
                "resetTransactionFilters",
                "renderFilterSummary",
                "Mes: ${formatMonth(filters.month || month)}",
                "No hay gastos que coincidan con los filtros seleccionados."
        );
        assertThat(utils).contains("export function setButtonBusy", "aria-busy", "Intl.DateTimeFormat(\"es-AR\"", "Intl.NumberFormat(\"en\"");
        assertThat(utils).doesNotContain("Intl.DateTimeFormat(\"en\"");
    }

    @Test
    void summaryMetricValuesUseSelectorScopedOneLineReadabilityContract() throws IOException {
        String styles = readStatic("css/styles.css");

        assertThat(cssRule(styles, ".summary-block")).contains(
                "display: grid;",
                "grid-template-columns: repeat(auto-fit, minmax(min(100%, 18rem), 1fr));",
                "min-width: 0;"
        );
        assertThat(cssRule(styles, ".summary-block-result")).contains("grid-template-columns: repeat(auto-fit, minmax(min(100%, 20rem), 1fr));");
        assertThat(cssRule(styles, ".summary-block-income")).contains("grid-template-columns: repeat(auto-fit, minmax(min(100%, 20rem), 1fr));");
        assertThat(cssRule(styles, ".summary-block-expenses")).contains("grid-template-columns: repeat(auto-fit, minmax(min(100%, 18.5rem), 1fr));");
        assertThat(cssRule(styles, ".metric-card strong")).contains(
                "display: block;",
                "max-width: 100%;",
                "font-size: clamp(1.2rem, 2.35vw, 2.05rem);",
                "line-height: 1.1;",
                "overflow: hidden;",
                "text-overflow: ellipsis;",
                "white-space: nowrap;"
        );
    }

    @Test
    void mobileResponsiveSliceAddsReadableDashboardAndBaseTableCardContracts() throws IOException {
        String index = readStatic("index.html");
        String styles = readStatic("css/styles.css");
        String transactions = readStatic("js/transactions.js");
        String incomes = readStatic("js/incomes.js");
        String manualExpenses = readStatic("js/manual-expenses.js");
        String statements = readStatic("js/statements.js");
        String simulator = readStatic("js/simulator.js");
        String supermarket = readStatic("js/supermarket.js");

        assertCssRuleHasDeclarations(styles, ":root", Map.of("--tap-target-min", "44px"));
        assertCssRuleHasDeclarations(styles, ".month-tabs", Map.of("max-width", "100%", "min-width", "0"));
        assertCssRuleHasDeclarations(styles, ".month-tabs", Map.of("width", "100%", "margin-inline", "0", "padding-inline", "0"));
        assertThat(styles).doesNotContain(".primary-tabs");
        assertNoCssDeclaration(styles, List.of(".month-tabs"), "margin-inline", "-0.5rem");
        assertNoPageOverflowMask(styles);
        assertCssRuleHasDeclarations(styles, ".metric-card strong", Map.of(
                "overflow", "visible",
                "text-overflow", "clip",
                "white-space", "normal",
                "overflow-wrap", "anywhere"
        ));
        assertResponsiveCardTableMobileCssContract(styles);
        assertResponsiveExpensesTableUsabilityContract(index, styles, transactions);
        assertCssRuleHasDeclarations(styles, ".expenses-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".income-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".manual-expense-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".draft-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".simulator-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".super-items-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".super-category-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertResponsiveCardTableAdopterContract(index);
        assertThat(index).contains(
                "class=\"table-wrap expenses-table-wrap responsive-card-table\"",
                "class=\"table-wrap income-table-wrap responsive-card-table\"",
                "class=\"table-wrap manual-expense-table-wrap responsive-card-table\"",
                "class=\"table-wrap draft-table-wrap responsive-card-table responsive-edit-table\"",
                "class=\"table-wrap simulator-table-wrap responsive-card-table\"",
                "class=\"table-wrap super-items-table-wrap responsive-card-table\"",
                "class=\"table-wrap super-category-table-wrap responsive-card-table\""
        );
        assertDraftEditTableMobileCssContract(styles);
        assertDataLabels(transactions, List.of("Mes", "Fecha", "Origen", "Tarjeta / Medio", "Descripción", "Tipo", "Categoría", "Cuota", "Pesos", "USD", "Finalización", "Resumen origen", "Notas", "Monto", "Acciones"));
        assertDataLabels(incomes, List.of("Mes", "Descripción", "Tipo", "Monto", "Recurrente", "Aplica desde", "Aplica hasta", "Estado", "Notas", "Acciones"));
        assertDataLabels(manualExpenses, List.of("Mes", "Descripción", "Tipo", "Cuota", "Categoría", "Pesos", "USD", "Estado", "Notas", "Acciones"));
        assertDataLabels(statements, List.of("Fecha", "Descripción", "Tipo", "Categoría", "Cuota", "Total de cuotas", "Pesos", "USD", "Notas", "Acciones"));
        assertThat(statements).contains("aria-label=\"Fecha\"", "aria-label=\"Descripción\"", "aria-label=\"Pesos\"", "aria-label=\"USD\"");
        assertDataLabels(simulator, List.of("Mes", "Ingresos del mes", "Deuda/gastos actuales del mes", "Nueva cuota simulada", "Saldo actual sin simulación", "Saldo final con simulación"));
        assertDataLabels(supermarket, List.of("Estado", "Producto", "Categoría", "Configuración", "Presentación", "Precio ref.", "Stock", "Cantidad rápida", "Notas", "Acciones"));
    }

    @Test
    void incomeActionsUseSelectorScopedSpacingAndAlignmentContract() throws IOException {
        String styles = readStatic("css/styles.css");

        assertThat(cssRule(styles, ".income-actions")).contains(
                "display: grid;",
                "grid-template-columns: repeat(2, max-content);",
                "gap: 0.4rem;",
                "justify-content: end;",
                "align-items: center;",
                "min-width: 5.75rem;"
        );
        assertThat(cssRule(styles, ".icon-button")).contains(
                "display: inline-flex;",
                "width: 2.5rem;",
                "min-width: 2.5rem;",
                "height: 2.5rem;"
        );
        assertThat(cssRules(styles, ".income-actions")).anySatisfy(rule -> assertThat(rule).contains(
                "grid-template-columns: repeat(2, max-content);",
                "justify-content: end;",
                "min-width: 5.75rem;"
        ));
        assertThat(styles).contains(".modal-overlay", ".income-edit-form", ".modal-actions");
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
                "renderDraftStatementList(allStatements)",
                "onDraftConfirmed: async (statement) =>"
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

        assertThat(optionValues(index, "filter-card"))
                .containsAll(enumValues("CardBrand"))
                .contains("MANUAL", "CASH", "LOAN");
        assertThat(optionValues(index, "filter-type"))
                .containsAll(enumValues("TransactionType"))
                .contains("ONE_PAYMENT", "CASH", "LOAN");
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

    private static int countMatches(String source, String needle) {
        return source.split(Pattern.quote(needle), -1).length - 1;
    }

    private static String htmlSection(String html, String startId, String endMarker) {
        int start = html.indexOf("id=\"" + startId + "\"");
        assertThat(start).as("HTML section start id %s", startId).isNotNegative();
        int end = html.indexOf(endMarker, start);
        assertThat(end).as("HTML section end marker %s", endMarker).isNotNegative();
        return html.substring(start, end + endMarker.length());
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

    private static Map<String, Integer> supermarketLimits() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java"));

        return Map.of(
                "categoryName", javaIntConstant(source, "CATEGORY_NAME_MAX_LENGTH"),
                "itemName", javaIntConstant(source, "ITEM_NAME_MAX_LENGTH"),
                "itemNotes", javaIntConstant(source, "ITEM_NOTES_MAX_LENGTH"),
                "itemUnit", javaIntConstant(source, "ITEM_UNIT_MAX_LENGTH"),
                "presentationLabel", javaIntConstant(source, "ITEM_PRESENTATION_LABEL_MAX_LENGTH"),
                "priceSourceLabel", javaIntConstant(source, "ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH"),
                "barcodeCode", javaIntConstant(source, "BARCODE_CODE_MAX_LENGTH"),
                "barcodeFormat", javaIntConstant(source, "BARCODE_FORMAT_MAX_LENGTH")
        );
    }

    private static Integer javaIntConstant(String source, String constantName) {
        var matcher = Pattern.compile("public\\s+static\\s+final\\s+int\\s+" + constantName + "\\s*=\\s*(\\d+)\\s*;").matcher(source);
        assertThat(matcher.find()).as("Java supermarket limit constant %s", constantName).isTrue();
        return Integer.valueOf(matcher.group(1));
    }

    private static Map<String, Integer> supermarketFrontendLimits(String supermarket) {
        var limitsMatcher = Pattern.compile("SUPER_FIELD_LIMITS\\s*=\\s*Object\\.freeze\\(\\{(?<body>.*?)\\}\\)", Pattern.DOTALL).matcher(supermarket);
        assertThat(limitsMatcher.find()).as("SUPER_FIELD_LIMITS object").isTrue();

        return Pattern.compile("([A-Za-z][A-Za-z0-9]*):\\s*(\\d+)")
                .matcher(limitsMatcher.group("body"))
                .results()
                .collect(Collectors.toMap(result -> result.group(1), result -> Integer.valueOf(result.group(2))));
    }

    private static Set<String> supermarketDataLimitKeys(String index) {
        return Pattern.compile("data-super-limit=\\\"([^\\\"]+)\\\"")
                .matcher(index)
                .results()
                .map(result -> result.group(1))
                .collect(Collectors.toSet());
    }

    private static void assertCssRuleHasDeclarations(String css, String selector, Map<String, String> expectedDeclarations) {
        assertThat(cssRules(css, selector).stream().map(StaticUiContractTests::cssDeclarations))
                .as("CSS declarations for %s include %s", selector, expectedDeclarations)
                .anySatisfy(declarations -> assertThat(declarations).containsAllEntriesOf(expectedDeclarations));
    }

    private static void assertCssMediaRuleHasDeclarations(String css, String mediaHeader, String selector, Map<String, String> expectedDeclarations) {
        String mediaCss = String.join("\n", cssAtRuleBlocks(css, mediaHeader));
        assertThat(mediaCss).as("CSS media rule %s", mediaHeader).isNotEmpty();
        assertCssRuleHasDeclarations(mediaCss, selector, expectedDeclarations);
    }

    private static void assertResponsiveCardTableMobileCssContract(String css) {
        String mediaHeader = "@media (max-width: 680px)";
        String mediaCss = String.join("\n", cssAtRuleBlocks(css, mediaHeader));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table", Map.of(
                "position", "relative",
                "overflow-x", "auto",
                "overscroll-behavior-inline", "contain"
        ));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table table", Map.of("display", "table"));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table thead", Map.of("display", "table-header-group"));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table tbody", Map.of("display", "table-row-group"));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table tr", Map.of("display", "table-row"));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table td", Map.of("display", "table-cell"));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table tbody td:first-child", Map.of(
                "position", "sticky",
                "left", "0"
        ));
        assertThat(mediaCss).contains("content: \"Deslizá para ver más\";")
                .doesNotContain("content: attr(data-label);");
    }

    private static void assertDesktopSidebarCssContract(String css) {
        String mediaHeader = "@media (min-width: 961px)";
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".app-shell", Map.of(
                "grid-template-columns", "15.5rem minmax(0, 1fr)",
                "align-items", "start"
        ));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".app-shell-nav", Map.of(
                "grid-column", "1",
                "grid-row", "1 / span 100",
                "position", "sticky"
        ));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".app-shell-drawer", Map.of(
                "display", "grid",
                "grid-template-columns", "1fr"
        ));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".app-shell-drawer a[aria-current=\"page\"]", Map.of(
                "border-left", "2px solid var(--accent-strong)",
                "border-bottom", "0"
        ));
    }

    @Test
    void currencyPairsRenderOnlyPresentCurrencies() throws IOException {
        String utils = readStatic("js/utils.js");
        String statements = readStatic("js/statements.js");

        assertThat(utils).contains(
                "export function formatMoneyPair(totals)",
                "totals?.pesos",
                "totals?.usd",
                "Number.isFinite(Number(value)) && Number(value) !== 0",
                ".join(\" / \") : \"Sin importe\""
        );
        assertThat(statements).contains(
                "formatMoneyPair, formatPesos, formatUsd",
                "formatMoneyPair({ pesos: draft.totalPesos, usd: draft.totalUsd })"
        ).doesNotContain("formatPesos(draft.totalPesos)} / ${formatUsd(draft.totalUsd)");
    }

    private static void assertNoResponsiveCardCellDeclarationOutsideMedia(String css, String mediaHeader, String property, String value) {
        assertNoTargetedCssDeclarationOutsideMedia(css, mediaHeader, StaticUiContractTests::selectorTargetsResponsiveCardCell, property, value);
    }

    private static void assertResponsiveExpensesTableUsabilityContract(String index, String styles, String transactions) {
        String mobileMedia = "@media (max-width: 680px)";
        String desktopMedia = "@media (min-width: 681px)";
        assertThat(index).contains("class=\"topbar mobile-control-bar\"", "class=\"app-shell-nav mobile-navigation-bar\"", "<th class=\"mobile-amount-column\">Monto</th>\n                    <th>Acciones</th>");
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, "body", Map.of(
                "width", "100%",
                "min-width", "0",
                "margin", "0",
                "box-sizing", "border-box"
        ));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".mobile-control-bar", Map.of(
                "width", "100%",
                "max-width", "none",
                "min-width", "0",
                "margin-inline", "0",
                "box-sizing", "border-box",
                "grid-template-columns", "minmax(0, 1fr)"
        ));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".topbar-editorial", Map.of("display", "none"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".mobile-control-bar", Map.of(
                "position", "sticky",
                "top", "calc(var(--tap-target-min) + 0.5rem + env(safe-area-inset-top))",
                "margin-top", "calc(var(--tap-target-min) + 0.5rem + env(safe-area-inset-top))",
                "padding", "0.45rem max(0.45rem, env(safe-area-inset-right)) 0.45rem max(0.45rem, env(safe-area-inset-left))"
        ));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".mobile-navigation-bar", Map.of("position", "fixed", "top", "env(safe-area-inset-top)", "z-index", "10"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".mobile-control-bar .topbar-actions", Map.of(
                "width", "100%",
                "max-width", "none",
                "min-width", "0",
                "box-sizing", "border-box",
                "grid-template-columns", "minmax(0, 3fr) minmax(0, 1fr)"
        ));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".mobile-control-bar .topbar-actions > *", Map.of(
                "width", "100%",
                "max-width", "100%",
                "min-width", "0",
                "min-inline-size", "0"
        ));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".logout-form .secondary-button", Map.of("white-space", "nowrap"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".logout-label-full", Map.of("display", "none"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".logout-label-compact", Map.of("display", "inline"));
        assertCssMediaRuleHasDeclarations(styles, desktopMedia, ".expenses-table-wrap", Map.of("max-height", "min(42rem, calc(100vh - 12rem))", "overflow", "auto"));
        assertCssMediaRuleHasDeclarations(styles, desktopMedia, ".expenses-table-wrap thead th", Map.of("position", "sticky", "top", "0"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".expenses-table-wrap .mobile-amount", Map.of("display", "table-cell"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".expenses-table-wrap .transaction-action-menu", Map.of("display", "block"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, "#tab-expenses-table .expenses-table-wrap table", Map.of(
                "min-width", "29rem",
                "table-layout", "fixed",
                "border-collapse", "separate",
                "border-spacing", "0.35rem 0"
            ));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".expenses-table-wrap th", Map.of("padding", "0.4rem 0.3rem", "font-size", "0.75rem"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".expenses-table-wrap th:nth-child(2)", Map.of("width", "2.9rem", "white-space", "nowrap"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".expenses-table-wrap th:nth-child(8)", Map.of("width", "2.25rem", "white-space", "nowrap"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".expenses-table-wrap th:nth-child(14)", Map.of("width", "8rem", "text-overflow", "ellipsis"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".expenses-table-wrap th:nth-child(15)", Map.of("width", "3.35rem"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".expenses-table-wrap th:nth-child(5)", Map.of("width", "11rem", "overflow", "hidden", "text-overflow", "ellipsis", "white-space", "nowrap"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".transaction-date-mobile", Map.of("display", "inline"));
        assertCssMediaRuleHasDeclarations(styles, mobileMedia, ".transaction-date-desktop", Map.of("display", "none"));
        assertThat(transactions).contains("class=\"transaction-date-desktop\"", "class=\"transaction-date-mobile\"", "function compactDate(value)", "class=\"mobile-amount amount\" data-label=\"Monto\"", "class=\"transaction-action-menu\"", "summary aria-label=\"Acciones para");
    }

    private static void assertNoSupermarketGroupRowDeclarationOutsideMedia(String css, String mediaHeader, String property, String value) {
        assertNoTargetedCssDeclarationOutsideMedia(css, mediaHeader, StaticUiContractTests::selectorTargetsSupermarketGroupRow, property, value);
    }

    private static void assertNoTargetedCssDeclarationOutsideMedia(String css, String mediaHeader, java.util.function.Predicate<String> selectorPredicate, String property, String value) {
        Pattern.compile("(?s)([^{}]+)\\{([^{}]*)\\}")
                .matcher(cssWithoutAtRuleBlocks(css, mediaHeader))
                .results()
                .forEach(result -> {
                    Map<String, String> declarations = cssDeclarations(result.group(2).trim());
                    if (!value.equals(declarations.get(property))) {
                        return;
                    }
                    var matchingSelector = cssSelectors(result.group(1)).stream()
                            .filter(selectorPredicate)
                            .findFirst();
                    assertThat(matchingSelector)
                            .as("Unexpected %s: %s in %s", property, value, matchingSelector.orElse(result.group(1).trim()))
                            .isEmpty();
                });
    }

    private static void assertSimulatorResultsCellMobileOverflowContract(String css) {
        String mediaHeader = "@media (max-width: 680px)";
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table .simulator-results-table td", Map.of(
                "overflow", "visible",
                "text-overflow", "clip"
        ));
        assertNoSimulatorResultsCellDeclarationOutsideMedia(css, mediaHeader, "overflow", "visible");
        assertNoSimulatorResultsCellDeclarationOutsideMedia(css, mediaHeader, "text-overflow", "clip");
        assertThatThrownBy(() -> assertNoSimulatorResultsCellDeclarationOutsideMedia(css + "\n.simulator-results-table td { overflow: visible; }", mediaHeader, "overflow", "visible"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected overflow: visible in .simulator-results-table td");
        assertThatThrownBy(() -> assertNoSimulatorResultsCellDeclarationOutsideMedia(css + "\n.simulator-table-wrap [data-label] { text-overflow: clip; }", mediaHeader, "text-overflow", "clip"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected text-overflow: clip in .simulator-table-wrap [data-label]");
    }

    private static void assertDraftEditTableMobileCssContract(String css) {
        String mediaHeader = "@media (max-width: 680px)";
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table input", Map.of(
                "min-width", "0",
                "width", "100%"
        ));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table select", Map.of(
                "min-width", "0",
                "width", "100%"
        ));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table .row-actions", Map.of(
                "display", "grid",
                "grid-template-columns", "1fr",
                "width", "100%"
        ));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table .row-actions button", Map.of("width", "100%"));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table .row-actions .secondary-button", Map.of("width", "100%"));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table .row-actions .danger-button", Map.of("width", "100%"));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table [data-save-transaction]", Map.of("width", "100%"));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table [data-delete-transaction]", Map.of("width", "100%"));
        assertNoDraftEditTableDeclarationOutsideMedia(css, mediaHeader, "min-width", "0");
        assertNoDraftEditTableDeclarationOutsideMedia(css, mediaHeader, "width", "100%");
        assertNoDraftEditTableDeclarationOutsideMedia(css, mediaHeader, "grid-template-columns", "1fr");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\n.responsive-edit-table input { min-width: 0; }", mediaHeader, "min-width", "0"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected min-width: 0 in .responsive-edit-table input");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\n.draft-table-wrap .row-actions { grid-template-columns: 1fr; }", mediaHeader, "grid-template-columns", "1fr"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected grid-template-columns: 1fr in .draft-table-wrap .row-actions");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\n.draft-table-wrap button { width: 100%; }", mediaHeader, "width", "100%"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected width: 100% in .draft-table-wrap button");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\n.draft-table-wrap .secondary-button { width: 100%; }", mediaHeader, "width", "100%"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected width: 100% in .draft-table-wrap .secondary-button");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\n.draft-table-wrap .danger-button { width: 100%; }", mediaHeader, "width", "100%"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected width: 100% in .draft-table-wrap .danger-button");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\n.draft-table-wrap [data-save-transaction] { width: 100%; }", mediaHeader, "width", "100%"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected width: 100% in .draft-table-wrap [data-save-transaction]");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\n.draft-table-wrap [data-delete-transaction] { width: 100%; }", mediaHeader, "width", "100%"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected width: 100% in .draft-table-wrap [data-delete-transaction]");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\nbutton { width: 100%; }", mediaHeader, "width", "100%"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected width: 100% in button");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\n.secondary-button { width: 100%; }", mediaHeader, "width", "100%"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected width: 100% in .secondary-button");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\n.danger-button { width: 100%; }", mediaHeader, "width", "100%"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected width: 100% in .danger-button");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\n.row-actions button { width: 100%; }", mediaHeader, "width", "100%"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected width: 100% in .row-actions button");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\n[data-save-transaction] { width: 100%; }", mediaHeader, "width", "100%"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected width: 100% in [data-save-transaction]");
        assertThatThrownBy(() -> assertNoDraftEditTableDeclarationOutsideMedia(css + "\n[data-delete-transaction] { width: 100%; }", mediaHeader, "width", "100%"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected width: 100% in [data-delete-transaction]");
    }

    private static void assertNoDraftEditTableDeclarationOutsideMedia(String css, String mediaHeader, String property, String value) {
        assertNoTargetedCssDeclarationOutsideMedia(css, mediaHeader, StaticUiContractTests::selectorTargetsDraftEditTable, property, value);
    }

    private static void assertNoSimulatorResultsCellDeclarationOutsideMedia(String css, String mediaHeader, String property, String value) {
        Pattern.compile("(?s)([^{}]+)\\{([^{}]*)\\}")
                .matcher(cssWithoutAtRuleBlocks(css, mediaHeader))
                .results()
                .forEach(result -> {
                    Map<String, String> declarations = cssDeclarations(result.group(2).trim());
                    if (!value.equals(declarations.get(property))) {
                        return;
                    }
                    var matchingSelector = cssSelectors(result.group(1)).stream()
                            .filter(StaticUiContractTests::selectorTargetsSimulatorResultsCell)
                            .findFirst();
                    assertThat(matchingSelector)
                            .as("Unexpected %s: %s in %s", property, value, matchingSelector.orElse(result.group(1).trim()))
                            .isEmpty();
                });
    }

    private static void assertNoCssDeclarationOutsideMedia(String css, String mediaHeader, List<String> selectors, String property, String value) {
        assertNoCssDeclaration(cssWithoutAtRuleBlocks(css, mediaHeader), selectors, property, value);
    }

    private static void assertNoCssDeclaration(String css, List<String> selectors, String property, String value) {
        selectors.forEach(selector -> cssRules(css, selector).stream()
                .map(StaticUiContractTests::cssDeclarations)
                .forEach(declarations -> assertThat(declarations)
                        .as("Unexpected %s: %s in %s", property, value, selector)
                        .doesNotContainEntry(property, value)));
    }

    private static void assertNoPageOverflowMask(String css) {
        assertNoCssDeclaration(css, List.of("html", "body"), "overflow-x", "hidden");
        assertNoCssDeclaration(css, List.of("html", "body"), "overflow", "hidden");
    }

    private static void assertResponsiveCardTableAdopterContract(String html) {
        assertThat(responsiveCardTableAdopterIsSafe("<div class=\"table-wrap responsive-card-table\"><table><tbody><tr><td data-label=\"Amount\">ARS 1</td></tr></tbody></table></div>"))
                .isTrue();
        assertThat(responsiveCardTableAdopterIsSafe("<div class=\"table-wrap responsive-card-table\"><table><tbody><tr><td>ARS 1</td></tr></tbody></table></div>"))
                .isFalse();
        responsiveCardTableAdopters(html).forEach(adopter -> {
            if (!responsiveCardTableCells(adopter).isEmpty()) {
                assertThat(responsiveCardTableAdopterIsSafe(adopter))
                        .as("responsive-card-table adopters with static cells must provide data-label values")
                        .isTrue();
            }
        });
    }

    private static void assertDataLabels(String source, List<String> labels) {
        labels.forEach(label -> assertThat(source).contains("data-label=\"" + label + "\""));
    }

    private static boolean responsiveCardTableAdopterIsSafe(String html) {
        var cells = responsiveCardTableCells(html);
        return !cells.isEmpty() && cells.stream().allMatch(cell -> Pattern.compile("\\bdata-label=\"[^\"]+\"").matcher(cell.group(1)).find());
    }

    private static List<java.util.regex.MatchResult> responsiveCardTableCells(String html) {
        return Pattern.compile("<td\\b([^>]*)>").matcher(html).results().toList();
    }

    private static List<String> responsiveCardTableAdopters(String html) {
        return Pattern.compile("<div\\b(?=[^>]*class=\"[^\"]*\\bresponsive-card-table\\b[^\"]*\")[^>]*>[\\s\\S]*?</div>")
                .matcher(html)
                .results()
                .map(result -> result.group(0))
                .toList();
    }

    private static Map<String, String> cssDeclarations(String ruleBody) {
        return Arrays.stream(ruleBody.split(";"))
                .map(String::trim)
                .filter(declaration -> !declaration.isBlank())
                .map(declaration -> declaration.split(":", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim(), (first, second) -> second));
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

    private static String cssRule(String css, String selector) {
        List<String> rules = cssRules(css, selector);
        assertThat(rules).as("CSS rule for selector %s", selector).isNotEmpty();

        return rules.get(0);
    }

    private static List<String> cssRules(String css, String selector) {
        return Pattern.compile("(?s)([^{}]+)\\{([^{}]*)\\}")
                .matcher(css)
                .results()
                .filter(result -> selectorListContains(result.group(1), selector))
                .map(result -> result.group(2).trim())
                .toList();
    }

    private static List<String> cssAtRuleBlocks(String css, String atRuleHeader) {
        var blocks = new java.util.ArrayList<String>();
        int searchIndex = 0;
        while (searchIndex < css.length()) {
            int headerIndex = css.indexOf(atRuleHeader, searchIndex);
            if (headerIndex == -1) {
                break;
            }
            int openingBraceIndex = css.indexOf("{", headerIndex + atRuleHeader.length());
            assertThat(openingBraceIndex).as("Expected %s to have an opening brace", atRuleHeader).isNotEqualTo(-1);
            int closingBraceIndex = findMatchingClosingBrace(css, openingBraceIndex);
            blocks.add(css.substring(openingBraceIndex + 1, closingBraceIndex));
            searchIndex = closingBraceIndex + 1;
        }
        return blocks;
    }

    private static String cssWithoutAtRuleBlocks(String css, String atRuleHeader) {
        StringBuilder result = new StringBuilder();
        int searchIndex = 0;
        while (searchIndex < css.length()) {
            int headerIndex = css.indexOf(atRuleHeader, searchIndex);
            if (headerIndex == -1) {
                result.append(css.substring(searchIndex));
                break;
            }
            int openingBraceIndex = css.indexOf("{", headerIndex + atRuleHeader.length());
            assertThat(openingBraceIndex).as("Expected %s to have an opening brace", atRuleHeader).isNotEqualTo(-1);
            int closingBraceIndex = findMatchingClosingBrace(css, openingBraceIndex);
            result.append(css, searchIndex, headerIndex);
            searchIndex = closingBraceIndex + 1;
        }
        return result.toString();
    }

    private static int findMatchingClosingBrace(String css, int openingBraceIndex) {
        int depth = 0;
        for (int index = openingBraceIndex; index < css.length(); index++) {
            if (css.charAt(index) == '{') {
                depth++;
            } else if (css.charAt(index) == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        throw new AssertionError("Expected CSS block to have a matching closing brace");
    }

    private static boolean selectorListContains(String selectorList, String selector) {
        return cssSelectors(selectorList).contains(selector);
    }

    private static List<String> cssSelectors(String selectorList) {
        return Arrays.stream(selectorList.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }

    private static boolean selectorTargetsSimulatorResultsCell(String selector) {
        String normalizedSelector = selector.replaceAll("\\s+", " ").trim();
        boolean isSimulatorSelector = normalizedSelector.contains(".simulator-results-table")
                || normalizedSelector.contains("#simulation-results-table")
                || normalizedSelector.contains(".simulator-table-wrap");

        return isSimulatorSelector && (
                selectorContainsType(normalizedSelector, "td")
                        || normalizedSelector.contains(".simulation-month-cell")
                        || normalizedSelector.contains(".simulation-amount-cell")
                        || normalizedSelector.contains("[data-label")
                        || selectorContainsUniversalTarget(normalizedSelector)
        );
    }

    private static boolean selectorTargetsResponsiveCardCell(String selector) {
        String normalizedSelector = selector.replaceAll("\\s+", " ").trim();
        return normalizedSelector.contains(".responsive-card-table") && (
                selectorContainsType(normalizedSelector, "td")
                        || normalizedSelector.contains("[data-label")
                        || selectorContainsUniversalTarget(normalizedSelector)
        );
    }

    private static boolean selectorTargetsSupermarketGroupRow(String selector) {
        String normalizedSelector = selector.replaceAll("\\s+", " ").trim();
        return normalizedSelector.contains(".responsive-card-table")
                && normalizedSelector.contains(".super-category-group-row");
    }

    private static boolean selectorTargetsDraftEditTable(String selector) {
        String normalizedSelector = selector.replaceAll("\\s+", " ").trim();
        if (selectorTargetsBroadDraftActionControl(normalizedSelector)) {
            return true;
        }

        boolean isDraftEditSelector = normalizedSelector.contains(".responsive-edit-table")
                || normalizedSelector.contains(".draft-table-wrap")
                || normalizedSelector.contains("#draft-transactions-table");

        return isDraftEditSelector && (
                selectorContainsType(normalizedSelector, "input")
                        || selectorContainsType(normalizedSelector, "select")
                        || selectorContainsType(normalizedSelector, "button")
                        || normalizedSelector.contains(".row-actions")
                        || normalizedSelector.contains(".secondary-button")
                        || normalizedSelector.contains(".danger-button")
                        || normalizedSelector.contains("[data-save-transaction")
                        || normalizedSelector.contains("[data-delete-transaction")
                        || normalizedSelector.contains("[name=")
                        || selectorContainsUniversalTarget(normalizedSelector)
        );
    }

    private static boolean selectorTargetsBroadDraftActionControl(String selector) {
        return selectorIsBareDraftActionControl(selector)
                || selectorTargetsRootDraftActionControl(selector)
                || selectorTargetsRowActionControl(selector);
    }

    private static boolean selectorIsBareDraftActionControl(String selector) {
        return selectorMatchesTypePrefix(selector, "button")
                || selectorMatchesClassPrefix(selector, "secondary-button")
                || selectorMatchesClassPrefix(selector, "danger-button")
                || selectorMatchesAttributePrefix(selector, "data-save-transaction")
                || selectorMatchesAttributePrefix(selector, "data-delete-transaction");
    }

    private static boolean selectorTargetsRootDraftActionControl(String selector) {
        var rootMatcher = Pattern.compile("^(?:html|body|\\*)\\s+(.+)$").matcher(selector);
        return rootMatcher.matches() && selectorIsBareDraftActionControl(rootMatcher.group(1));
    }

    private static boolean selectorTargetsRowActionControl(String selector) {
        return selectorMatchesClassPrefix(selector, "row-actions")
                || selector.contains(".row-actions ") && selectorIncludesDraftActionControl(selector);
    }

    private static boolean selectorIncludesDraftActionControl(String selector) {
        return selectorContainsType(selector, "button")
                || selector.contains(".secondary-button")
                || selector.contains(".danger-button")
                || selector.contains("[data-save-transaction")
                || selector.contains("[data-delete-transaction");
    }

    private static boolean selectorMatchesTypePrefix(String selector, String type) {
        return selectorHasPrefixBoundary(selector, type);
    }

    private static boolean selectorMatchesClassPrefix(String selector, String className) {
        return selectorHasPrefixBoundary(selector, "." + className);
    }

    private static boolean selectorMatchesAttributePrefix(String selector, String attributeName) {
        if (!selector.startsWith("[" + attributeName)) {
            return false;
        }
        int closingBracketIndex = selector.indexOf("]");
        return closingBracketIndex != -1 && selectorHasPrefixBoundary(selector, selector.substring(0, closingBracketIndex + 1));
    }

    private static boolean selectorHasPrefixBoundary(String selector, String prefix) {
        if (selector.equals(prefix)) {
            return true;
        }
        if (!selector.startsWith(prefix)) {
            return false;
        }
        return List.of('.', '#', ':', '[', ' ', '>', '+', '~').contains(selector.charAt(prefix.length()));
    }

    private static boolean selectorContainsType(String selector, String type) {
        return Pattern.compile("(^|[\\s>+~])" + type + "(?=$|[.#:\\[\\s>+~])")
                .matcher(selector)
                .find();
    }

    private static boolean selectorContainsUniversalTarget(String selector) {
        return Pattern.compile("(^|[\\s>+~])\\*(?=$|[.#:\\[\\s>+~])")
                .matcher(selector)
                .find();
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
