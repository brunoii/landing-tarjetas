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
    private static final String STAGE1_API_TOKEN = "20260714-super-inventory-stage1-api";
    private static final String STAGE1_UI_TOKEN = "20260714-super-inventory-stage1-ui";
    private static final String STALE_API_TOKEN = "20260712-security-hardening";

    @Test
    void indexLinksExpectedStaticAssets() throws IOException {
        String index = readStatic("index.html");
        String login = readStatic("login.html");
        String app = readStatic("js/app.js");

        assertThat(index).contains("<link rel=\"stylesheet\" href=\"/css/styles.css?v=" + STAGE1_UI_TOKEN + "\">");
        assertThat(index).doesNotContain("<link rel=\"stylesheet\" href=\"/css/styles.css\">");
        assertThat(index).contains("<script type=\"module\" src=\"/js/app.js?v=" + STAGE1_UI_TOKEN + "\"></script>");
        assertThat(index).doesNotContain("/css/styles.css?v=20260711-security-login", "/js/app.js?v=20260711-security-login");
        assertThat(login).contains("<link rel=\"stylesheet\" href=\"/css/styles.css?v=" + FRESH_STATIC_TOKEN + "\">")
                .contains("/js/login.js?v=" + FRESH_STATIC_TOKEN)
                .doesNotContain("/css/styles.css?v=20260711-security-login", "/js/login.js?v=20260711-security-login");
        assertThat(app)
                .contains("./api.js?v=" + STAGE1_API_TOKEN, "./categories.js", "./dashboard.js?v=" + FRESH_STATIC_TOKEN, "./incomes.js?v=" + FRESH_STATIC_TOKEN, "./manual-expenses.js?v=" + FRESH_STATIC_TOKEN, "./navigation.js?v=" + FRESH_STATIC_TOKEN, "./simulator.js?v=" + FRESH_STATIC_TOKEN, "./statements.js?v=" + FRESH_STATIC_TOKEN, "./supermarket.js?v=" + STAGE1_UI_TOKEN, "./transactions.js?v=" + FRESH_STATIC_TOKEN, "./utils.js")
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
                "js/app.js", "./api.js?v=" + STAGE1_API_TOKEN,
                "js/supermarket.js", "./api.js?v=" + STAGE1_API_TOKEN,
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
                "Categorías",
                "Lista del super"
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
                "id=\"tab-categories\" data-tab-panel=\"categories\"",
                "id=\"tab-supermarket\" data-tab-panel=\"supermarket\""
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
                "label: \"Categorías\"",
                "label: \"Lista del super\""
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
        assertSimulatorResultsCellMobileOverflowContract(styles);
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
                "Cerrar sesión"
        );
        assertThat(login).contains(
                "id=\"login-form\" method=\"post\" action=\"/login\"",
                "name=\"username\"",
                "name=\"password\"",
                "/js/login.js?v=" + FRESH_STATIC_TOKEN
        );
        assertThat(app).contains("from \"./api.js?v=" + STAGE1_API_TOKEN + "\"")
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
                "id=\"super-items-table\"",
                "id=\"super-generate-list\"",
                "Generar lista",
                "id=\"super-uncheck-all\"",
                "Desmarcar todos",
                "Copiar",
                "Descargar TXT",
                "Compartir por WhatsApp",
                "No hay productos marcados para comprar.",
                "id=\"super-item-form\"",
                "Nombre del producto",
                "id=\"super-item-name\" type=\"text\" data-super-limit=\"itemName\"",
                "Unidad opcional",
                "id=\"super-item-unit\" type=\"text\" data-super-limit=\"itemUnit\"",
                "Objetivo habitual opcional",
                "id=\"super-item-objective\" type=\"number\" min=\"0.001\" step=\"0.001\" inputmode=\"decimal\"",
                "id=\"super-item-notes\" type=\"text\" data-super-limit=\"itemNotes\"",
                "id=\"super-category-form\"",
                "id=\"super-category-name\" type=\"text\" data-super-limit=\"categoryName\"",
                "id=\"super-category-toggle\"",
                "aria-expanded=\"false\"",
                "aria-controls=\"super-category-table-wrap\"",
                "id=\"super-category-table-wrap\" hidden",
                "class=\"super-category-table\"",
                "<th>Configuración</th>",
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
                "createSuperItem(payload)",
                "updateSuperItem(id, payload)",
                "updateSuperItemChecked(id, checked)",
                "`/api/super/items/${id}/checked`",
                "uncheckAllSuperItems()",
                "request(\"/api/super/items/uncheck-all\""
        );
        assertThat(app).contains("setupSupermarket({ apiClient: api })");
        assertThat(supermarket).contains(
                "generatedSuperListText",
                "groupSuperItems",
                "superItemConfigurationLabel",
                "SUPER_FIELD_LIMITS",
                "applySupermarketFieldLimits",
                "habitualObjective",
                "super-configuration-badge",
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
        assertThat(styles).contains(".supermarket-layout", ".super-item-form", ".super-items-table-wrap table", ".super-generated-list", ".super-category-table", ".super-category-actions", ".super-configuration-badge", ".super-configuration-badge.configured", ".super-configuration-badge.pending");
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
        assertDataLabels(supermarket, List.of("Estado", "Producto", "Categoría", "Configuración", "Notas", "Acciones"));
        assertThat(supermarket).doesNotContain(
                "amount", "price", "prices", "history",
                "stock", "movement", "movements",
                "barcode", "ocr", "suggested", "suggested-list", "suggestedList"
        );
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
        assertThat(styles).doesNotContain("--border-strong", ".placeholder-panel .empty-state", ".wide-field");
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
        assertCssRuleHasDeclarations(styles, ".primary-tabs", Map.of("max-width", "100%", "min-width", "0"));
        assertCssRuleHasDeclarations(styles, ".month-tabs", Map.of("max-width", "100%", "min-width", "0"));
        assertCssRuleHasDeclarations(styles, ".primary-tabs", Map.of("width", "100%", "margin-inline", "0", "padding-inline", "0"));
        assertCssRuleHasDeclarations(styles, ".month-tabs", Map.of("width", "100%", "margin-inline", "0", "padding-inline", "0"));
        assertNoCssDeclaration(styles, List.of(".primary-tabs", ".month-tabs"), "margin-inline", "-0.5rem");
        assertNoPageOverflowMask(styles);
        assertCssRuleHasDeclarations(styles, ".metric-card strong", Map.of(
                "overflow", "visible",
                "text-overflow", "clip",
                "white-space", "normal",
                "overflow-wrap", "anywhere"
        ));
        assertResponsiveCardTableMobileCssContract(styles);
        assertCssRuleHasDeclarations(styles, ".expenses-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".income-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".manual-expense-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".draft-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".simulator-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".super-items-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".super-category-table-wrap.responsive-card-table", Map.of("--responsive-card-label-width", "7.75rem"));
        assertCssRuleHasDeclarations(styles, ".responsive-card-table td.amount", Map.of(
                "text-align", "left",
                "white-space", "normal"
        ));
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
        assertDataLabels(transactions, List.of("Mes", "Fecha", "Origen", "Tarjeta / Medio", "Descripción", "Tipo", "Categoría", "Cuota", "Pesos", "USD", "Finalización", "Resumen origen", "Notas", "Acciones"));
        assertDataLabels(incomes, List.of("Mes", "Descripción", "Tipo", "Monto", "Recurrente", "Aplica desde", "Aplica hasta", "Estado", "Notas", "Acciones"));
        assertDataLabels(manualExpenses, List.of("Mes", "Descripción", "Tipo", "Cuota", "Categoría", "Pesos", "USD", "Estado", "Notas", "Acciones"));
        assertDataLabels(statements, List.of("Fecha", "Descripción", "Tipo", "Categoría", "Cuota", "Total de cuotas", "Pesos", "USD", "Notas", "Acciones"));
        assertThat(statements).contains("aria-label=\"Fecha\"", "aria-label=\"Descripción\"", "aria-label=\"Pesos\"", "aria-label=\"USD\"");
        assertDataLabels(simulator, List.of("Mes", "Ingresos del mes", "Deuda/gastos actuales del mes", "Nueva cuota simulada", "Saldo actual sin simulación", "Saldo final con simulación"));
        assertDataLabels(supermarket, List.of("Estado", "Producto", "Categoría", "Configuración", "Notas", "Acciones"));
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
                "itemUnit", javaIntConstant(source, "ITEM_UNIT_MAX_LENGTH")
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
        String cardCellColumns = "minmax(6.5rem, var(--responsive-card-label-width)) minmax(0, 1fr)";
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table td", Map.of(
                "grid-template-columns", cardCellColumns,
                "white-space", "normal",
                "overflow-wrap", "anywhere"
        ));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table td::before", Map.of("content", "attr(data-label)"));
        assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table .super-category-group-row", Map.of(
                "padding", "0",
                "overflow", "hidden"
        ));
        assertNoResponsiveCardCellDeclarationOutsideMedia(css, mediaHeader, "grid-template-columns", cardCellColumns);
        assertNoResponsiveCardCellDeclarationOutsideMedia(css, mediaHeader, "white-space", "normal");
        assertNoResponsiveCardCellDeclarationOutsideMedia(css, mediaHeader, "overflow-wrap", "anywhere");
        assertNoResponsiveCardCellDeclarationOutsideMedia(css, mediaHeader, "content", "attr(data-label)");
        assertNoSupermarketGroupRowDeclarationOutsideMedia(css, mediaHeader, "padding", "0");
        assertNoSupermarketGroupRowDeclarationOutsideMedia(css, mediaHeader, "overflow", "hidden");
        assertThatThrownBy(() -> assertNoResponsiveCardCellDeclarationOutsideMedia(css + "\n.responsive-card-table td { grid-template-columns: " + cardCellColumns + "; }", mediaHeader, "grid-template-columns", cardCellColumns))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected grid-template-columns: " + cardCellColumns + " in .responsive-card-table td");
        assertThatThrownBy(() -> assertNoResponsiveCardCellDeclarationOutsideMedia(css + "\n.super-items-table-wrap.responsive-card-table [data-label] { overflow-wrap: anywhere; }", mediaHeader, "overflow-wrap", "anywhere"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected overflow-wrap: anywhere in .super-items-table-wrap.responsive-card-table [data-label]");
        assertThatThrownBy(() -> assertNoResponsiveCardCellDeclarationOutsideMedia(css + "\nbody .responsive-card-table td::before { content: attr(data-label); }", mediaHeader, "content", "attr(data-label)"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected content: attr(data-label) in body .responsive-card-table td::before");
        assertThatThrownBy(() -> assertNoSupermarketGroupRowDeclarationOutsideMedia(css + "\n.responsive-card-table .super-category-group-row { padding: 0; }", mediaHeader, "padding", "0"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Unexpected padding: 0 in .responsive-card-table .super-category-group-row");
    }

    private static void assertNoResponsiveCardCellDeclarationOutsideMedia(String css, String mediaHeader, String property, String value) {
        assertNoTargetedCssDeclarationOutsideMedia(css, mediaHeader, StaticUiContractTests::selectorTargetsResponsiveCardCell, property, value);
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
