import assert from "node:assert/strict";
import { copyFile, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { pathToFileURL } from "node:url";
import vm from "node:vm";

const sourceRoot = path.resolve("src/main/resources/static/js");
const moduleRoot = path.join(tmpdir(), `landing-tarjetas-static-ui-${process.pid}`);
const staticModuleFileNames = ["api.js", "app.js", "categories.js", "dashboard.js", "incomes.js", "login.js", "manual-expenses.js", "navigation.js", "simulator.js", "statements.js", "supermarket.js", "transactions.js", "utils.js"];
const freshStaticToken = "20260713-pending-main";
const stage5ApiToken = "20260716-super-inventory-stage5-api";
const stage5UiToken = "20260716-super-inventory-stage5-ui";
const stage8UiToken = "20260716-super-inventory-stage8-price-source-ui";
const stage9UiToken = "20260718-super-inventory-stage9-price-observed-date-ui";
const stage10ApiToken = "20260718-super-inventory-stage10-price-observations-api";
const stage10UiToken = "20260718-super-inventory-stage10-price-observations-ui";
const stage11ApiToken = "20260718-super-inventory-stage11-price-sources-api";
const stage11UiToken = "20260718-super-inventory-stage11-price-sources-ui";
const stage15ApiToken = "20260725-super-inventory-stage15-ticket-ocr-ui-api";
const stage15UiToken = "20260725-super-inventory-stage15-ticket-ocr-ui";
const foundationApiToken = "20260727-mobile-scanner-ocr-pwa-foundation-api";
const foundationUiToken = "20260727-mobile-scanner-ocr-pwa-foundation-ui";
const appShellUiToken = "20260730-app-shell-domain-navigation-ui";
const staleApiToken = "20260712-security-hardening";

await rm(moduleRoot, { force: true, recursive: true });
await mkdir(moduleRoot, { recursive: true });
await writeFile(path.join(moduleRoot, "package.json"), JSON.stringify({ type: "module" }));

for (const fileName of staticModuleFileNames) {
    await copyFile(path.join(sourceRoot, fileName), path.join(moduleRoot, fileName));
}

try {
    const { api } = await import(pathToFileURL(path.join(moduleRoot, "api.js")));
    const { formatDate, formatMonth, setButtonBusy } = await import(pathToFileURL(path.join(moduleRoot, "utils.js")));
    const { matchesTargetCard, monthTabLabel, renderDashboard, visibleMonthTabs } = await import(pathToFileURL(path.join(moduleRoot, "dashboard.js")));
    const {
        incomePayloadFromValues,
        incomeProjectionLabel,
        incomeTypeLabel,
        loadIncomes,
        recurringLabel,
        setupIncomes,
        validateIncomePayload
    } = await import(pathToFileURL(path.join(moduleRoot, "incomes.js")));
    const {
        manualExpensePayloadFromValues,
        manualExpenseTypeLabel,
        manualProjectionLabel,
        renderManualExpenses,
        setManualExpenseCategories,
        setupManualExpenses,
        validateManualExpensePayload
    } = await import(pathToFileURL(path.join(moduleRoot, "manual-expenses.js")));
    const {
        affectedMonths,
        buildSimulationRows,
        calculateMonthlyInstallment,
        MAX_SIMULATOR_INSTALLMENTS,
        renderSimulationResults,
        runPurchaseSimulation,
        setSimulatorCategories,
        setupSimulator,
        simulationPayloadFromValues,
        validateSimulationPayload
    } = await import(pathToFileURL(path.join(moduleRoot, "simulator.js")));
    const {
        DEFAULT_ROUTE_HASH,
        DEFAULT_PRIMARY_TAB_ID,
        DEFAULT_SUPERMARKET_TAB_ID,
        parseHash,
        setupPrimaryTabs,
        syncPrimaryRouteFromLocation,
        setupTabs
    } = await import(pathToFileURL(path.join(moduleRoot, "navigation.js")));
    const {
        SUPER_FIELD_LIMITS,
        generatedSuperListText,
        getSuperBarcodeScannerAvailability,
        groupSuperItems,
        normalizeSuperBarcodeCode,
        renderSuperSuggestedItems,
        setupSupermarket,
        shouldAcceptSuperBarcodeScan,
        superBarcodePayloadFromValues,
        superBarcodeAliasLabel,
        superItemConfigurationLabel,
        superMovementQuantityLabel,
        superMovementSummary,
        superMovementTypeLabel,
        superItemQuickQuantityLabel,
        superItemPayloadFromValues,
        superItemStockLabel,
        validateSuperBarcodeLookup,
        validateSuperItemPayload,
        superItemCommercialPresentationLabel,
        superItemCommercialPresentationPriceHtml,
        superItemCommercialPresentationPriceLabel,
        superItemCommercialPresentationPriceObservedDateLabel,
        superItemCommercialPresentationPriceSourceLabel,
        superPriceSourcePayloadFromValues,
        validateSuperPriceSourcePayload,
        superPriceObservationPayloadFromValues,
        validateSuperPriceObservationPayload,
        superPriceObservationPresentationLabel,
        superPriceObservationRowHtml
    } = await import(pathToFileURL(path.join(moduleRoot, "supermarket.js")));
    const {
        draftTransactionCountLabel,
        missingTransactionControlsState,
        missingTransactionSubmitIntent,
        parserDisplayLabel
    } = await import(pathToFileURL(path.join(moduleRoot, "statements.js")));
    const {
        renderTransactions,
        resetTransactionFilters,
        setTransactionCategories,
        syncTransactionMonth,
        transactionFilters
    } = await import(pathToFileURL(path.join(moduleRoot, "transactions.js")));

    const indexHtml = await readFile(path.resolve("src/main/resources/static/index.html"), "utf8");
    const loginHtml = await readFile(path.resolve("src/main/resources/static/login.html"), "utf8");
    const manifestSource = await readFile(path.resolve("src/main/resources/static/manifest.webmanifest"), "utf8");
    const offlineHtml = await readFile(path.resolve("src/main/resources/static/offline.html"), "utf8");
    const serviceWorkerSource = await readFile(path.resolve("src/main/resources/static/service-worker.js"), "utf8");
    const icon192Source = await readFile(path.resolve("src/main/resources/static/icons/icon-192.svg"), "utf8");
    const icon512Source = await readFile(path.resolve("src/main/resources/static/icons/icon-512.svg"), "utf8");
    const stylesCss = await readFile(path.resolve("src/main/resources/static/css/styles.css"), "utf8");
    const appSource = await readFile(path.resolve("src/main/resources/static/js/app.js"), "utf8");
    const supermarketSource = await readFile(path.resolve("src/main/resources/static/js/supermarket.js"), "utf8");
    const loginSource = await readFile(path.resolve("src/main/resources/static/js/login.js"), "utf8");
    const statementsSource = await readFile(path.resolve("src/main/resources/static/js/statements.js"), "utf8");
    const supermarketLimitConstants = await readSupermarketLimitConstants();
    const expectedSuperFieldLimits = {
        categoryName: supermarketLimitConstants.CATEGORY_NAME_MAX_LENGTH,
        itemName: supermarketLimitConstants.ITEM_NAME_MAX_LENGTH,
        itemNotes: supermarketLimitConstants.ITEM_NOTES_MAX_LENGTH,
        itemUnit: supermarketLimitConstants.ITEM_UNIT_MAX_LENGTH,
        presentationLabel: supermarketLimitConstants.ITEM_PRESENTATION_LABEL_MAX_LENGTH,
        priceSourceLabel: supermarketLimitConstants.ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH,
        barcodeCode: supermarketLimitConstants.BARCODE_CODE_MAX_LENGTH,
        barcodeFormat: supermarketLimitConstants.BARCODE_FORMAT_MAX_LENGTH
    };
    assert.ok(indexHtml.indexOf('id="super-items-table"') < indexHtml.indexOf('id="super-category-form"'));
    assert.ok(indexHtml.indexOf('id="super-generated-list"') < indexHtml.indexOf('id="super-category-form"'));
    assert.deepEqual(SUPER_FIELD_LIMITS, expectedSuperFieldLimits);
    assert.match(indexHtml, /id="super-item-name"[^>]+data-super-limit="itemName"/);
    assert.match(indexHtml, /id="super-item-unit"[^>]+data-super-limit="itemUnit"/);
    assert.match(indexHtml, /id="super-item-presentation-label"[^>]+type="text"[^>]+data-super-limit="presentationLabel"/);
    assert.match(indexHtml, /id="super-item-presentation-quantity"[^>]+type="number"[^>]+min="0\.001"[^>]+step="0\.001"/);
    assert.match(indexHtml, /id="super-item-presentation-price-pesos"[^>]+type="number"[^>]+min="0\.01"[^>]+step="0\.01"[^>]+inputmode="decimal"/);
    assert.match(indexHtml, /id="super-item-presentation-price-source"[^>]+name="commercialPresentationPriceSourceId"/);
    assert.match(indexHtml, /id="super-item-presentation-price-source-label"[^>]+type="text"[^>]+name="commercialPresentationPriceSourceLabel"[^>]+data-super-limit="priceSourceLabel"/);
    assert.match(indexHtml, /id="super-item-presentation-price-observed-date"[^>]+type="date"[^>]+name="commercialPresentationPriceObservedDate"/);
    assert.match(indexHtml, /Presentación comercial opcional/);
    assert.match(indexHtml, /Cantidad por presentación opcional/);
    assert.match(indexHtml, /Precio ref\. opcional/);
    assert.match(indexHtml, /Fuente opcional del precio ref\./);
    assert.match(indexHtml, /Fuente reutilizable opcional del precio ref\./);
    assert.match(indexHtml, /Fuente manual opcional para el precio ref\./);
    assert.match(indexHtml, /Use una fuente reutilizable o una manual\. Nunca ambas\./);
    assert.match(indexHtml, /Fecha observada opcional del precio ref\./);
    assert.match(indexHtml, /Fecha manual opcional en formato YYYY-MM-DD\./);
    assert.match(indexHtml, /id="super-item-objective"[^>]+type="number"[^>]+min="0\.001"[^>]+step="0\.001"/);
    assert.match(indexHtml, /id="super-item-notes"[^>]+data-super-limit="itemNotes"/);
    assert.match(indexHtml, /id="super-category-name"[^>]+data-super-limit="categoryName"/);
    assert.doesNotMatch(indexHtml, /id="super-(?:item-name|item-notes|category-name)"[^>]+maxlength=/);
    assert.match(indexHtml, /id="super-category-toggle"[^>]+aria-expanded="false"[^>]+aria-controls="super-category-table-wrap"/);
    assert.match(indexHtml, /<table class="super-category-table">[\s\S]*<th>Categoría<\/th>[\s\S]*<tbody id="super-category-list">/);
    assert.match(indexHtml, /id="super-item-quick-quantity"[^>]+type="number"[^>]+min="0\.001"[^>]+step="0\.001"/);
    assert.match(indexHtml, /id="super-item-current-stock"[^>]+type="number"[^>]+min="0"[^>]+step="0\.001"/);
    assert.match(indexHtml, /id="super-barcode-form"/);
    assert.match(indexHtml, /id="super-barcode-code"[^>]+type="text"[^>]+data-super-limit="barcodeCode"/);
    assert.match(indexHtml, /id="super-barcode-format"[^>]+type="text"[^>]+data-super-limit="barcodeFormat"/);
    assert.match(indexHtml, /id="super-barcode-item"/);
    assert.match(indexHtml, /id="super-barcode-attach"/);
    assert.match(indexHtml, /id="super-barcode-remove"/);
    assert.match(indexHtml, /id="super-barcode-scan-start"[^>]+type="button"/);
    assert.match(indexHtml, /id="super-barcode-scan-stop"[^>]+type="button"[^>]+disabled/);
    assert.match(indexHtml, /id="super-barcode-scanner"[^>]+hidden/);
    assert.match(indexHtml, /id="super-barcode-scanner-preview"[^>]+playsinline[^>]+muted[^>]+hidden/);
    assert.match(indexHtml, /id="super-barcode-scanner-status"[^>]+role="status"[^>]+aria-live="polite"/);
    assert.match(indexHtml, /id="super-barcode-actions"[^>]+hidden/);
    assert.match(indexHtml, /id="super-barcode-purchase"[^>]+data-super-barcode-stock-action="purchase"[^>]+aria-controls="super-session-panel"[^>]+disabled/);
    assert.match(indexHtml, /id="super-barcode-consume"[^>]+data-super-barcode-stock-action="consume"[^>]+aria-controls="super-session-panel"[^>]+disabled/);
    assert.match(indexHtml, /class="supermarket-subtabs"[^>]+role="tablist"/);
    assert.match(indexHtml, /id="super-tab-list"[^>]+aria-controls="super-panel-list"[^>]+aria-selected="true"[^>]+data-super-tab-target="list"/);
    assert.match(indexHtml, /id="super-tab-barcode"[^>]+aria-controls="super-panel-barcode"[^>]+aria-selected="false"[^>]+data-super-tab-target="barcode"/);
    assert.match(indexHtml, /id="super-tab-tickets"[^>]+aria-controls="super-panel-tickets"[^>]+aria-selected="false"[^>]+data-super-tab-target="tickets"/);
    assert.match(indexHtml, /id="super-tab-categories"[^>]+aria-controls="super-panel-categories"[^>]+aria-selected="false"[^>]+data-super-tab-target="categories"/);
    assert.match(indexHtml, /id="super-panel-list"[^>]+role="tabpanel"[^>]+aria-labelledby="super-tab-list"[^>]+data-super-tab-panel="list"/);
    assert.match(indexHtml, /id="super-panel-barcode"[^>]+role="tabpanel"[^>]+aria-labelledby="super-tab-barcode"[^>]+data-super-tab-panel="barcode"[^>]+hidden/);
    assert.match(indexHtml, /id="super-panel-tickets"[^>]+role="tabpanel"[^>]+aria-labelledby="super-tab-tickets"[^>]+data-super-tab-panel="tickets"[^>]+hidden/);
    assert.match(indexHtml, /id="super-panel-categories"[^>]+role="tabpanel"[^>]+aria-labelledby="super-tab-categories"[^>]+data-super-tab-panel="categories"[^>]+hidden/);
    assert.doesNotMatch(indexHtml, /super-mobile-shell-nav|super-mobile-shell-link|supermarket-subtabs-contract/);
    assert.match(indexHtml, /Buscar código local/);
    assert.match(indexHtml, /Escanear código/);
    assert.match(indexHtml, /Detener escaneo/);
    assert.match(indexHtml, /La cámara es opcional\. Si no está disponible, ingresá el código manualmente\./);
    assert.match(indexHtml, /El escaneo solo resuelve el producto\. Enviá compra o consumo a la sesión para revisarlo antes de confirmar stock\./);
    assert.match(indexHtml, /Preparar compra/);
    assert.match(indexHtml, /Preparar consumo/);
    assert.match(indexHtml, /<th>Presentación<\/th>/);
    assert.match(indexHtml, /<th>Precio ref\.<\/th>/);
    assert.match(indexHtml, /<th>Stock<\/th>/);
    assert.match(indexHtml, /<th>Cantidad rápida<\/th>/);
    assert.match(indexHtml, /id="super-movement-modal"/);
    assert.match(indexHtml, /id="super-movement-form"/);
    assert.match(indexHtml, /id="super-movement-quantity"[^>]+type="number"[^>]+min="0\.001"[^>]+step="0\.001"/);
    assert.match(indexHtml, /id="super-movement-allow-negative"[^>]+type="checkbox"/);
    assert.match(indexHtml, /id="super-movement-history"/);
    assert.match(indexHtml, /id="super-movement-history-table"/);
    assert.match(indexHtml, /id="super-price-observation-form"/);
    assert.match(indexHtml, /id="super-price-observation-item"/);
    assert.match(indexHtml, /id="super-price-observation-price-pesos"[^>]+type="number"[^>]+min="0\.01"[^>]+step="0\.01"/);
    assert.match(indexHtml, /id="super-price-observation-price-source"/);
    assert.match(indexHtml, /id="super-price-observation-source-label"[^>]+type="text"[^>]+data-super-limit="priceSourceLabel"/);
    assert.match(indexHtml, /id="super-price-source-form"/);
    assert.match(indexHtml, /id="super-price-source-name"[^>]+type="text"[^>]+data-super-limit="priceSourceLabel"/);
    assert.match(indexHtml, /id="super-price-source-feedback"/);
    assert.match(indexHtml, /id="super-price-observation-observed-date"[^>]+type="date"/);
    assert.match(indexHtml, /id="super-price-observation-table"/);
    assert.match(indexHtml, /id="super-price-observation-context-summary"/);
    assert.match(indexHtml, /id="super-price-observation-global-reset"/);
    assert.match(indexHtml, /Registrar observación de precio/);
    assert.match(indexHtml, /Crear fuente de precio/);
    assert.match(indexHtml, /Cantidad/);
    assert.match(indexHtml, /Confirmar stock negativo/);
    assert.match(indexHtml, /<link rel="manifest" href="\/manifest\.webmanifest">/);
    assert.match(indexHtml, /navigator\.serviceWorker\.register\("\/service-worker\.js", \{ scope: "\/" \}\)/);
    assert.match(indexHtml, /window\.isSecureContext/);
    assert.match(indexHtml, /"serviceWorker" in navigator/);
    assert.doesNotMatch(indexHtml, /localStorage|sessionStorage|archivosJPG/);
    assert.match(manifestSource, /"name": "Landing Tarjetas"/);
    assert.match(manifestSource, /"short_name": "Tarjetas"/);
    assert.match(manifestSource, /"src": "\/icons\/icon-192\.svg"/);
    assert.match(manifestSource, /"src": "\/icons\/icon-512\.svg"/);
    assert.doesNotMatch(manifestSource, /ticket|ocr|api|auth|archivosJPG/i);
    assert.match(offlineHtml, /Sin conexión/);
    assert.match(offlineHtml, /solo conserva una copia mínima del shell público/);
    assert.match(offlineHtml, /Volver a intentar/);
    assert.doesNotMatch(offlineHtml, /ticket|ocr|api|auth|pdf|archivosJPG/i);
    assert.match(icon192Source, /<svg/);
    assert.match(icon512Source, /<svg/);
    assert.doesNotMatch(icon192Source, /ticket|ocr/i);
    assert.doesNotMatch(icon512Source, /ticket|ocr/i);
    const serviceWorkerRuntime = createServiceWorkerRuntime(serviceWorkerSource);
    await serviceWorkerRuntime.dispatchInstall();
    await serviceWorkerRuntime.dispatchActivate();
    const allowlistedResponse = await serviceWorkerRuntime.dispatchFetch("https://example.com/", {
        mode: "navigate",
        accept: "text/html",
        network: () => new Response("<html>shell</html>", { status: 200, headers: { "content-type": "text/html" } })
    });
    assert.equal(await allowlistedResponse.text(), "<html>shell</html>");
    assert.equal(serviceWorkerRuntime.wasCached("/"), true);
    const apiResponse = await serviceWorkerRuntime.dispatchFetch("https://example.com/api/super/items", {
        network: () => new Response("[]", { status: 200, headers: { "content-type": "application/json" } })
    });
    assert.equal(await apiResponse.text(), "[]");
    assert.equal(serviceWorkerRuntime.wasCached("/api/super/items"), false);
    const loginResponse = await serviceWorkerRuntime.dispatchFetch("https://example.com/login", {
        network: () => new Response("login", { status: 200, headers: { "content-type": "text/html" } })
    });
    assert.equal(await loginResponse.text(), "login");
    assert.equal(serviceWorkerRuntime.wasCached("/login"), false);
    const ticketResponse = await serviceWorkerRuntime.dispatchFetch("https://example.com/tickets/private-scan.pdf", {
        network: () => new Response("private", { status: 200, headers: { "content-type": "application/pdf" } })
    });
    assert.equal(await ticketResponse.text(), "private");
    assert.equal(serviceWorkerRuntime.wasCached("/tickets/private-scan.pdf"), false);
    const fallbackResponse = await serviceWorkerRuntime.dispatchFetch("https://example.com/some/offline/page", {
        mode: "navigate",
        accept: "text/html",
        network: () => {
            throw new Error("offline");
        }
    });
    assert.match(await fallbackResponse.text(), /Sin conexión/);
    assert.ok(indexHtml.includes(`/css/styles.css?v=${appShellUiToken}`));
    assert.ok(indexHtml.includes(`/js/app.js?v=${appShellUiToken}`));
    assert.ok(loginHtml.includes(`/css/styles.css?v=${freshStaticToken}`));
    assert.ok(loginHtml.includes(`/js/login.js?v=${freshStaticToken}`));
    assert.doesNotMatch(indexHtml, /\/css\/styles\.css\?v=20260711-security-login|\/js\/app\.js\?v=20260711-security-login/);
    assert.doesNotMatch(loginHtml, /\/css\/styles\.css\?v=20260711-security-login|\/js\/login\.js\?v=20260711-security-login/);
    assert.ok(appSource.includes(`from "./api.js?v=${foundationApiToken}"`));
    assert.doesNotMatch(indexHtml, /super-inventory-stage17-session-shell/);
    assert.doesNotMatch(appSource, new RegExp(staleApiToken));
    assert.doesNotMatch(appSource, /from "\.\/api\.js"/);
    assert.ok(loginSource.includes(`from "./api.js?v=${freshStaticToken}"`));
    assert.doesNotMatch(loginSource, new RegExp(staleApiToken));
    assert.doesNotMatch(loginSource, /from "\.\/api\.js"/);
    const expectedApiImports = new Map([
        ["app.js", `./api.js?v=${foundationApiToken}`],
        ["supermarket.js", `./api.js?v=${foundationApiToken}`],
        ["incomes.js", `./api.js?v=${freshStaticToken}`],
        ["login.js", `./api.js?v=${freshStaticToken}`],
        ["statements.js", `./api.js?v=${freshStaticToken}`]
    ]);
    const directApiImportPattern = /(?:from\s+|import\(\s*)["'](\.\/api\.js(?:\?[^"']*)?)["']/g;
    const apiImportOffenders = [];
    let apiImportCount = 0;
    for (const fileName of staticModuleFileNames) {
        const source = await readFile(path.join(sourceRoot, fileName), "utf8");
        for (const match of source.matchAll(directApiImportPattern)) {
            apiImportCount += 1;
            if (match[1] !== expectedApiImports.get(fileName)) {
                apiImportOffenders.push(`${fileName} -> ${match[1]}`);
            }
        }
    }
    assert.equal(apiImportCount, expectedApiImports.size);
    assert.deepEqual(apiImportOffenders, []);
    assert.deepEqual(apiImportOffenders.filter((offender) => offender.includes(staleApiToken)), []);
    for (const moduleName of ["dashboard", "incomes", "manual-expenses", "simulator", "statements", "transactions"]) {
        assert.ok(appSource.includes(`./${moduleName}.js?v=${freshStaticToken}`), `${moduleName}.js should preserve origin/main cache token`);
    }
    assert.ok(appSource.includes(`./navigation.js?v=${appShellUiToken}`));
    assert.ok(appSource.includes(`./supermarket.js?v=${appShellUiToken}`));
    assert.match(serviceWorkerSource, /privacy-safe-shell-v2/);
    assert.match(serviceWorkerSource, new RegExp(`/css/styles\\.css\\?v=${appShellUiToken}`));
    assert.match(serviceWorkerSource, new RegExp(`/js/app\\.js\\?v=${appShellUiToken}`));
    assert.match(serviceWorkerSource, new RegExp(`/js/navigation\\.js\\?v=${appShellUiToken}`));
    assert.match(serviceWorkerSource, new RegExp(`/js/supermarket\\.js\\?v=${appShellUiToken}`));
    assert.doesNotMatch(serviceWorkerSource, new RegExp(`/js/navigation\\.js\\?v=${foundationUiToken}`));
    assert.doesNotMatch(serviceWorkerSource, new RegExp(`/js/supermarket\\.js\\?v=${foundationUiToken}`));
    assert.match(indexHtml, /id="super-session-panel"/);
    assert.match(indexHtml, /id="super-session-title"/);
    assert.match(indexHtml, /Revisar sesión antes de confirmar/);
    assert.match(indexHtml, /id="super-session-summary"/);
    assert.match(indexHtml, /id="super-session-status"[^>]+role="status"[^>]+aria-live="polite"/);
    assert.match(indexHtml, /id="super-session-lines"/);
    assert.match(indexHtml, /id="super-session-empty"/);
    assert.match(indexHtml, /id="super-session-draft-form"/);
    assert.match(indexHtml, /id="super-session-item-name"[^>]+readonly/);
    assert.match(indexHtml, /id="super-session-draft-type"/);
    assert.match(indexHtml, /id="super-session-draft-quantity"[^>]+type="number"[^>]+min="0\.001"[^>]+step="0\.001"/);
    assert.match(indexHtml, /id="super-session-draft-notes"/);
    assert.match(indexHtml, /id="super-session-draft-allow-negative"[^>]+type="checkbox"/);
    assert.match(indexHtml, /id="super-session-cancel-draft"[^>]+disabled/);
    assert.match(indexHtml, /id="super-session-clear"[^>]+disabled/);
    assert.match(indexHtml, /id="super-session-save-draft"[^>]+disabled/);
    assert.match(indexHtml, /id="super-session-confirm"[^>]+disabled/);
    assert.match(indexHtml, /La sesión revisa borradores no mutantes\. El modal directo sigue disponible solo desde la tabla de productos\./);
    assert.match(indexHtml, /La sesión reúne borradores vinculados desde barcode para revisarlos antes de confirmar stock\./);
    assert.match(indexHtml, /No hay una sesión activa vinculada desde barcode local\./);
    assert.doesNotMatch(indexHtml, /próximo slice|todavía/);
    assert.ok(indexHtml.indexOf('id="super-items-table"') < indexHtml.indexOf('id="super-session-panel"'));
    assert.match(stylesCss, /\.super-session-panel/);
    assert.match(stylesCss, /\.super-session-summary/);
    assert.match(stylesCss, /\.supermarket-subtab-panel/);
    assertCssRuleHasDeclarations(stylesCss, ".supermarket-subtab-panel[hidden]", { display: "none" });
    assert.match(stylesCss, /\.super-session-table-wrap table/);
    assert.match(stylesCss, /\.super-session-draft-form/);
    assert.match(stylesCss, /\.super-session-actions/);
    assert.match(indexHtml, /aria-controls="super-session-panel"/);
    assert.match(indexHtml, /aria-controls="super-session-draft-form"/);
    assert.match(indexHtml, /aria-controls="super-session-lines"/);
    assert.match(supermarketSource, /activeSuperScanSession/);
    assert.match(supermarketSource, /queueSuperScanSessionResolvedItem/);
    assert.match(supermarketSource, /createSuperScanSessionDraft/);
    assert.match(supermarketSource, /updateSuperScanSessionDraft/);
    assert.match(supermarketSource, /deleteSuperScanSessionDraft/);
    assert.match(supermarketSource, /confirmSuperScanSession/);
    assert.doesNotMatch(appSource, /20260709-stage-7-polish|20260710-mobile-slice-2|20260711-mobile-simulator|20260711-mobile-draft-responsive|20260711-mobile-supermarket/);
    assert.doesNotMatch(appSource, /from "\.\/statements\.js";/);
    assert.match(indexHtml, /id="app-shell-menu-button"[^>]+aria-expanded="false"[^>]+aria-controls="app-shell-drawer"/);
    assert.match(indexHtml, /id="app-shell-drawer"[^>]+aria-label="Secciones de la aplicación"[^>]+hidden/);
    assert.match(indexHtml, /id="app-shell-drawer-close"[^>]+aria-label="Cerrar navegación"/);
    assert.match(indexHtml, /id="summary-home-title"[^>]+tabindex="-1"/);
    assert.match(indexHtml, /id="income-table-title"[^>]+tabindex="-1"/);
    assert.match(indexHtml, /id="income-upload-title"[^>]+tabindex="-1"/);
    assert.match(indexHtml, /id="simulator-title"[^>]+tabindex="-1"/);
    assert.match(indexHtml, /id="supermarket-title"[^>]+tabindex="-1"/);
    assert.match(indexHtml, /id="super-barcode-title"[^>]+tabindex="-1"/);
    assert.match(indexHtml, /id="super-ticket-ocr-title"[^>]+tabindex="-1"/);
    assert.match(indexHtml, /id="super-category-title"[^>]+tabindex="-1"/);
    assert.match(indexHtml, /app-shell-drawer-label">Resumen mensual</);
    assert.match(indexHtml, /href="#monthly\/summary"[^>]+data-shell-route="#monthly\/summary">Inicio</);
    assert.match(indexHtml, /href="#monthly\/expenses-upload"[^>]+data-shell-route="#monthly\/expenses-upload">Cargar gastos</);
    assert.match(indexHtml, /href="#monthly\/expenses-table"[^>]+data-shell-route="#monthly\/expenses-table">Tabla de gastos</);
    assert.match(indexHtml, /href="#monthly\/income-upload"[^>]+data-shell-route="#monthly\/income-upload">Cargar ingresos</);
    assert.match(indexHtml, /href="#monthly\/income-table"[^>]+data-shell-route="#monthly\/income-table">Tabla de ingresos</);
    assert.match(indexHtml, /href="#monthly\/simulator"[^>]+data-shell-route="#monthly\/simulator">Simulador</);
    assert.match(indexHtml, /href="#monthly\/categories"[^>]+data-shell-route="#monthly\/categories">Categorías financieras</);
    assert.match(indexHtml, /app-shell-drawer-label">Stock</);
    assert.match(indexHtml, /href="#stock\/list"[^>]+data-shell-route="#stock\/list">Lista</);
    assert.match(indexHtml, /href="#stock\/barcode"[^>]+data-shell-route="#stock\/barcode">Códigos de barra</);
    assert.match(indexHtml, /href="#stock\/tickets"[^>]+data-shell-route="#stock\/tickets">Tickets</);
    assert.match(indexHtml, /href="#stock\/categories"[^>]+data-shell-route="#stock\/categories">Categorías de stock</);
    assert.doesNotMatch(indexHtml, /id="primary-tab-summary"|id="primary-tab-supermarket"/);
    assert.match(appSource, /syncPrimaryRouteFromLocation\(\{ replace: true \}\)/);
    assert.match(appSource, /addEventListener\?\.\("hashchange"/);
    assertCssRuleHasDeclarations(stylesCss, ":root", { "--tap-target-min": "44px" });
    assertCssRuleHasDeclarations(stylesCss, ".app-shell-drawer[hidden]", { display: "none" });
    assert.doesNotMatch(stylesCss, /\.primary-tabs/);
    assertCssRuleHasDeclarations(stylesCss, ".month-tabs", { "max-width": "100%", "min-width": "0" });
    assertCssRuleHasDeclarations(stylesCss, ".month-tabs", { "width": "100%", "margin-inline": "0", "padding-inline": "0" });
    assertNoCssDeclaration(stylesCss, [".month-tabs"], "margin-inline", "-0.5rem");
    assertNoPageOverflowMask(stylesCss);
    assertCssRuleHasDeclarations(stylesCss, ".metric-card strong", {
        overflow: "visible",
        "text-overflow": "clip",
        "white-space": "normal",
        "overflow-wrap": "anywhere"
    });
    assertResponsiveCardTableMobileCssContract(stylesCss);
    assertCssRuleHasDeclarations(stylesCss, ".expenses-table-wrap.responsive-card-table", { "--responsive-card-label-width": "7.75rem" });
    assertCssRuleHasDeclarations(stylesCss, ".income-table-wrap.responsive-card-table", { "--responsive-card-label-width": "7.75rem" });
    assertCssRuleHasDeclarations(stylesCss, ".manual-expense-table-wrap.responsive-card-table", { "--responsive-card-label-width": "7.75rem" });
    assertCssRuleHasDeclarations(stylesCss, ".draft-table-wrap.responsive-card-table", { "--responsive-card-label-width": "7.75rem" });
    assertCssRuleHasDeclarations(stylesCss, ".simulator-table-wrap.responsive-card-table", { "--responsive-card-label-width": "7.75rem" });
    assertCssRuleHasDeclarations(stylesCss, ".super-items-table-wrap.responsive-card-table", { "--responsive-card-label-width": "7.75rem" });
    assertCssRuleHasDeclarations(stylesCss, ".super-category-table-wrap.responsive-card-table", { "--responsive-card-label-width": "7.75rem" });
    assertCssRuleHasDeclarations(stylesCss, ".super-generated-list", { "white-space": "pre-wrap", "overflow-wrap": "anywhere" });
    assertCssRuleHasDeclarations(stylesCss, ".super-configuration-badge", { display: "inline-flex", "white-space": "normal" });
    assertSimulatorResultsCellMobileOverflowContract(stylesCss);
    assertDraftEditTableMobileCssContract(stylesCss);
    assertCssMediaRuleHasDeclarations(stylesCss, "@media (max-width: 420px)", ".responsive-card-table td", { "grid-template-columns": "1fr" });
    assertNoCssDeclarationOutsideMedia(stylesCss, "@media (max-width: 420px)", [".responsive-card-table td"], "grid-template-columns", "1fr");
    assertResponsiveCardTableAdopterContract(indexHtml);
    assertResponsiveCardTableMarkup(indexHtml);
    assertResponsiveCardLabels(statementsSource, ["Fecha", "Descripción", "Tipo", "Categoría", "Cuota", "Total de cuotas", "Pesos", "USD", "Notas", "Acciones"]);
    assert.match(statementsSource, /aria-label="Fecha"/);
    assert.match(statementsSource, /aria-label="Descripción"/);
    assert.match(statementsSource, /aria-label="Pesos"/);
    assert.match(statementsSource, /aria-label="USD"/);

    const successButton = fakeButton("Guardar");
    setButtonBusy(successButton, true, "Guardando...");
    assert.equal(successButton.textContent, "Guardando...");
    assert.equal(successButton.disabled, true);
    assert.equal(successButton.attributes.get("aria-busy"), "true");
    setButtonBusy(successButton, false);
    assert.equal(successButton.textContent, "Guardar");
    assert.equal(successButton.disabled, false);
    assert.equal(successButton.attributes.has("aria-busy"), false);
    assert.equal("idleLabel" in successButton.dataset, false);

    const errorButton = fakeButton("Eliminar");
    setButtonBusy(errorButton, true, "Eliminando...");
    setButtonBusy(errorButton, false);
    assert.equal(errorButton.textContent, "Eliminar");
    assert.equal(errorButton.disabled, false);
    assert.equal(errorButton.attributes.has("aria-busy"), false);

    assert.equal(formatMonth("2026-07"), "jul 2026");
    assert.equal(formatMonth(""), "Sin mes");
    assert.equal(formatDate("2026-07-10"), "10 de jul de 2026");

    const previousLoginDocument = globalThis.document;
    const previousLoginWindow = globalThis.window;
    try {
        const loginDom = fakeLoginDom("?error");
        globalThis.document = loginDom.document;
        globalThis.window = loginDom.window;
        const { renderLoginFeedback } = await import(`${pathToFileURL(path.join(moduleRoot, "login.js")).href}?feedback-error`);
        renderLoginFeedback();
        assert.equal(loginDom.feedback.textContent, "Usuario o contraseña inválidos.");
        assert.equal(loginDom.feedback.classList.contains("error"), true);

        loginDom.window.location.search = "?logout";
        renderLoginFeedback();
        assert.equal(loginDom.feedback.textContent, "Sesión cerrada correctamente.");
        assert.equal(loginDom.feedback.classList.contains("error"), false);
    } finally {
        if (previousLoginDocument === undefined) {
            delete globalThis.document;
        } else {
            globalThis.document = previousLoginDocument;
        }
        if (previousLoginWindow === undefined) {
            delete globalThis.window;
        } else {
            globalThis.window = previousLoginWindow;
        }
    }

    assert.equal(incomeTypeLabel("SALARY"), "Sueldo");
    assert.equal(incomeTypeLabel("VARIABLE"), "Ingreso vario");
    assert.equal(recurringLabel(true), "Sí");
    assert.equal(recurringLabel(false), "No");
    assert.equal(incomeProjectionLabel({ projected: true }), "Proyectado");
    assert.equal(incomeProjectionLabel({ projected: false }), "Real");
    assert.deepEqual(incomePayloadFromValues({
        description: "  Sueldo principal ",
        incomeType: "SALARY",
        amountPesos: "2500.50",
        startMonth: "2026-07",
        endMonth: "",
        recurringMonthly: "true",
        notes: "  Neto estimado "
    }), {
        description: "Sueldo principal",
        incomeType: "SALARY",
        amountPesos: "2500.50",
        startMonth: "2026-07",
        endMonth: "",
        recurringMonthly: true,
        notes: "Neto estimado"
    });
    assert.equal(validateIncomePayload(incomePayloadFromValues({ description: " ", amountPesos: "10", startMonth: "2026-07" })), "La descripción del ingreso es obligatoria.");
    assert.equal(validateIncomePayload(incomePayloadFromValues({ description: "Honorarios", amountPesos: "0", startMonth: "2026-07" })), "El monto del ingreso debe ser mayor que cero.");
    assert.equal(validateIncomePayload(incomePayloadFromValues({ description: "Honorarios", amountPesos: "10", startMonth: "2026-07", endMonth: "2026-06" })), "El mes de aplicación hasta no puede ser anterior al mes de inicio.");
    assert.equal(validateIncomePayload(incomePayloadFromValues({ description: "Honorarios", amountPesos: "10", startMonth: "2026-07" })), "");

    const previousFetch = globalThis.fetch;
    const apiCalls = [];
    globalThis.fetch = async (requestPath, options = {}) => {
        apiCalls.push({ requestPath, options });
        return {
            ok: true,
            status: 200,
            async json() {
                return [];
            }
        };
    };
    try {
        await api.incomes();
        await api.incomes({ month: "2026-07" });
        await api.createIncome({ description: "Sueldo", amountPesos: "100", startMonth: "2026-07" });
        await api.updateIncome(12, { description: "Sueldo actualizado" });
        await api.updateIncomeFromMonth(12, "2026-09", { startMonth: "2026-09" });
        await api.deleteIncome(12);
        await api.manualExpenses({ month: "2026-07" });
        await api.createManualExpense({ description: "Préstamo", amountPesos: "100", startMonth: "2026-07" });
        await api.updateManualExpense(77, { description: "Préstamo actualizado" });
        await api.deleteManualExpense(77);
        await api.superCategories();
        await api.createSuperCategory({ name: "Almacén" });
        await api.updateSuperCategory(4, { name: "Verdulería" });
        await api.deleteSuperCategory(4);
        await api.superItems();
        await api.superSuggestedList();
        await api.createSuperItem({ name: "Leche", categoryId: 4, unit: "litro", habitualObjective: "2.000" });
        await api.updateSuperItem(9, { name: "Leche", categoryId: 4, checked: true });
        await api.adjustSuperItemStock(9, "4.000");
        await api.purchaseSuperItem(9, { quantity: "2.000", notes: "Reposición" });
        await api.consumeSuperItem(9, { quantity: "1.000", notes: "Cena", allowNegativeStock: false });
        await api.quickConsumeSuperItem(9, { allowNegativeStock: false });
        await api.superPriceSources();
        await api.createSuperPriceSource({ name: "Ticket proveedor" });
        await api.createSuperItemPriceObservation(9, { pricePesos: "1250.50", sourceLabel: "Ticket proveedor", observedDate: "2026-07-18" });
        await api.superPriceObservations({ itemId: 9, limit: 50 });
        await api.uploadSuperTicketOcrCandidates(new File(["ticket"], "ticket.png", { type: "image/png" }));
        await api.superStockMovements({ itemId: 9, limit: 25 });
        await api.activeSuperScanSession();
        await api.createActiveSuperScanSession();
        await api.queueSuperScanSessionResolvedItem(12, { itemId: 9, barcodeCode: "0075012345678" });
        await api.createSuperScanSessionDraft(12, { itemId: 9, type: "PURCHASE", quantity: "2.000", notes: "Reposición" });
        await api.updateSuperScanSessionDraft(12, 88, { type: "CONSUMPTION", quantity: "1.000", notes: "Cena", allowNegativeStock: true });
        await api.deleteSuperScanSessionDraft(12, 88);
        await api.confirmSuperScanSession(12);
        await api.lookupSuperItemBarcodeAlias("0075012345678");
        await api.attachSuperItemBarcodeAlias(9, { code: "0075012345678", format: "EAN_13" });
        await api.removeSuperItemBarcodeAlias(9, 44);
        await api.updateSuperItemChecked(9, true);
        await api.uncheckAllSuperItems();
        await api.deleteSuperItem(9);
        await api.summary("2026-09");
    } finally {
        if (previousFetch === undefined) {
            delete globalThis.fetch;
        } else {
            globalThis.fetch = previousFetch;
        }
    }
    assert.deepEqual(apiCalls.map((call) => [call.requestPath, call.options.method || "GET"]), [
        ["/api/incomes", "GET"],
        ["/api/incomes?month=2026-07", "GET"],
        ["/api/incomes", "POST"],
        ["/api/incomes/12", "PUT"],
        ["/api/incomes/12/from-month/2026-09", "PUT"],
        ["/api/incomes/12", "DELETE"],
        ["/api/manual-expenses?month=2026-07", "GET"],
        ["/api/manual-expenses", "POST"],
        ["/api/manual-expenses/77", "PUT"],
        ["/api/manual-expenses/77", "DELETE"],
        ["/api/super/categories", "GET"],
        ["/api/super/categories", "POST"],
        ["/api/super/categories/4", "PUT"],
        ["/api/super/categories/4", "DELETE"],
        ["/api/super/items", "GET"],
        ["/api/super/suggested-list", "GET"],
        ["/api/super/items", "POST"],
        ["/api/super/items/9", "PUT"],
        ["/api/super/items/9/stock-adjustments", "POST"],
        ["/api/super/items/9/purchases", "POST"],
        ["/api/super/items/9/consumptions", "POST"],
        ["/api/super/items/9/quick-consumptions", "POST"],
        ["/api/super/price-sources", "GET"],
        ["/api/super/price-sources", "POST"],
        ["/api/super/items/9/price-observations", "POST"],
        ["/api/super/price-observations?itemId=9&limit=50", "GET"],
        ["/api/super/ticket-ocr/candidates", "POST"],
        ["/api/super/movements?itemId=9&limit=25", "GET"],
        ["/api/super/scan-sessions/active", "GET"],
        ["/api/super/scan-sessions/active", "POST"],
        ["/api/super/scan-sessions/12/resolved-items", "POST"],
        ["/api/super/scan-sessions/12/drafts", "POST"],
        ["/api/super/scan-sessions/12/drafts/88", "PUT"],
        ["/api/super/scan-sessions/12/drafts/88", "DELETE"],
        ["/api/super/scan-sessions/12/confirm", "POST"],
        ["/api/super/barcode-aliases?code=0075012345678", "GET"],
        ["/api/super/items/9/barcode-aliases", "POST"],
        ["/api/super/items/9/barcode-aliases/44", "DELETE"],
        ["/api/super/items/9/checked", "PATCH"],
        ["/api/super/items/uncheck-all", "POST"],
        ["/api/super/items/9", "DELETE"],
        ["/api/dashboard/summary?month=2026-09", "GET"]
    ]);
    assert.deepEqual(JSON.parse(apiCalls[2].options.body), { description: "Sueldo", amountPesos: "100", startMonth: "2026-07" });
    assert.deepEqual(JSON.parse(apiCalls[7].options.body), { description: "Préstamo", amountPesos: "100", startMonth: "2026-07" });
    assert.deepEqual(JSON.parse(apiCalls[16].options.body), { name: "Leche", categoryId: 4, unit: "litro", habitualObjective: "2.000" });
    assert.deepEqual(JSON.parse(apiCalls[18].options.body), { currentStock: "4.000" });
    assert.deepEqual(JSON.parse(apiCalls[19].options.body), { quantity: "2.000", notes: "Reposición" });
    assert.deepEqual(JSON.parse(apiCalls[20].options.body), { quantity: "1.000", notes: "Cena", allowNegativeStock: false });
    assert.deepEqual(JSON.parse(apiCalls[21].options.body), { allowNegativeStock: false });
    assert.deepEqual(JSON.parse(apiCalls[23].options.body), { name: "Ticket proveedor" });
    assert.deepEqual(JSON.parse(apiCalls[24].options.body), { pricePesos: "1250.50", sourceLabel: "Ticket proveedor", observedDate: "2026-07-18" });
    assert.equal(apiCalls[26].options.body.get("file").name, "ticket.png");
    assert.deepEqual(JSON.parse(apiCalls[30].options.body), { itemId: 9, barcodeCode: "0075012345678" });
    assert.deepEqual(JSON.parse(apiCalls[31].options.body), { itemId: 9, type: "PURCHASE", quantity: "2.000", notes: "Reposición" });
    assert.deepEqual(JSON.parse(apiCalls[32].options.body), { type: "CONSUMPTION", quantity: "1.000", notes: "Cena", allowNegativeStock: true });
    assert.deepEqual(JSON.parse(apiCalls[36].options.body), { code: "0075012345678", format: "EAN_13" });
    assert.deepEqual(JSON.parse(apiCalls[38].options.body), { checked: true });

    const previousConflictFetch = globalThis.fetch;
    globalThis.fetch = async () => ({
        ok: false,
        status: 409,
        async json() {
            return {
                status: 409,
                error: "El consumo dejaría stock negativo. Confirme para continuar.",
                details: ["Reintente con allowNegativeStock=true para confirmar."],
                itemId: 9,
                itemName: "Leche",
                currentStock: "1.000",
                quantity: "2.000",
                resultingStock: "-1.000",
                movementType: "CONSUMPTION"
            };
        }
    });
    try {
        await assert.rejects(
            () => api.consumeSuperItem(9, { quantity: "2.000", allowNegativeStock: false }),
            (error) => {
                assert.equal(error.message, "Reintente con allowNegativeStock=true para confirmar.");
                assert.equal(error.status, 409);
                assert.equal(error.body.itemId, 9);
                assert.deepEqual(error.details, ["Reintente con allowNegativeStock=true para confirmar."]);
                assert.equal(error.resultingStock, "-1.000");
                assert.equal(error.movementType, "CONSUMPTION");
                return true;
            }
        );
    } finally {
        if (previousConflictFetch === undefined) {
            delete globalThis.fetch;
        } else {
            globalThis.fetch = previousConflictFetch;
        }
    }

    assert.equal(manualExpenseTypeLabel("ONE_PAYMENT"), "Un pago");
    assert.equal(manualExpenseTypeLabel("LOAN"), "Préstamo");
    assert.equal(manualProjectionLabel({ projected: true }), "Proyectado");
    assert.deepEqual(manualExpensePayloadFromValues({
        description: " Préstamo personal ",
        type: "LOAN",
        amountPesos: "1000.50",
        amountUsd: "12.25",
        startMonth: "2026-07",
        totalInstallments: "6",
        currentInstallment: "2",
        categoryId: "4",
        notes: " Banco "
    }), {
        description: "Préstamo personal",
        type: "LOAN",
        amountPesos: "1000.50",
        amountUsd: "12.25",
        startMonth: "2026-07",
        totalInstallments: 6,
        currentInstallment: 2,
        categoryId: 4,
        notes: "Banco"
    });
    assert.equal(validateManualExpensePayload(manualExpensePayloadFromValues({ description: " ", amountPesos: "10", startMonth: "2026-07" })), "La descripción del gasto manual es obligatoria.");
    assert.equal(validateManualExpensePayload(manualExpensePayloadFromValues({ description: "Taxi", amountPesos: "0", startMonth: "2026-07" })), "El monto en pesos debe ser mayor que cero.");
    assert.equal(validateManualExpensePayload(manualExpensePayloadFromValues({ description: "Préstamo", type: "LOAN", amountPesos: "100", startMonth: "2026-07" })), "La cantidad de cuotas es obligatoria para cuotas y préstamos.");
    assert.equal(validateManualExpensePayload(manualExpensePayloadFromValues({ description: "Cuotas", type: "INSTALLMENT", amountPesos: "100", startMonth: "2026-07", totalInstallments: "3", currentInstallment: "4" })), "La cuota actual no puede superar el total de cuotas.");
    assert.equal(validateManualExpensePayload(manualExpensePayloadFromValues({ description: "Taxi", amountPesos: "100", startMonth: "2026-07" })), "");

    assert.deepEqual(superItemPayloadFromValues({
        name: "  Leche ",
        categoryId: "4",
        checked: "true",
        notes: "  Sin lactosa ",
        unit: "  litro ",
        habitualObjective: "2.500",
        quickQuantity: "1.000",
        currentStock: "9.000"
    }), {
        name: "Leche",
        categoryId: 4,
        checked: true,
        notes: "Sin lactosa",
        unit: "litro",
        habitualObjective: "2.500",
        quickQuantity: "1.000"
    });
    assert.deepEqual(superItemPayloadFromValues({
        name: "  Arroz ",
        categoryId: "4",
        checked: true,
        notes: "  Doble carolina ",
        unit: " kg ",
        commercialPresentationLabel: " Pack x 6 ",
        commercialPresentationQuantity: "6.000",
        commercialPresentationPricePesos: " 1250.50 ",
        commercialPresentationPriceSourceId: "7",
        commercialPresentationPriceSourceLabel: " Ticket proveedor ignorado ",
        commercialPresentationPriceObservedDate: " 2026-07-18 "
    }), {
        name: "Arroz",
        categoryId: 4,
        checked: true,
        notes: "Doble carolina",
        unit: "kg",
        commercialPresentationLabel: "Pack x 6",
        commercialPresentationQuantity: "6.000",
        commercialPresentationPricePesos: "1250.50",
        commercialPresentationPriceSourceId: 7,
        commercialPresentationPriceObservedDate: "2026-07-18"
    });
    assert.deepEqual(superItemPayloadFromValues({
        name: "  Azúcar ",
        categoryId: "4",
        commercialPresentationLabel: " Bolsa 1kg ",
        commercialPresentationPricePesos: " 999.99 ",
        commercialPresentationPriceSourceId: "",
        commercialPresentationPriceSourceLabel: " Lista mayorista "
    }), {
        name: "Azúcar",
        categoryId: 4,
        checked: false,
        notes: "",
        commercialPresentationLabel: "Bolsa 1kg",
        commercialPresentationPricePesos: "999.99",
        commercialPresentationPriceSourceLabel: "Lista mayorista"
    });
    assert.deepEqual(superItemPayloadFromValues({
        name: "  Leche ",
        categoryId: "4",
        commercialPresentationLabel: " Botella 1L ",
        commercialPresentationPricePesos: " 900 ",
        commercialPresentationPriceSourceLabel: "   "
    }), {
        name: "Leche",
        categoryId: 4,
        checked: false,
        notes: "",
        commercialPresentationLabel: "Botella 1L",
        commercialPresentationPricePesos: "900"
    });
    const orphanSourcePayload = superItemPayloadFromValues({
        name: "  Leche ",
        categoryId: "4",
        commercialPresentationLabel: " ",
        commercialPresentationPricePesos: " ",
        commercialPresentationPriceSourceLabel: " Ticket proveedor "
    });
    assert.deepEqual(orphanSourcePayload, {
        name: "Leche",
        categoryId: 4,
        checked: false,
        notes: "",
        commercialPresentationPriceSourceLabel: "Ticket proveedor"
    });
    assert.equal(validateSuperItemPayload(orphanSourcePayload), "La fuente del precio requiere un precio de referencia.");
    const sourceWithoutPresentationPayload = superItemPayloadFromValues({
        name: "  Leche ",
        categoryId: "4",
        commercialPresentationLabel: " ",
        commercialPresentationPricePesos: " 900 ",
        commercialPresentationPriceSourceLabel: " Ticket proveedor "
    });
    assert.deepEqual(sourceWithoutPresentationPayload, {
        name: "Leche",
        categoryId: 4,
        checked: false,
        notes: "",
        commercialPresentationPricePesos: "900",
        commercialPresentationPriceSourceLabel: "Ticket proveedor"
    });
    assert.equal(validateSuperItemPayload(sourceWithoutPresentationPayload), "La fuente del precio requiere una presentación comercial.");
    const orphanObservedDatePayload = superItemPayloadFromValues({
        name: "  Leche ",
        categoryId: "4",
        commercialPresentationLabel: " ",
        commercialPresentationPricePesos: " ",
        commercialPresentationPriceObservedDate: "2026-07-18"
    });
    assert.deepEqual(orphanObservedDatePayload, {
        name: "Leche",
        categoryId: 4,
        checked: false,
        notes: "",
        commercialPresentationPriceObservedDate: "2026-07-18"
    });
    assert.equal(validateSuperItemPayload(orphanObservedDatePayload), "La fecha observada del precio requiere un precio de referencia.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4", commercialPresentationPricePesos: "1250.50", commercialPresentationPriceObservedDate: "2026-07-18" })), "La fecha observada del precio requiere una presentación comercial.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4", commercialPresentationLabel: "Pack", commercialPresentationPricePesos: "1250.50", commercialPresentationPriceObservedDate: "9999-12-31" })), "La fecha observada del precio no puede ser futura.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4", commercialPresentationLabel: "Pack", commercialPresentationPricePesos: "1250.50", commercialPresentationPriceObservedDate: "18/07/2026" })), "La fecha observada del precio debe usar formato YYYY-MM-DD.");
    assert.equal(validateSuperItemPayload({ name: "Leche", categoryId: 4, commercialPresentationLabel: "Pack", commercialPresentationPricePesos: "1250.50", commercialPresentationPriceSourceId: 7, commercialPresentationPriceSourceLabel: "Ticket proveedor" }), "Elegí una fuente reutilizable o una manual, no ambas.");
    assert.equal(validateSuperItemPayload({ name: "Leche", categoryId: 4, commercialPresentationLabel: "Pack", commercialPresentationPriceSourceId: 7 }), "La fuente del precio requiere un precio de referencia.");
    assert.equal(validateSuperItemPayload({ name: "Leche", categoryId: 4, commercialPresentationPricePesos: "1250.50", commercialPresentationPriceSourceId: 7 }), "La fuente del precio requiere una presentación comercial.");
    assert.deepEqual(superItemPayloadFromValues({
        name: "  Leche ",
        categoryId: "4",
        checked: false,
        notes: "  Sin lactosa ",
        unit: " ",
        habitualObjective: "",
        quickQuantity: ""
    }), {
        name: "Leche",
        categoryId: 4,
        checked: false,
        notes: "Sin lactosa"
    });
    assert.deepEqual(superItemPayloadFromValues({
        name: "  Leche ",
        categoryId: "4",
        commercialPresentationLabel: " ",
        commercialPresentationQuantity: ""
    }), {
        name: "Leche",
        categoryId: 4,
        checked: false,
        notes: ""
    });
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: " ", categoryId: "4" })), "El nombre del producto es obligatorio.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "" })), "La categoría del producto es obligatoria.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4", habitualObjective: "0" })), "El objetivo habitual debe ser mayor que cero.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4", habitualObjective: "-1" })), "El objetivo habitual debe ser mayor que cero.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4", quickQuantity: "0" })), "La cantidad rápida debe ser mayor que cero.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4", unit: "unidad", commercialPresentationLabel: "Pack", commercialPresentationQuantity: "0" })), "La cantidad de presentación debe ser mayor que cero.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4", commercialPresentationLabel: "Pack", commercialPresentationQuantity: "6" })), "La cantidad de presentación requiere unidad de inventario.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4", unit: "unidad", commercialPresentationQuantity: "6" })), "La cantidad de presentación requiere una presentación comercial.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4", commercialPresentationLabel: "Pack", commercialPresentationPricePesos: "0" })), "El precio de referencia debe ser mayor que cero.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4", commercialPresentationLabel: "Pack", commercialPresentationPricePesos: "-1" })), "El precio de referencia debe ser mayor que cero.");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4", commercialPresentationPricePesos: "1250.50" })), "El precio de referencia requiere una presentación comercial.");
    assert.equal(validateSuperItemPayload({ name: "Leche", categoryId: 4, commercialPresentationLabel: "Pack", commercialPresentationPriceSourceLabel: "Ticket proveedor" }), "La fuente del precio requiere un precio de referencia.");
    assert.equal(validateSuperItemPayload({ name: "Leche", categoryId: 4, commercialPresentationPricePesos: "1250.50", commercialPresentationPriceSourceLabel: "Ticket proveedor" }), "La fuente del precio requiere una presentación comercial.");
    assert.equal(validateSuperItemPayload({ name: "Leche", categoryId: 4, commercialPresentationLabel: "Pack", commercialPresentationPricePesos: "1250.50" }), "");
    assert.equal(validateSuperItemPayload({ name: "Leche", categoryId: 4, commercialPresentationLabel: "Pack", commercialPresentationPricePesos: "1250.50", commercialPresentationPriceSourceId: 7 }), "");
    assert.equal(validateSuperItemPayload({ name: "Leche", categoryId: 4, commercialPresentationLabel: "Pack", commercialPresentationPricePesos: "1250.50", commercialPresentationPriceObservedDate: "2026-07-18" }), "");
    assert.equal(validateSuperItemPayload(superItemPayloadFromValues({ name: "Leche", categoryId: "4" })), "");
    assert.deepEqual(superPriceSourcePayloadFromValues({ name: ` ${"Ticket proveedor largo ".repeat(8)} ` }), {
        name: `${"Ticket proveedor largo ".repeat(8)}`.trim().slice(0, SUPER_FIELD_LIMITS.priceSourceLabel)
    });
    assert.equal(validateSuperPriceSourcePayload(superPriceSourcePayloadFromValues({ name: " " })), "El nombre de la fuente de precio es obligatorio.");
    assert.equal(validateSuperPriceSourcePayload(superPriceSourcePayloadFromValues({ name: "Ticket proveedor" })), "");
    assert.deepEqual(superPriceObservationPayloadFromValues({ pricePesos: " 1250.50 ", priceSourceId: "7", sourceLabel: " Texto libre ignorado ", observedDate: " 2026-07-18 " }), {
        pricePesos: "1250.50",
        priceSourceId: 7,
        observedDate: "2026-07-18"
    });
    assert.deepEqual(superPriceObservationPayloadFromValues({ pricePesos: " 1250.50 ", priceSourceId: "", sourceLabel: ` ${"Ticket proveedor largo ".repeat(8)} `, observedDate: " 2026-07-18 " }), {
        pricePesos: "1250.50",
        sourceLabel: `${"Ticket proveedor largo ".repeat(8)}`.trim().slice(0, SUPER_FIELD_LIMITS.priceSourceLabel),
        observedDate: "2026-07-18"
    });
    assert.deepEqual(superPriceObservationPayloadFromValues({ pricePesos: " 900 ", sourceLabel: "   ", observedDate: " " }), { pricePesos: "900" });
    assert.equal(validateSuperPriceObservationPayload(superPriceObservationPayloadFromValues({ pricePesos: "0" })), "El precio observado debe ser mayor que cero.");
    assert.equal(validateSuperPriceObservationPayload(superPriceObservationPayloadFromValues({ pricePesos: "-1" })), "El precio observado debe ser mayor que cero.");
    assert.equal(validateSuperPriceObservationPayload(superPriceObservationPayloadFromValues({ pricePesos: "1250.50", observedDate: "18/07/2026" })), "La fecha observada de la observación debe usar formato YYYY-MM-DD.");
    assert.equal(validateSuperPriceObservationPayload(superPriceObservationPayloadFromValues({ pricePesos: "1250.50", observedDate: "9999-12-31" })), "La fecha observada de la observación no puede ser futura.");
    assert.equal(validateSuperPriceObservationPayload(superPriceObservationPayloadFromValues({ pricePesos: "1250.50", observedDate: "2026-07-18" })), "");
    assert.equal(superPriceObservationPresentationLabel({ presentationLabelSnapshot: "Pack x 6", presentationQuantitySnapshot: "6.000" }), "Pack x 6 · 6.000");
    assert.equal(superPriceObservationPresentationLabel({ presentationLabelSnapshot: "Botella 1L", presentationQuantitySnapshot: null }), "Botella 1L");
    const observationRowHtml = superPriceObservationRowHtml({ itemName: "Arroz", presentationLabelSnapshot: "Pack x 6", presentationQuantitySnapshot: "6.000", pricePesos: "1250.50", sourceLabel: "Ticket proveedor", observedDate: "2026-07-18", createdAt: "2026-07-18T12:30:00" });
    assert.match(observationRowHtml, /Arroz/);
    assert.match(observationRowHtml, /Pack x 6 · 6\.000/);
    assert.match(observationRowHtml, /ARS\s1,250\.50/);
    assert.match(observationRowHtml, /Ticket proveedor/);
    assert.match(observationRowHtml, /2026-07-18/);
    assert.match(observationRowHtml, /2026-07-18 12:30/);
    assert.equal(normalizeSuperBarcodeCode("  0075012345678  "), "0075012345678");
    assert.equal(normalizeSuperBarcodeCode(75012345678), "75012345678");
    assert.deepEqual(superBarcodePayloadFromValues({ code: "  0075012345678  ", format: " EAN_13 " }), { code: "0075012345678", format: "EAN_13" });
    assert.deepEqual(superBarcodePayloadFromValues({ code: "00042", format: " " }), { code: "00042" });
    assert.deepEqual(getSuperBarcodeScannerAvailability({ isSecureContext: true, BarcodeDetector: class BarcodeDetector {}, navigator: { mediaDevices: { getUserMedia() {} } } }), {
        secureContext: true,
        hasBarcodeDetector: true,
        hasMediaDevices: true,
        supported: true
    });
    assert.equal(getSuperBarcodeScannerAvailability({ isSecureContext: false, BarcodeDetector: class BarcodeDetector {}, navigator: { mediaDevices: { getUserMedia() {} } } }).supported, false);
    assert.equal(shouldAcceptSuperBarcodeScan({ nextCode: "0075012345678", lastCode: "", lastAcceptedAt: 0, now: 1000 }), true);
    assert.equal(shouldAcceptSuperBarcodeScan({ nextCode: "0075012345678", lastCode: "0075012345678", lastAcceptedAt: 1000, now: 2500 }), false);
    assert.equal(shouldAcceptSuperBarcodeScan({ nextCode: "0075012345678", lastCode: "0075012345678", lastAcceptedAt: 1000, now: 3501 }), true);
    assert.equal(shouldAcceptSuperBarcodeScan({ nextCode: "0075012345678", lastCode: "0000000000001", lastAcceptedAt: 1000, now: 1500, lookupInFlight: true }), false);
    assert.equal(validateSuperBarcodeLookup({ code: "" }), "Ingresá un código de barras para buscar.");
    assert.equal(validateSuperBarcodeLookup({ code: "0".repeat(SUPER_FIELD_LIMITS.barcodeCode + 1) }), `El código de barras no puede superar ${SUPER_FIELD_LIMITS.barcodeCode} caracteres.`);
    assert.equal(validateSuperBarcodeLookup({ code: "0075012345678" }), "");
    assert.equal(superBarcodeAliasLabel({ code: "0075012345678", format: "EAN_13" }), "0075012345678 · EAN_13");
    assert.equal(superItemConfigurationLabel(superItemFixture({ unit: "kg", habitualObjective: "2.000", configured: true })), "Configurado");
    assert.equal(superItemConfigurationLabel(superItemFixture({ unit: "kg", habitualObjective: null, configured: false })), "Pendiente");
    assert.equal(superItemStockLabel(superItemFixture({ currentStock: null, unit: "kg" })), "Sin cargar");
    assert.equal(superItemStockLabel(superItemFixture({ currentStock: "0", unit: "kg" })), "0 kg");
    assert.equal(superItemStockLabel(superItemFixture({ currentStock: "2.500", unit: "kg" })), "2.500 kg");
    assert.equal(superItemQuickQuantityLabel(superItemFixture({ quickQuantity: "1.000", unit: "kg" })), "1.000 kg");
    assert.equal(superItemQuickQuantityLabel(superItemFixture({ quickQuantity: "1.000", unit: null })), "—");
    assert.equal(superItemCommercialPresentationLabel(superItemFixture({ commercialPresentationLabel: null, commercialPresentationQuantity: null, unit: "kg" })), "—");
    assert.equal(superItemCommercialPresentationLabel(superItemFixture({ commercialPresentationLabel: "Pack x 6", commercialPresentationQuantity: null, unit: "unidad" })), "Pack x 6");
    assert.equal(superItemCommercialPresentationLabel(superItemFixture({ commercialPresentationLabel: "Pack x 6", commercialPresentationQuantity: "6.000", unit: "unidad" })), "Pack x 6 · 6.000 unidad");
    assert.equal(superItemCommercialPresentationPriceLabel(superItemFixture({ commercialPresentationPricePesos: null })), "—");
    assert.equal(superItemCommercialPresentationPriceLabel(superItemFixture({ commercialPresentationPricePesos: "1250.50" })), "ARS 1,250.50");
    assert.equal(superItemCommercialPresentationPriceSourceLabel(superItemFixture({ commercialPresentationPriceSourceLabel: null })), "");
    assert.equal(superItemCommercialPresentationPriceSourceLabel(superItemFixture({ commercialPresentationPriceSourceLabel: "Ticket proveedor" })), "Fuente: Ticket proveedor");
    assert.equal(superItemCommercialPresentationPriceObservedDateLabel(superItemFixture({ commercialPresentationPriceObservedDate: null })), "");
    assert.equal(superItemCommercialPresentationPriceObservedDateLabel(superItemFixture({ commercialPresentationPriceObservedDate: "2026-07-18" })), "Observado: 2026-07-18");
    assert.match(superItemCommercialPresentationPriceHtml(superItemFixture({ commercialPresentationPricePesos: "1250.50", commercialPresentationPriceSourceLabel: "Ticket proveedor", commercialPresentationPriceObservedDate: "2026-07-18" })), /Fuente: Ticket proveedor[\s\S]*Observado: 2026-07-18/);
    assert.equal(supermarketSource.includes("item.superItemCommercialPresentationPriceLabel"), false);
    assert.equal(supermarketSource.includes("formatDate(item.commercialPresentationPriceObservedDate"), false);
    assert.doesNotMatch(supermarketSource, /commercialPresentationPriceObservedAt|observedAt|ObservedAt|datetime|timestamp/);
    assert.match(supermarketSource, /superPriceObservationPayloadFromValues/);
    assert.match(supermarketSource, /superPriceSourcePayloadFromValues/);
    assert.match(supermarketSource, /createSuperPriceSource/);
    assert.match(supermarketSource, /superPriceSources/);
    assert.match(supermarketSource, /createSuperItemPriceObservation/);
    assert.match(supermarketSource, /superPriceObservations/);
    assert.match(supermarketSource, /data-super-action="history"/);
    assert.match(supermarketSource, /super-movement-history/);
    assert.equal(superMovementTypeLabel("PURCHASE"), "Compra");
    assert.equal(superMovementTypeLabel("CONSUMPTION"), "Consumo");
    assert.equal(superMovementTypeLabel("QUICK_CONSUMPTION"), "Consumo rápido");
    assert.equal(superMovementTypeLabel("ADJUSTMENT"), "Ajuste");
    assert.equal(superMovementQuantityLabel({ movementType: "PURCHASE", quantity: "2.000", itemUnit: "kg" }), "+2.000 kg");
    assert.equal(superMovementQuantityLabel({ movementType: "CONSUMPTION", quantity: "1.000", itemUnit: "kg" }), "-1.000 kg");
    assert.equal(superMovementQuantityLabel({ movementType: "ADJUSTMENT", quantity: null, resultingStock: "4.000", itemUnit: "kg" }), "Ajuste a 4.000 kg");
    assert.equal(superMovementSummary({ movementType: "QUICK_CONSUMPTION", itemName: "Arroz", quantity: "1.000", resultingStock: "3.000", itemUnit: "kg" }), "Consumo rápido · Arroz · -1.000 kg · stock 3.000 kg");
    assert.deepEqual([...groupSuperItems([
        superItemFixture({ name: "Zanahoria", categoryName: "Verdulería" }),
        superItemFixture({ name: "Arroz", categoryName: "Almacén" }),
        superItemFixture({ name: "Banana", categoryName: "Verdulería" })
    ]).entries()].map(([categoryName, items]) => [categoryName, items.map((item) => item.name)]), [
        ["Almacén", ["Arroz"]],
        ["Verdulería", ["Banana", "Zanahoria"]]
    ]);
    assert.equal(generatedSuperListText([]), "No hay productos marcados para comprar.");
    assert.equal(generatedSuperListText([
        superItemFixture({ name: "Zanahoria", categoryName: "Verdulería", checked: true }),
        superItemFixture({ name: "Arroz", categoryName: "Almacén", checked: true, notes: "Doble carolina", unit: "kg", quickQuantity: "1.000" }),
        superItemFixture({ name: "Leche", categoryName: "Lácteos", checked: false, unit: "litro", habitualObjective: "2.000", configured: true })
    ]), "Lista del super\n\nAlmacén\n- Arroz (1.000 kg) — Doble carolina\n\nVerdulería\n- Zanahoria");
    assert.equal(generatedSuperListText([
        superItemFixture({ name: "Arroz", categoryName: "Almacén", checked: true, unit: "kg", habitualObjective: "3.000", configured: true, stock: "12", price: "99", barcode: "779", ocr: true, suggestedList: true }),
        superItemFixture({ name: "Leche", categoryName: "Lácteos", checked: true, movements: [{ quantity: 1 }], suggestedQuantity: 2 })
    ]), "Lista del super\n\nAlmacén\n- Arroz\n\nLácteos\n- Leche");
    assert.equal(generatedSuperListText([
        superItemFixture({ name: "Arroz", categoryName: "Almacén", checked: false, suggestedQuantity: "2.000", unit: "kg" }),
        superItemFixture({ name: "Leche", categoryName: "Lácteos", checked: true, quickQuantity: "1.000", unit: "litro" })
    ]), "Lista del super\n\nLácteos\n- Leche (1.000 litro)");
    assert.match(indexHtml, /id="super-ticket-ocr-form"/);
    assert.match(indexHtml, /id="super-ticket-ocr-file"[^>]+name="file"[^>]+accept="image\/png,image\/jpeg,.png,.jpg,.jpeg"/);
    assert.match(indexHtml, /id="super-ticket-ocr-summary"/);
    assert.match(indexHtml, /id="super-ticket-ocr-warning-list"/);
    assert.match(indexHtml, /id="super-ticket-ocr-table"/);
    assert.match(indexHtml, /id="super-ticket-ocr-confirm-form"/);
    assert.match(indexHtml, /id="super-ticket-ocr-product"/);
    assert.match(indexHtml, /id="super-ticket-ocr-date-candidate"/);
    assert.match(indexHtml, /id="super-ticket-ocr-date"[^>]+type="date"/);
    assert.match(indexHtml, /id="super-ticket-ocr-source-candidate"/);
    assert.match(indexHtml, /id="super-ticket-ocr-source-label"[^>]+data-super-limit="priceSourceLabel"/);
    assert.match(indexHtml, /id="super-ticket-ocr-sync-current-reference-price"[^>]+type="checkbox"/);
    assert.match(indexHtml, /id="super-ticket-ocr-discard"/);
    assert.match(indexHtml, /Los candidatos se descartan al refrescar o descartar esta revisión\./);
    assert.match(indexHtml, /Nada se guarda hasta confirmar una fila\./);
    assert.match(stylesCss, /\.super-ticket-ocr-card/);
    assert.match(stylesCss, /\.super-ticket-ocr-upload-form/);
    assert.match(stylesCss, /\.super-ticket-ocr-review-grid/);
    assert.match(stylesCss, /\.super-ticket-ocr-warning-list/);
    assert.match(stylesCss, /\.super-ticket-ocr-selected-warnings/);
    assert.match(stylesCss, /\.super-barcode-scan-controls/);
    assert.match(stylesCss, /\.super-barcode-scanner/);
    assert.match(stylesCss, /\.super-barcode-scanner-preview/);
    assert.match(stylesCss, /\.super-barcode-scanner-status/);
    assert.match(stylesCss, /\.super-barcode-action-group/);
    assert.match(stylesCss, /\.super-barcode-action-buttons/);
    assert.match(supermarketSource, /submitSuperTicketOcrUploadForm/);
    assert.match(supermarketSource, /submitSuperTicketOcrConfirmForm/);
    assert.match(supermarketSource, /renderSuperTicketOcrReview/);
    assert.match(supermarketSource, /createSuperItemPriceObservation/);
    assert.doesNotMatch(supermarketSource, /localStorage|sessionStorage/);
    assert.match(supermarketSource, /BarcodeDetector/);
    assert.match(supermarketSource, /getUserMedia/);
    assert.match(supermarketSource, /visibilitychange/);
    assert.match(supermarketSource, /pagehide/);
    assertNoUnsupportedSuperInventorySemantics(supermarketSource);

    const supermarketDom = fakeSupermarketDom();
    const previousSupermarketDocument = globalThis.document;
    const previousSupermarketConfirm = globalThis.confirm;
    const previousSupermarketNavigator = globalThis.navigator;
    const previousSupermarketOpen = globalThis.open;
    const previousSupermarketConsole = globalThis.console;
    const previousSupermarketBarcodeDetector = globalThis.BarcodeDetector;
    const previousSupermarketSecureContext = globalThis.isSecureContext;
    const previousSupermarketAddEventListener = globalThis.addEventListener;
    const previousSupermarketRequestAnimationFrame = globalThis.requestAnimationFrame;
    const previousSupermarketCancelAnimationFrame = globalThis.cancelAnimationFrame;
    const supermarketConfirmMessages = [];
    const supermarketGlobalListeners = new Map();
    const scheduledAnimationFrames = [];
    const detectorResults = [];
    const deniedError = new Error("Permiso denegado");
    deniedError.name = "NotAllowedError";
    let streamStopCount = 0;
    let denyCameraAccess = false;
    let lastRequestedStream = null;
    globalThis.document = supermarketDom.document;
    globalThis.confirm = (message) => {
        supermarketConfirmMessages.push(message);
        return true;
    };
    globalThis.BarcodeDetector = class BarcodeDetector {
        async detect() {
            return detectorResults.shift() || [];
        }
    };
    globalThis.isSecureContext = true;
    globalThis.addEventListener = (type, listener) => {
        supermarketGlobalListeners.set(type, listener);
    };
    globalThis.requestAnimationFrame = (callback) => {
        scheduledAnimationFrames.push(callback);
        return scheduledAnimationFrames.length;
    };
    globalThis.cancelAnimationFrame = () => {};
    Object.defineProperty(globalThis, "navigator", {
        configurable: true,
        value: {
            clipboard: { async writeText(text) { supermarketDom.copiedText = text; } },
            mediaDevices: {
                async getUserMedia() {
                    if (denyCameraAccess) {
                        throw deniedError;
                    }
                    lastRequestedStream = fakeMediaStream(() => {
                        streamStopCount += 1;
                    });
                    return lastRequestedStream;
                }
            }
        }
    });
    globalThis.open = (url) => {
        supermarketDom.openedUrl = url;
    };
    try {
        setupTabs({
            buttons: [...supermarketDom.supermarketTabs.buttonsById.values()],
            panels: [...supermarketDom.supermarketTabs.sectionsById.values()],
            defaultTabId: DEFAULT_SUPERMARKET_TAB_ID,
            targetDatasetKey: "superTabTarget",
            panelDatasetKey: "superTabPanel"
        });
        assertTabState(supermarketDom.supermarketTabs, "list");

        setupSupermarket({ apiClient: supermarketDom.api });
        await flushAsyncWork();
        assert.deepEqual(supermarketDom.api.calls.slice(0, 6), [{ method: "superCategories" }, { method: "superItems" }, { method: "superSuggestedList" }, { method: "superPriceSources" }, { method: "activeSuperScanSession" }, { method: "superPriceObservations", filters: { limit: 50 } }]);
        assert.match(supermarketDom.elements.get("#super-session-summary").textContent, /Sesión activa/);
        assert.equal(supermarketDom.elements.get("#super-session-lines").children.length, 0);
        assert.equal(supermarketDom.elements.get("#super-suggested-list").innerHTML.includes("Comprar 2.000 kg"), true);
        assert.equal(supermarketDom.elements.get("#super-suggested-list").innerHTML.includes("Arroz"), true);
        assert.equal(supermarketDom.elements.get("#super-suggested-empty").hidden, true);
        assert.equal(supermarketDom.elements.get("#super-suggested-summary").textContent, "1 producto sugerido para reponer.");
        renderSuperSuggestedItems([]);
        assert.equal(supermarketDom.elements.get("#super-suggested-list").innerHTML, "");
        assert.equal(supermarketDom.elements.get("#super-suggested-empty").hidden, false);
        assert.equal(supermarketDom.elements.get("#super-suggested-summary").textContent, "Sin sugerencias por ahora.");
        renderSuperSuggestedItems([{ itemId: 10, name: "Arroz", categoryName: "Almacén", unit: "kg", currentStock: "1.000", habitualObjective: "3.000", suggestedQuantity: "2.000" }]);
        assert.equal(supermarketDom.elements.get("#super-category-name").maxLength, SUPER_FIELD_LIMITS.categoryName);
        assert.equal(supermarketDom.elements.get("#super-item-name").maxLength, SUPER_FIELD_LIMITS.itemName);
        assert.equal(supermarketDom.elements.get("#super-item-notes").maxLength, SUPER_FIELD_LIMITS.itemNotes);
        assert.equal(supermarketDom.elements.get("#super-item-unit").maxLength, SUPER_FIELD_LIMITS.itemUnit);
        assert.equal(supermarketDom.elements.get("#super-item-presentation-label").maxLength, SUPER_FIELD_LIMITS.presentationLabel);
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-source-label").maxLength, SUPER_FIELD_LIMITS.priceSourceLabel);
        assert.equal(supermarketDom.elements.get("#super-price-observation-source-label").maxLength, SUPER_FIELD_LIMITS.priceSourceLabel);
        assert.equal(supermarketDom.elements.get("#super-price-source-name").maxLength, SUPER_FIELD_LIMITS.priceSourceLabel);
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-observed-date").maxLength, -1);
        assert.equal(supermarketDom.elements.get("#super-barcode-code").maxLength, SUPER_FIELD_LIMITS.barcodeCode);
        assert.equal(supermarketDom.elements.get("#super-barcode-format").maxLength, SUPER_FIELD_LIMITS.barcodeFormat);
        assert.match(supermarketDom.elements.get("#super-item-category").innerHTML, /Almacén/);
        assert.match(supermarketDom.elements.get("#super-barcode-item").innerHTML, /Arroz/);
        assert.equal(supermarketDom.elements.get("#super-category-table-wrap").hidden, true);
        assert.equal(supermarketDom.elements.get("#super-category-toggle").textContent, "Mostrar categorías (2)");
        assert.equal(supermarketDom.elements.get("#super-category-toggle").attributes.get("aria-expanded"), "false");
        assert.equal(supermarketDom.elements.get("#super-category-list").children.length, 2);
        assert.match(supermarketDom.elements.get("#super-category-list").children[0].innerHTML, /data-super-category-action="edit"/);
        assert.match(supermarketDom.elements.get("#super-category-list").children[0].innerHTML, /data-super-category-action="delete"/);
        await supermarketDom.elements.get("#super-category-toggle").click();
        assert.equal(supermarketDom.elements.get("#super-category-table-wrap").hidden, false);
        assert.equal(supermarketDom.elements.get("#super-category-toggle").textContent, "Ocultar categorías (2)");
        assert.equal(supermarketDom.elements.get("#super-category-toggle").attributes.get("aria-expanded"), "true");
        assert.equal(supermarketDom.elements.get("#super-items-table").children.length, 5);
        assert.match(supermarketDom.elements.get("#super-items-table").children[0].innerHTML, /Almacén/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[2].innerHTML, /Verdulería/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /Configurado/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /Pack x 6 · 6\.000 kg/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /ARS\s1,250\.50/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /Fuente: Ticket proveedor/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /Observado: 2026-07-18/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /Sin cargar/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /1\.000 kg/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[3].innerHTML, /Pendiente/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[3].innerHTML, />0</);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /data-super-action="purchase"/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /data-super-action="consume"/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /data-super-action="quick-consume"/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /data-super-action="history"/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /data-super-action="price-history"/);
        assert.match(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, /Ver observaciones de precio de Arroz/);
        assertResponsiveCardLabels(supermarketDom.elements.get("#super-items-table").children[1].innerHTML, ["Estado", "Producto", "Categoría", "Configuración", "Presentación", "Precio ref.", "Stock", "Cantidad rápida", "Notas", "Acciones"]);
        assertResponsiveCardLabels(supermarketDom.elements.get("#super-category-list").children[0].innerHTML, ["Categoría", "Acciones"]);
        assert.match(supermarketDom.elements.get("#super-price-observation-item").innerHTML, /Arroz/);
        assert.match(supermarketDom.elements.get("#super-price-observation-price-source").innerHTML, /Sin fuente reutilizable/);
        assert.match(supermarketDom.elements.get("#super-price-observation-price-source").innerHTML, /Ticket proveedor/);
        assert.match(supermarketDom.elements.get("#super-price-observation-price-source").innerHTML, /Lista mayorista/);
        assert.equal(supermarketDom.elements.get("#super-price-observation-table").children.length, 2);
        supermarketDom.elements.get("#super-barcode-code").value = "0075012345678";
        const sessionHandoffCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-barcode-form").submit();
        await supermarketDom.elements.get("#super-barcode-purchase").click();
        assert.deepEqual(supermarketDom.api.calls.slice(sessionHandoffCallStart).map((call) => call.method), [
            "lookupSuperItemBarcodeAlias",
            "queueSuperScanSessionResolvedItem",
            "createSuperScanSessionDraft"
        ]);
        assert.equal(supermarketDom.elements.get("#super-session-lines").children.length, 1);
        assert.match(supermarketDom.elements.get("#super-session-summary").textContent, /1 borrador/);
        assert.equal(supermarketDom.elements.get("#super-session-save-draft").disabled, false);
        assert.equal(supermarketDom.elements.get("#super-session-confirm").disabled, false);
        assert.equal(supermarketDom.elements.get("#super-session-item-name").value, "Arroz");
        assert.equal(supermarketDom.elements.get("#super-price-observation-title").textContent, "Observaciones de precio");
        assert.equal(supermarketDom.elements.get("#super-price-observation-context-summary").textContent, "Historial reciente global de observaciones de precio.");
        assert.equal(supermarketDom.elements.get("#super-price-observation-global-reset").hidden, true);
        assert.match(supermarketDom.elements.get("#super-price-observation-table").children[0].innerHTML, /Arroz/);
        assert.match(supermarketDom.elements.get("#super-price-observation-table").children[0].innerHTML, /Pack x 6 · 6\.000/);
        assert.match(supermarketDom.elements.get("#super-price-observation-table").children[0].innerHTML, /ARS\s1,250\.50/);
        assert.match(supermarketDom.elements.get("#super-price-observation-table").children[0].innerHTML, /Ticket proveedor/);
        assertResponsiveCardLabels(supermarketDom.elements.get("#super-price-observation-table").children[0].innerHTML, ["Creada", "Producto", "Presentación", "Precio", "Fuente", "Observada"]);
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-review-panel").hidden, true);
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-summary").textContent, "No hay candidatos OCR cargados.");
        const invalidTicketUploadCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-ticket-ocr-form").submit();
        assert.equal(supermarketDom.api.calls.length, invalidTicketUploadCallStart);
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-feedback").textContent, "Seleccioná una imagen PNG o JPEG para extraer candidatos.");

        supermarketDom.elements.get("#super-ticket-ocr-file").files = [new File(["ticket"], "ticket.png", { type: "image/png" })];
        const ticketUploadCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-ticket-ocr-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(ticketUploadCallStart), [{ method: "uploadSuperTicketOcrCandidates", file: supermarketDom.elements.get("#super-ticket-ocr-file").files[0] }]);
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-review-panel").hidden, false);
        assert.match(supermarketDom.elements.get("#super-ticket-ocr-summary").textContent, /ticket\.png/);
        assert.match(supermarketDom.elements.get("#super-ticket-ocr-warning-list").innerHTML, /Review OCR output before saving/);
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-table").children.length, 2);
        assert.match(supermarketDom.elements.get("#super-ticket-ocr-table").children[0].innerHTML, /ARROZ 1250,50/);
        assert.match(supermarketDom.elements.get("#super-ticket-ocr-table").children[1].innerHTML, /Low OCR confidence; review this line manually/);
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-selected-line").value, "ARROZ 1250,50");
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-price-pesos").value, "1250.50");
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-date").value, "2026-07-18");
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-source-label").value, "Ticket proveedor");

        supermarketDom.supermarketTabs.buttonsById.get("tickets").click();
        assertTabState(supermarketDom.supermarketTabs, "tickets");

        await supermarketDom.elements.get("#super-ticket-ocr-table").clickTarget(fakeSuperTicketOcrActionButton("select", "1"));
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-line-index").value, "1");
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-selected-line").value, "BANANA ???");
        assert.match(supermarketDom.elements.get("#super-ticket-ocr-selected-warnings").innerHTML, /Low OCR confidence/);
        supermarketDom.elements.get("#super-ticket-ocr-product").value = "11";
        supermarketDom.elements.get("#super-ticket-ocr-description").value = "Banana madura";
        supermarketDom.elements.get("#super-ticket-ocr-price-pesos").value = "900.00";
        supermarketDom.elements.get("#super-ticket-ocr-source-candidate").value = "Ticket proveedor";
        await supermarketDom.elements.get("#super-ticket-ocr-source-candidate").change();
        supermarketDom.elements.get("#super-ticket-ocr-date-candidate").value = "2026-07-18";
        await supermarketDom.elements.get("#super-ticket-ocr-date-candidate").change();
        const ticketConfirmCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-ticket-ocr-confirm-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(ticketConfirmCallStart), [
            {
                method: "createSuperItemPriceObservation",
                id: 11,
                payload: { pricePesos: "900.00", sourceLabel: "Ticket proveedor", observedDate: "2026-07-18" }
            },
            { method: "superPriceObservations", filters: { limit: 50 } }
        ]);
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-confirm-feedback").textContent, "Observación creada.");

        supermarketDom.elements.get("#super-ticket-ocr-product").value = "10";
        supermarketDom.elements.get("#super-ticket-ocr-price-pesos").value = "1250.50";
        supermarketDom.elements.get("#super-ticket-ocr-sync-current-reference-price").checked = true;
        const ticketSyncConfirmCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-ticket-ocr-confirm-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(ticketSyncConfirmCallStart).map((call) => call.method), [
            "createSuperItemPriceObservation",
            "superCategories",
            "superItems",
            "superSuggestedList",
            "superPriceSources",
            "activeSuperScanSession",
            "superPriceObservations",
            "superStockMovements"
        ]);
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-confirm-feedback").textContent, "Observación creada y precio sincronizado.");
        const preservedTicketSummary = supermarketDom.elements.get("#super-ticket-ocr-summary").textContent;
        const preservedTicketSelectedLine = supermarketDom.elements.get("#super-ticket-ocr-selected-line").value;

        supermarketDom.supermarketTabs.buttonsById.get("barcode").click();
        assertTabState(supermarketDom.supermarketTabs, "barcode");
        assert.equal(supermarketDom.elements.get("#super-session-lines").children.length, 1);
        assert.match(supermarketDom.elements.get("#super-session-summary").textContent, /1 borrador/);

        supermarketDom.supermarketTabs.buttonsById.get("tickets").click();
        assertTabState(supermarketDom.supermarketTabs, "tickets");
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-review-panel").hidden, false);
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-summary").textContent, preservedTicketSummary);
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-selected-line").value, preservedTicketSelectedLine);

        await supermarketDom.elements.get("#super-ticket-ocr-discard").click();
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-review-panel").hidden, true);
        assert.equal(supermarketDom.elements.get("#super-ticket-ocr-summary").textContent, "No hay candidatos OCR cargados.");

        supermarketDom.elements.get("#super-price-observation-item").value = "10";
        await supermarketDom.elements.get("#super-price-observation-item").change();
        assert.equal(supermarketDom.elements.get("#super-price-observation-price-pesos").value, "1250.50");
        assert.equal(supermarketDom.elements.get("#super-price-observation-price-source").value, "7");
        assert.equal(supermarketDom.elements.get("#super-price-observation-source-label").value, "");
        assert.equal(supermarketDom.elements.get("#super-price-observation-observed-date").value, "2026-07-18");

        supermarketDom.elements.get("#super-price-observation-price-source").value = "7";
        supermarketDom.elements.get("#super-price-observation-item").value = "11";
        await supermarketDom.elements.get("#super-price-observation-item").change();
        assert.equal(supermarketDom.elements.get("#super-price-observation-price-source").value, "");
        assert.equal(supermarketDom.elements.get("#super-price-observation-source-label").value, "");

        supermarketDom.elements.get("#super-price-observation-price-pesos").value = "0";
        const invalidObservationCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-price-observation-form").submit();
        assert.equal(supermarketDom.api.calls.length, invalidObservationCallStart);
        assert.equal(supermarketDom.elements.get("#super-price-observation-feedback").textContent, "El precio observado debe ser mayor que cero.");

        supermarketDom.elements.get("#super-price-observation-item").value = "11";
        supermarketDom.elements.get("#super-price-observation-price-pesos").value = "950";
        supermarketDom.elements.get("#super-price-observation-price-source").value = "";
        supermarketDom.elements.get("#super-price-observation-source-label").value = "";
        supermarketDom.elements.get("#super-price-observation-observed-date").value = "";
        const switchedItemObservationCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-price-observation-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(switchedItemObservationCallStart), [
            {
                method: "createSuperItemPriceObservation",
                id: 11,
                payload: { pricePesos: "950" }
            },
            { method: "superPriceObservations", filters: { limit: 50 } }
        ]);

        supermarketDom.elements.get("#super-price-observation-item").value = "10";
        supermarketDom.elements.get("#super-price-observation-price-pesos").value = "1500.25";
        supermarketDom.elements.get("#super-price-observation-price-source").value = "7";
        supermarketDom.elements.get("#super-price-observation-source-label").value = ` ${"Ticket proveedor largo ".repeat(8)} `;
        supermarketDom.elements.get("#super-price-observation-observed-date").value = "2026-07-18";
        const createObservationCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-price-observation-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(createObservationCallStart), [
            {
                method: "createSuperItemPriceObservation",
                id: 10,
                payload: {
                    pricePesos: "1500.25",
                    priceSourceId: 7,
                    observedDate: "2026-07-18"
                }
            },
            { method: "superPriceObservations", filters: { limit: 50 } }
        ]);
        assert.equal(supermarketDom.elements.get("#super-price-observation-feedback").textContent, "Observación de precio registrada.");

        supermarketDom.elements.get("#super-price-observation-item").value = "10";
        supermarketDom.elements.get("#super-price-observation-price-pesos").value = "1750.75";
        supermarketDom.elements.get("#super-price-observation-price-source").value = "7";
        supermarketDom.elements.get("#super-price-observation-source-label").value = "";
        supermarketDom.elements.get("#super-price-observation-observed-date").value = "2026-07-21";
        supermarketDom.elements.get("#super-price-observation-sync-current-reference-price").checked = true;
        const syncObservationCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-price-observation-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(syncObservationCallStart).map((call) => call.method), [
            "createSuperItemPriceObservation",
            "superCategories",
            "superItems",
            "superSuggestedList",
            "superPriceSources",
            "activeSuperScanSession",
            "superPriceObservations",
            "superStockMovements"
        ]);
        assert.deepEqual(supermarketDom.api.calls.slice(syncObservationCallStart, syncObservationCallStart + 1), [
            {
                method: "createSuperItemPriceObservation",
                id: 10,
                payload: {
                    pricePesos: "1750.75",
                    syncCurrentReferencePrice: true,
                    priceSourceId: 7,
                    observedDate: "2026-07-21"
                }
            }
        ]);
        assert.equal(supermarketDom.elements.get("#super-price-observation-feedback").textContent, "Observación registrada y precio actual/de referencia actualizado.");
        assert.equal(supermarketDom.elements.get("#super-price-observation-sync-current-reference-price").checked, false);

        supermarketDom.elements.get("#super-price-source-name").value = "  Lista nueva ";
        const createPriceSourceCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-price-source-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(createPriceSourceCallStart), [
            { method: "createSuperPriceSource", payload: { name: "Lista nueva" } },
            { method: "superPriceSources" },
            { method: "superPriceObservations", filters: { limit: 50 } }
        ]);
        assert.equal(supermarketDom.elements.get("#super-price-observation-price-source").value, "9");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-source").value, "9");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-source-label").value, "");
        assert.equal(supermarketDom.elements.get("#super-price-source-feedback").textContent, "Fuente de precio creada.");
        assert.match(supermarketDom.elements.get("#super-price-observation-price-source").innerHTML, /Lista nueva/);
        assert.match(supermarketDom.elements.get("#super-item-presentation-price-source").innerHTML, /Lista nueva/);
        supermarketDom.elements.get("#super-item-presentation-price-source-label").value = "Ticket manual";
        await supermarketDom.elements.get("#super-item-presentation-price-source-label").input?.();
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-source").value, "");
        supermarketDom.elements.get("#super-item-presentation-price-source").value = "8";
        await supermarketDom.elements.get("#super-item-presentation-price-source").change?.();
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-source-label").value, "");

        supermarketDom.elements.get("#super-price-observation-item").value = "10";
        supermarketDom.elements.get("#super-price-observation-price-pesos").value = "1600";
        supermarketDom.elements.get("#super-price-observation-price-source").value = "";
        supermarketDom.elements.get("#super-price-observation-source-label").value = "  Ticket libre ";
        supermarketDom.elements.get("#super-price-observation-observed-date").value = "";
        const createFreeTextObservationCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-price-observation-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(createFreeTextObservationCallStart), [
            {
                method: "createSuperItemPriceObservation",
                id: 10,
                payload: { pricePesos: "1600", sourceLabel: "Ticket libre" }
            },
            { method: "superPriceObservations", filters: { limit: 50 } }
        ]);

        detectorResults.push([{ rawValue: "  0075012345678  ", format: " EAN_13 " }]);
        await supermarketDom.elements.get("#super-barcode-scan-start").click();
        assert.equal(supermarketDom.elements.get("#super-barcode-scan-start").disabled, true);
        assert.equal(supermarketDom.elements.get("#super-barcode-scan-stop").disabled, false);
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner").hidden, false);
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner-preview").hidden, false);
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner-status").textContent, "Escaneando... Apuntá al código de barras.");
        await scheduledAnimationFrames.shift()?.();
        await flushAsyncWork();
        assert.equal(supermarketDom.elements.get("#super-barcode-code").value, "0075012345678");
        assert.equal(supermarketDom.elements.get("#super-barcode-format").value, "EAN_13");
        assert.equal(supermarketDom.elements.get("#super-barcode-actions").hidden, false);
        assert.equal(supermarketDom.elements.get("#super-barcode-purchase").disabled, false);
        assert.equal(supermarketDom.elements.get("#super-barcode-consume").disabled, false);
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner").hidden, true);
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner-preview").srcObject, null);
        assert.equal(streamStopCount > 0, true);
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner-status").textContent, "Escaneo listo para Arroz. Enviá compra o consumo a la sesión para revisarlo antes de confirmar stock.");

        const scanSessionCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-barcode-purchase").click();
        assert.deepEqual(supermarketDom.api.calls.slice(scanSessionCallStart).map((call) => call.method), [
            "queueSuperScanSessionResolvedItem",
            "createSuperScanSessionDraft"
        ]);
        assert.equal(supermarketDom.elements.get("#super-movement-modal").hidden, true);
        assert.equal(supermarketDom.elements.get("#super-session-lines").children.length > 0, true);
        assert.equal(supermarketDom.elements.get("#super-session-save-draft").disabled, false);
        assert.equal(supermarketDom.elements.get("#super-session-confirm").disabled, false);

        detectorResults.push([]);
        await supermarketDom.elements.get("#super-barcode-scan-start").click();
        supermarketDom.document.hidden = true;
        supermarketDom.document.dispatchEvent({ type: "visibilitychange" });
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner").hidden, true);
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner-status").textContent, "Escaneo detenido al ocultarse la página. Podés retomarlo cuando quieras.");
        supermarketDom.document.hidden = false;

        denyCameraAccess = true;
        await supermarketDom.elements.get("#super-barcode-scan-start").click();
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner-status").textContent, "Permiso de cámara denegado. Ingresá el código manualmente.");
        assert.equal(supermarketDom.elements.get("#super-barcode-code").focused, true);
        denyCameraAccess = false;

        detectorResults.push([]);
        await supermarketDom.elements.get("#super-barcode-scan-start").click();
        supermarketGlobalListeners.get("pagehide")?.({ type: "pagehide" });
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner").hidden, true);
        assert.equal(lastRequestedStream.getTracks()[0].stopped, true);

        let resolvePendingCamera;
        let pendingStreamStopCount = 0;
        const pendingStream = fakeMediaStream(() => {
            pendingStreamStopCount += 1;
        });
        globalThis.navigator.mediaDevices.getUserMedia = async () => new Promise((resolve) => {
            resolvePendingCamera = () => resolve(pendingStream);
        });
        detectorResults.push([]);
        const pendingStartClick = supermarketDom.elements.get("#super-barcode-scan-start").click();
        await flushAsyncWork();
        await supermarketDom.elements.get("#super-barcode-scan-stop").click();
        resolvePendingCamera();
        await pendingStartClick;
        await flushAsyncWork();
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner").hidden, true);
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner-preview").srcObject, null);
        assert.equal(pendingStreamStopCount, 1);
        assert.equal(supermarketDom.elements.get("#super-barcode-scanner-status").textContent, "Escaneo detenido. Podés ingresar el código manualmente.");
        globalThis.navigator.mediaDevices.getUserMedia = async () => {
            lastRequestedStream = fakeMediaStream(() => {
                streamStopCount += 1;
            });
            return lastRequestedStream;
        };

        supermarketDom.elements.get("#super-barcode-code").value = "  0075012345678  ";
        const barcodeFoundCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-barcode-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(barcodeFoundCallStart), [{ method: "lookupSuperItemBarcodeAlias", code: "0075012345678" }]);
        assert.equal(supermarketDom.elements.get("#super-barcode-result").textContent, "Código 0075012345678 asociado a Arroz.");
        assert.equal(supermarketDom.elements.get("#super-items-table").children[1].classList.contains("super-item-barcode-match"), true);
        assert.equal(supermarketDom.elements.get("#super-barcode-remove").hidden, false);

        const removeBarcodeCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-barcode-remove").click();
        assertSupermarketMutationAfter(supermarketDom, removeBarcodeCallStart, { method: "removeSuperItemBarcodeAlias", itemId: 10, aliasId: 44 });
        assert.equal(supermarketDom.elements.get("#super-barcode-result").textContent, "Alias 0075012345678 quitado de Arroz.");

        supermarketDom.elements.get("#super-barcode-code").value = "0000000000001";
        const barcodeNotFoundCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-barcode-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(barcodeNotFoundCallStart), [{ method: "lookupSuperItemBarcodeAlias", code: "0000000000001" }]);
        assert.equal(supermarketDom.elements.get("#super-barcode-result").textContent, "Código 0000000000001 no encontrado. Podés asociarlo a un producto existente.");
        assert.equal(supermarketDom.elements.get("#super-barcode-attach").disabled, false);
        supermarketDom.elements.get("#super-barcode-item").value = "11";
        supermarketDom.elements.get("#super-barcode-format").value = "  EAN_13 ";
        const attachBarcodeCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-barcode-attach").click();
        assertSupermarketMutationAfter(supermarketDom, attachBarcodeCallStart, {
            method: "attachSuperItemBarcodeAlias",
            id: 11,
            payload: { code: "0000000000001", format: "EAN_13" }
        });
        assert.equal(supermarketDom.elements.get("#super-barcode-result").textContent, "Código 0000000000001 asociado a Banana.");

        const barcodeCallsAfterUi = supermarketDom.api.calls.slice(barcodeFoundCallStart).map((call) => call.method);
        assert.deepEqual(barcodeCallsAfterUi.filter((method) => ["adjustSuperItemStock", "updateSuperItemChecked", "purchaseSuperItem", "consumeSuperItem", "quickConsumeSuperItem", "superStockMovements"].includes(method)), []);

        const movementRow = supermarketDom.elements.get("#super-items-table").children[1];
        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("purchase", movementRow, "10"));
        assert.equal(supermarketDom.elements.get("#super-movement-modal").hidden, false);
        assert.equal(supermarketDom.elements.get("#super-movement-title").textContent, "Registrar compra");
        assert.equal(supermarketDom.elements.get("#super-movement-item-name").textContent, "Arroz");
        assert.equal(supermarketDom.elements.get(".super-movement-negative-field").hidden, true);
        supermarketDom.elements.get("#super-movement-quantity").value = "2.000";
        supermarketDom.elements.get("#super-movement-notes").value = " Reposición ";
        const purchaseCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-movement-form").submit();
        assertSupermarketMutationAfter(supermarketDom, purchaseCallStart, {
            method: "purchaseSuperItem",
            id: 10,
            payload: { quantity: "2.000", notes: "Reposición" }
        });

        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("consume", movementRow, "10"));
        assert.equal(supermarketDom.elements.get(".super-movement-negative-field").hidden, false);
        supermarketDom.elements.get("#super-movement-quantity").value = "5.000";
        const consumeCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-movement-form").submit();
        assert.equal(supermarketConfirmMessages.at(-1).includes("stock negativo"), true);
        assertSupermarketMutationsAfter(supermarketDom, consumeCallStart, [
            { method: "consumeSuperItem", id: 10, payload: { quantity: "5.000", notes: "", allowNegativeStock: false } },
            { method: "consumeSuperItem", id: 10, payload: { quantity: "5.000", notes: "", allowNegativeStock: true } }
        ]);

        const quickConsumeCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("quick-consume", movementRow, "10"));
        assert.equal(supermarketConfirmMessages.at(-1).includes("Stock actual: 1.000"), true);
        assert.equal(supermarketConfirmMessages.at(-1).includes("Resultado: -1.000"), true);
        assertSupermarketMutationsAfter(supermarketDom, quickConsumeCallStart, [
            { method: "quickConsumeSuperItem", id: "10", payload: { allowNegativeStock: false } },
            { method: "quickConsumeSuperItem", id: "10", payload: { allowNegativeStock: true } }
        ]);

        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("history", movementRow, "10"));
        assert.equal(supermarketDom.api.calls.at(-1).method, "superStockMovements");
        assert.deepEqual(supermarketDom.api.calls.at(-1).filters, { itemId: "10", limit: 50 });
        assert.equal(supermarketDom.elements.get("#super-movement-history-title").textContent, "Historial reciente · Arroz");
        assert.match(supermarketDom.elements.get("#super-movement-history-table").children[0].innerHTML, /Compra/);
        assert.match(supermarketDom.elements.get("#super-movement-history-table").children[1].innerHTML, /Consumo rápido/);

        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("price-history", movementRow, "10"));
        assert.equal(supermarketDom.api.calls.at(-1).method, "superPriceObservations");
        assert.deepEqual(supermarketDom.api.calls.at(-1).filters, { itemId: "10", limit: 50 });
        assert.equal(supermarketDom.elements.get("#super-price-observation-title").textContent, "Observaciones de precio · Arroz");
        assert.equal(supermarketDom.elements.get("#super-price-observation-context-summary").textContent, "Historial filtrado para Arroz.");
        assert.equal(supermarketDom.elements.get("#super-price-observation-global-reset").hidden, false);
        assert.equal(supermarketDom.elements.get("#super-price-observation-table").children.length, 1);
        assert.match(supermarketDom.elements.get("#super-price-observation-table").children[0].innerHTML, /Arroz/);
        assert.doesNotMatch(supermarketDom.elements.get("#super-price-observation-table").children[0].innerHTML, /Banana/);

        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("price-history", movementRow, "12"));
        assert.deepEqual(supermarketDom.api.calls.at(-1).filters, { itemId: "12", limit: 50 });
        assert.equal(supermarketDom.elements.get("#super-price-observation-title").textContent, "Observaciones de precio · Zanahoria");
        assert.equal(supermarketDom.elements.get("#super-price-observation-empty").textContent, "Todavía no hay observaciones de precio recientes para Zanahoria.");
        assert.equal(supermarketDom.elements.get("#super-price-observation-global-reset").hidden, false);
        assert.equal(supermarketDom.elements.get("#super-price-observation-table").children.length, 0);

        await supermarketDom.elements.get("#super-price-observation-global-reset").click();
        assert.equal(supermarketDom.api.calls.at(-1).method, "superPriceObservations");
        assert.deepEqual(supermarketDom.api.calls.at(-1).filters, { limit: 50 });
        assert.equal(supermarketDom.elements.get("#super-price-observation-title").textContent, "Observaciones de precio");
        assert.equal(supermarketDom.elements.get("#super-price-observation-context-summary").textContent, "Historial reciente global de observaciones de precio.");
        assert.equal(supermarketDom.elements.get("#super-price-observation-global-reset").hidden, true);
        assert.equal(supermarketDom.elements.get("#super-price-observation-table").children.length, 2);
        assert.match(supermarketDom.elements.get("#super-price-observation-table").children[0].innerHTML, /Arroz/);
        assert.match(supermarketDom.elements.get("#super-price-observation-table").children[1].innerHTML, /Banana/);

        const superPriceObservations = supermarketDom.api.superPriceObservations;
        supermarketDom.api.superPriceObservations = async (filters = {}) => {
            supermarketDom.api.calls.push({ method: "superPriceObservations", filters });
            throw new Error("Servicio no disponible");
        };
        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("price-history", movementRow, "10"));
        assert.deepEqual(supermarketDom.api.calls.at(-1).filters, { itemId: "10", limit: 50 });
        assert.equal(supermarketDom.elements.get("#super-price-observation-title").textContent, "Observaciones de precio · Arroz");
        assert.equal(supermarketDom.elements.get("#super-price-observation-context-summary").textContent, "Historial filtrado para Arroz.");
        assert.equal(supermarketDom.elements.get("#super-price-observation-global-reset").hidden, false);
        assert.equal(supermarketDom.elements.get("#super-price-observation-empty").hidden, false);
        assert.equal(supermarketDom.elements.get("#super-price-observation-empty").textContent, "No se pudieron cargar las observaciones de Arroz: Servicio no disponible");
        assert.equal(supermarketDom.elements.get("#super-price-observation-table").children.length, 0);
        supermarketDom.api.superPriceObservations = superPriceObservations;
        await supermarketDom.elements.get("#super-price-observation-global-reset").click();
        assert.deepEqual(supermarketDom.api.calls.at(-1).filters, { limit: 50 });
        assert.equal(supermarketDom.elements.get("#super-price-observation-title").textContent, "Observaciones de precio");
        assert.equal(supermarketDom.elements.get("#super-price-observation-context-summary").textContent, "Historial reciente global de observaciones de precio.");
        assert.equal(supermarketDom.elements.get("#super-price-observation-global-reset").hidden, true);
        assert.equal(supermarketDom.elements.get("#super-price-observation-table").children.length, 2);
        assert.match(supermarketDom.elements.get("#super-price-observation-table").children[0].innerHTML, /Arroz/);
        assert.match(supermarketDom.elements.get("#super-price-observation-table").children[1].innerHTML, /Banana/);

        supermarketDom.elements.get("#super-category-name").value = "  Limpieza ";
        const createCategoryCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-category-form").submit();
        assertSupermarketMutationAfter(supermarketDom, createCategoryCallStart, {
            method: "createSuperCategory",
            payload: { name: "Limpieza", active: true }
        });
        assert.equal(supermarketDom.elements.get("#super-category-form").resetCount, 1);

        const categoryRow = supermarketDom.elements.get("#super-category-list").children[0];
        await supermarketDom.elements.get("#super-category-list").clickTarget(fakeSuperCategoryActionButton("edit", categoryRow, "4"));
        const categoryEditRow = supermarketDom.elements.get("#super-category-list").children[0];
        assert.match(categoryEditRow.innerHTML, /super-category-edit-4/);
        assertResponsiveCardLabels(categoryEditRow.innerHTML, ["Categoría", "Acciones"]);
        categoryEditRow.querySelector = (selector) => {
            assert.equal(selector, "input[name='name']");
            return fakeInput("Almacén actualizado");
        };
        const updateCategoryCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-category-list").clickTarget(fakeSuperCategoryActionButton("save", categoryEditRow, "4"));
        assertSupermarketMutationAfter(supermarketDom, updateCategoryCallStart, {
            method: "updateSuperCategory",
            id: 4,
            payload: { name: "Almacén actualizado", active: true }
        });

        const deleteCategoryCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-category-list").clickTarget(fakeSuperCategoryActionButton("delete", categoryEditRow, "4"));
        assert.equal(supermarketConfirmMessages.at(-1), "¿Seguro que querés eliminar esta categoría de la lista del super?");
        assertSupermarketMutationAfter(supermarketDom, deleteCategoryCallStart, { method: "deleteSuperCategory", id: 4 });

        supermarketDom.elements.get("#super-item-name").value = "  Huevos ";
        supermarketDom.elements.get("#super-item-category").value = "4";
        supermarketDom.elements.get("#super-item-unit").value = "  unidad ";
        supermarketDom.elements.get("#super-item-presentation-label").value = " Maple x 30 ";
        supermarketDom.elements.get("#super-item-presentation-quantity").value = "30";
        supermarketDom.elements.get("#super-item-presentation-price-pesos").value = "1250.50";
        supermarketDom.elements.get("#super-item-presentation-price-source").value = "7";
        supermarketDom.elements.get("#super-item-presentation-price-source").change?.();
        supermarketDom.elements.get("#super-item-presentation-price-observed-date").value = "2026-07-18";
        supermarketDom.elements.get("#super-item-objective").value = "12";
        supermarketDom.elements.get("#super-item-quick-quantity").value = "6";
        supermarketDom.elements.get("#super-item-current-stock").value = "9";
        supermarketDom.elements.get("#super-item-notes").value = " Maples ";
        const createItemCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-item-form").submit();
        assertSupermarketMutationsAfter(supermarketDom, createItemCallStart, [
            {
                method: "createSuperItem",
                payload: { name: "Huevos", categoryId: 4, checked: false, notes: "Maples", unit: "unidad", habitualObjective: "12", quickQuantity: "6", commercialPresentationLabel: "Maple x 30", commercialPresentationQuantity: "30", commercialPresentationPricePesos: "1250.50", commercialPresentationPriceSourceId: 7, commercialPresentationPriceObservedDate: "2026-07-18" }
            },
            { method: "adjustSuperItemStock", id: 99, currentStock: "9" }
        ]);
        assert.equal(supermarketDom.elements.get("#super-item-form").resetCount, 1);

        const originalAdjustSuperItemStock = supermarketDom.api.adjustSuperItemStock;
        supermarketDom.api.adjustSuperItemStock = async (id, currentStock) => {
            supermarketDom.api.calls.push({ method: "adjustSuperItemStock", id, currentStock });
            throw new Error("Stock API no disponible");
        };
        supermarketDom.elements.get("#super-item-name").value = "  Café ";
        supermarketDom.elements.get("#super-item-category").value = "4";
        supermarketDom.elements.get("#super-item-unit").value = "  paquete ";
        supermarketDom.elements.get("#super-item-objective").value = "2";
        supermarketDom.elements.get("#super-item-quick-quantity").value = "1";
        supermarketDom.elements.get("#super-item-current-stock").value = "5";
        supermarketDom.elements.get("#super-item-notes").value = "  Molido ";
        const partialCreateCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-item-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(partialCreateCallStart).map((call) => call.method), [
            "createSuperItem",
            "adjustSuperItemStock",
            "superCategories",
            "superItems",
            "superSuggestedList",
            "superPriceSources",
            "activeSuperScanSession",
            "superPriceObservations",
            "superStockMovements"
        ]);
        assert.deepEqual(supermarketDom.api.calls.slice(partialCreateCallStart, partialCreateCallStart + 2), [
            {
                method: "createSuperItem",
                payload: { name: "Café", categoryId: 4, checked: false, notes: "Molido", unit: "paquete", habitualObjective: "2", quickQuantity: "1" }
            },
            { method: "adjustSuperItemStock", id: 99, currentStock: "5" }
        ]);
        assert.equal(supermarketDom.elements.get("#super-feedback").textContent, "Producto guardado, pero no se pudo ajustar el stock: Stock API no disponible");
        assert.equal(supermarketDom.elements.get("#super-feedback").classList.contains("error-text"), true);
        supermarketDom.api.adjustSuperItemStock = originalAdjustSuperItemStock;

        const originalSuperItems = supermarketDom.api.superItems;
        supermarketDom.api.adjustSuperItemStock = async (id, currentStock) => {
            supermarketDom.api.calls.push({ method: "adjustSuperItemStock", id, currentStock });
            throw new Error("Stock API no disponible");
        };
        supermarketDom.api.superItems = async () => {
            supermarketDom.api.calls.push({ method: "superItems" });
            throw new Error("Refresh API caída");
        };
        supermarketDom.elements.get("#super-item-name").value = "  Yerba ";
        supermarketDom.elements.get("#super-item-category").value = "4";
        supermarketDom.elements.get("#super-item-unit").value = "  paquete ";
        supermarketDom.elements.get("#super-item-objective").value = "2";
        supermarketDom.elements.get("#super-item-quick-quantity").value = "1";
        supermarketDom.elements.get("#super-item-current-stock").value = "7";
        supermarketDom.elements.get("#super-item-notes").value = "  Suave ";
        const partialCreateRefreshFailureCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-item-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(partialCreateRefreshFailureCallStart).map((call) => call.method), [
            "createSuperItem",
            "adjustSuperItemStock",
            "superCategories",
            "superItems",
            "superSuggestedList",
            "superPriceSources"
        ]);
        assert.equal(supermarketDom.elements.get("#super-feedback").textContent, "Producto guardado, pero no se pudo ajustar el stock: Stock API no disponible. Además, no se pudo refrescar la lista: Refresh API caída");
        assert.doesNotMatch(supermarketDom.elements.get("#super-feedback").textContent, /No se pudo guardar el producto/);
        assert.equal(supermarketDom.elements.get("#super-feedback").classList.contains("error-text"), true);
        supermarketDom.api.adjustSuperItemStock = originalAdjustSuperItemStock;
        supermarketDom.api.superItems = originalSuperItems;

        const knownStockRow = supermarketDom.elements.get("#super-items-table").children[3];
        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("edit", knownStockRow, "11"));
        assert.equal(supermarketDom.elements.get("#super-item-name").value, "Banana");
        assert.equal(supermarketDom.elements.get("#super-item-current-stock").value, "0");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-label").value, "");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-quantity").value, "");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-pesos").value, "");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-source").value, "");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-source-label").value, "");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-observed-date").value, "");
        const legacySourceRow = supermarketDom.elements.get("#super-items-table").children[4];
        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("edit", legacySourceRow, "12"));
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-pesos").value, "800.00");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-source").value, "");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-source-label").value, "Feria barrial");
        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("edit", knownStockRow, "11"));
        supermarketDom.elements.get("#super-item-unit").value = "unidad";
        supermarketDom.elements.get("#super-item-objective").value = "6";
        supermarketDom.elements.get("#super-item-quick-quantity").value = "1";
        const updateWithoutStockAdjustmentCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-item-form").submit();
        assertSupermarketMutationAfter(supermarketDom, updateWithoutStockAdjustmentCallStart, {
            method: "updateSuperItem",
            id: 11,
            payload: { name: "Banana", categoryId: 5, checked: true, notes: "", unit: "unidad", habitualObjective: "6", quickQuantity: "1" }
        });

        await supermarketDom.elements.get("#super-generate-list").click();
        assert.equal(supermarketDom.elements.get("#super-generated-list").textContent, "Lista del super\n\nAlmacén\n- Arroz (1.000 kg) — Doble carolina\n\nVerdulería\n- Banana");
        assert.equal(supermarketDom.elements.get("#super-copy-list").disabled, false);

        const checkedItemCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-items-table").changeTarget(fakeSuperItemCheckedCheckbox("10", false));
        assertSupermarketMutationAfter(supermarketDom, checkedItemCallStart, { method: "updateSuperItemChecked", id: "10", checked: false });
        assert.equal(supermarketDom.elements.get("#super-generated-list").textContent, "Generá la lista para ver los productos marcados actuales.");
        assert.equal(supermarketDom.elements.get("#super-copy-list").disabled, true);

        await supermarketDom.elements.get("#super-generate-list").click();
        assert.equal(supermarketDom.elements.get("#super-copy-list").disabled, false);

        await supermarketDom.elements.get("#super-copy-list").click();
        assert.match(supermarketDom.copiedText, /Lista del super/);

        await supermarketDom.elements.get("#super-whatsapp-list").click();
        assert.match(supermarketDom.openedUrl, /^https:\/\/wa\.me\/\?text=/);
        assert.match(supermarketDom.openedUrl, /Arroz/);

        const row = supermarketDom.elements.get("#super-items-table").children[1];
        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("edit", row, "10"));
        assert.equal(supermarketDom.elements.get("#super-item-name").value, "Arroz");
        assert.equal(supermarketDom.elements.get("#super-item-unit").value, "kg");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-label").value, "Pack x 6");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-quantity").value, "6.000");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-pesos").value, "1250.50");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-source").value, "7");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-source-label").value, "");
        assert.equal(supermarketDom.elements.get("#super-item-presentation-price-observed-date").value, "2026-07-18");
        assert.equal(supermarketDom.elements.get("#super-item-objective").value, "2.000");
        assert.equal(supermarketDom.elements.get("#super-item-quick-quantity").value, "1.000");
        assert.equal(supermarketDom.elements.get("#super-item-current-stock").value, "");
        assert.equal(supermarketDom.elements.get("#super-item-submit").textContent, "Guardar producto");

        supermarketDom.elements.get("#super-item-name").value = "Arroz integral";
        supermarketDom.elements.get("#super-item-unit").value = "kg";
        supermarketDom.elements.get("#super-item-presentation-label").value = "Pack x 8";
        supermarketDom.elements.get("#super-item-presentation-quantity").value = "8.000";
        supermarketDom.elements.get("#super-item-presentation-price-pesos").value = "1499.99";
        supermarketDom.elements.get("#super-item-presentation-price-source").value = "8";
        supermarketDom.elements.get("#super-item-presentation-price-source").change?.();
        supermarketDom.elements.get("#super-item-presentation-price-observed-date").value = "2026-07-17";
        supermarketDom.elements.get("#super-item-objective").value = "3.000";
        supermarketDom.elements.get("#super-item-quick-quantity").value = "2.000";
        supermarketDom.elements.get("#super-item-current-stock").value = "4.000";
        const updateItemCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-item-form").submit();
        assertSupermarketMutationsAfter(supermarketDom, updateItemCallStart, [
            {
                method: "updateSuperItem",
                id: 10,
                payload: { name: "Arroz integral", categoryId: 4, checked: true, notes: "Doble carolina", unit: "kg", habitualObjective: "3.000", quickQuantity: "2.000", commercialPresentationLabel: "Pack x 8", commercialPresentationQuantity: "8.000", commercialPresentationPricePesos: "1499.99", commercialPresentationPriceSourceId: 8, commercialPresentationPriceObservedDate: "2026-07-17" }
            },
            { method: "adjustSuperItemStock", id: 10, currentStock: "4.000" }
        ]);

        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("edit", row, "10"));
        supermarketDom.api.adjustSuperItemStock = async (id, currentStock) => {
            supermarketDom.api.calls.push({ method: "adjustSuperItemStock", id, currentStock });
            throw new Error("Timeout de stock");
        };
        supermarketDom.elements.get("#super-item-name").value = "Arroz doble";
        supermarketDom.elements.get("#super-item-unit").value = "kg";
        supermarketDom.elements.get("#super-item-objective").value = "4.000";
        supermarketDom.elements.get("#super-item-quick-quantity").value = "2.500";
        supermarketDom.elements.get("#super-item-current-stock").value = "6.000";
        const partialUpdateCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-item-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(partialUpdateCallStart).map((call) => call.method), [
            "updateSuperItem",
            "adjustSuperItemStock",
            "superCategories",
            "superItems",
            "superSuggestedList",
            "superPriceSources",
            "activeSuperScanSession",
            "superPriceObservations",
            "superStockMovements"
        ]);
        assert.deepEqual(supermarketDom.api.calls.slice(partialUpdateCallStart, partialUpdateCallStart + 2), [
            {
                method: "updateSuperItem",
                id: 10,
                payload: { name: "Arroz doble", categoryId: 4, checked: true, notes: "Doble carolina", unit: "kg", habitualObjective: "4.000", quickQuantity: "2.500", commercialPresentationLabel: "Pack x 6", commercialPresentationQuantity: "6.000", commercialPresentationPricePesos: "1250.50", commercialPresentationPriceSourceId: 7, commercialPresentationPriceObservedDate: "2026-07-18" }
            },
            { method: "adjustSuperItemStock", id: 10, currentStock: "6.000" }
        ]);
        assert.equal(supermarketDom.elements.get("#super-feedback").textContent, "Producto guardado, pero no se pudo ajustar el stock: Timeout de stock");
        assert.equal(supermarketDom.elements.get("#super-feedback").classList.contains("error-text"), true);
        supermarketDom.api.adjustSuperItemStock = originalAdjustSuperItemStock;

        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("edit", row, "10"));
        supermarketDom.api.adjustSuperItemStock = async (id, currentStock) => {
            supermarketDom.api.calls.push({ method: "adjustSuperItemStock", id, currentStock });
            throw new Error("Timeout de stock");
        };
        supermarketDom.api.superItems = async () => {
            supermarketDom.api.calls.push({ method: "superItems" });
            throw new Error("Refresh API caída");
        };
        supermarketDom.elements.get("#super-item-name").value = "Arroz triple";
        supermarketDom.elements.get("#super-item-unit").value = "kg";
        supermarketDom.elements.get("#super-item-objective").value = "5.000";
        supermarketDom.elements.get("#super-item-quick-quantity").value = "3.000";
        supermarketDom.elements.get("#super-item-current-stock").value = "8.000";
        const partialUpdateRefreshFailureCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-item-form").submit();
        assert.deepEqual(supermarketDom.api.calls.slice(partialUpdateRefreshFailureCallStart).map((call) => call.method), [
            "updateSuperItem",
            "adjustSuperItemStock",
            "superCategories",
            "superItems",
            "superSuggestedList",
            "superPriceSources"
        ]);
        assert.equal(supermarketDom.elements.get("#super-feedback").textContent, "Producto guardado, pero no se pudo ajustar el stock: Timeout de stock. Además, no se pudo refrescar la lista: Refresh API caída");
        assert.doesNotMatch(supermarketDom.elements.get("#super-feedback").textContent, /No se pudo guardar el producto/);
        assert.equal(supermarketDom.elements.get("#super-feedback").classList.contains("error-text"), true);
        supermarketDom.api.adjustSuperItemStock = originalAdjustSuperItemStock;
        supermarketDom.api.superItems = originalSuperItems;

        const deleteItemCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("delete", row, "10"));
        assert.equal(supermarketConfirmMessages.at(-1), "¿Seguro que querés eliminar este producto de la lista del super?");
        assertSupermarketMutationAfter(supermarketDom, deleteItemCallStart, { method: "deleteSuperItem", id: "10" });

        const uncheckAllCallStart = supermarketDom.api.calls.length;
        await supermarketDom.elements.get("#super-uncheck-all").click();
        assert.equal(supermarketConfirmMessages.at(-1), "¿Querés desmarcar todos los productos?");
        assertSupermarketMutationAfter(supermarketDom, uncheckAllCallStart, { method: "uncheckAllSuperItems" });
        assert.equal(supermarketDom.elements.get("#super-generated-list").textContent, "Generá la lista para ver los productos marcados actuales.");
        assert.equal(supermarketDom.elements.get("#super-download-list").disabled, true);

        const degradedSupermarketDom = fakeSupermarketDom();
        const degradedWarnings = [];
        const previousConsole = globalThis.console;
        degradedSupermarketDom.api.superPriceSources = async () => {
            degradedSupermarketDom.api.calls.push({ method: "superPriceSources" });
            throw new Error("Price source API caída");
        };
        globalThis.document = degradedSupermarketDom.document;
        globalThis.console = {
            ...(previousConsole || {}),
            warn(...args) {
                degradedWarnings.push(args);
                previousConsole?.warn?.(...args);
            }
        };
        setupSupermarket({ apiClient: degradedSupermarketDom.api });
        await flushAsyncWork();
        assert.deepEqual(degradedSupermarketDom.api.calls.slice(0, 7), [
            { method: "superCategories" },
            { method: "superItems" },
            { method: "superSuggestedList" },
            { method: "superPriceSources" },
            { method: "activeSuperScanSession" },
            { method: "superPriceObservations", filters: { limit: 50 } },
            { method: "superStockMovements", filters: { limit: 50 } }
        ]);
        assert.match(degradedSupermarketDom.elements.get("#super-category-list").children[0].innerHTML, /Almacén/);
        assert.match(degradedSupermarketDom.elements.get("#super-items-table").children[1].innerHTML, /Arroz/);
        assert.equal(degradedSupermarketDom.elements.get("#super-suggested-summary").textContent, "1 producto sugerido para reponer.");
        assert.equal(degradedSupermarketDom.elements.get("#super-feedback").textContent, "Lista del super cargada. Fuentes de precio no disponibles por ahora.");
        assert.equal(degradedSupermarketDom.elements.get("#super-price-observation-price-source").innerHTML, '<option value="">Sin fuente reutilizable</option>');
        assert.equal(degradedSupermarketDom.elements.get("#super-price-source-feedback").textContent, "");
        const degradedRow = degradedSupermarketDom.elements.get("#super-items-table").children[1];
        await degradedSupermarketDom.elements.get("#super-items-table").clickTarget(fakeSuperItemActionButton("edit", degradedRow, "10"));
        assert.equal(degradedSupermarketDom.elements.get("#super-item-presentation-price-source").value, "7");
        assert.match(degradedSupermarketDom.elements.get("#super-item-presentation-price-source").innerHTML, /Ticket proveedor/);
        assert.equal(degradedSupermarketDom.elements.get("#super-item-presentation-price-source-label").value, "");
        degradedSupermarketDom.elements.get("#super-item-name").value = "Arroz fino";
        const degradedUpdateCallStart = degradedSupermarketDom.api.calls.length;
        await degradedSupermarketDom.elements.get("#super-item-form").submit();
        assertSupermarketMutationAfter(degradedSupermarketDom, degradedUpdateCallStart, {
            method: "updateSuperItem",
            id: 10,
            payload: { name: "Arroz fino", categoryId: 4, checked: true, notes: "Doble carolina", unit: "kg", habitualObjective: "2.000", quickQuantity: "1.000", commercialPresentationLabel: "Pack x 6", commercialPresentationQuantity: "6.000", commercialPresentationPricePesos: "1250.50", commercialPresentationPriceSourceId: 7, commercialPresentationPriceObservedDate: "2026-07-18" }
        });
        assert.equal(degradedWarnings.length, 2);
        assert.match(String(degradedWarnings[0][0]), /Price sources unavailable during initial-load/);
        assert.match(String(degradedWarnings[0][1]?.message), /Price source API caída/);
        assert.match(String(degradedWarnings[1][0]), /Price sources unavailable during initial-load/);
        assert.match(String(degradedWarnings[1][1]?.message), /Price source API caída/);
        globalThis.console = previousConsole;
        globalThis.document = supermarketDom.document;
    } finally {
        if (previousSupermarketDocument === undefined) {
            delete globalThis.document;
        } else {
            globalThis.document = previousSupermarketDocument;
        }
        if (previousSupermarketConfirm === undefined) {
            delete globalThis.confirm;
        } else {
            globalThis.confirm = previousSupermarketConfirm;
        }
        Object.defineProperty(globalThis, "navigator", {
            configurable: true,
            value: previousSupermarketNavigator
        });
        if (previousSupermarketOpen === undefined) {
            delete globalThis.open;
        } else {
            globalThis.open = previousSupermarketOpen;
        }
        if (previousSupermarketConsole === undefined) {
            delete globalThis.console;
        } else {
            globalThis.console = previousSupermarketConsole;
        }
        if (previousSupermarketBarcodeDetector === undefined) {
            delete globalThis.BarcodeDetector;
        } else {
            globalThis.BarcodeDetector = previousSupermarketBarcodeDetector;
        }
        if (previousSupermarketSecureContext === undefined) {
            delete globalThis.isSecureContext;
        } else {
            globalThis.isSecureContext = previousSupermarketSecureContext;
        }
        if (previousSupermarketAddEventListener === undefined) {
            delete globalThis.addEventListener;
        } else {
            globalThis.addEventListener = previousSupermarketAddEventListener;
        }
        if (previousSupermarketRequestAnimationFrame === undefined) {
            delete globalThis.requestAnimationFrame;
        } else {
            globalThis.requestAnimationFrame = previousSupermarketRequestAnimationFrame;
        }
        if (previousSupermarketCancelAnimationFrame === undefined) {
            delete globalThis.cancelAnimationFrame;
        } else {
            globalThis.cancelAnimationFrame = previousSupermarketCancelAnimationFrame;
        }
    }

    assert.deepEqual(simulationPayloadFromValues({
        description: " Notebook ",
        totalAmount: "1200",
        installmentCount: "6",
        startMonth: "2026-11",
        categoryId: "4"
    }), {
        description: "Notebook",
        totalAmount: "1200",
        installmentCount: 6,
        startMonth: "2026-11",
        categoryId: 4
    });
    assert.equal(validateSimulationPayload(simulationPayloadFromValues({ totalAmount: "", installmentCount: "6", startMonth: "2026-07" })), "El importe total es obligatorio.");
    assert.equal(validateSimulationPayload(simulationPayloadFromValues({ totalAmount: "1200", installmentCount: "", startMonth: "2026-07" })), "La cantidad de cuotas es obligatoria.");
    assert.equal(validateSimulationPayload(simulationPayloadFromValues({ totalAmount: "1200", installmentCount: "0", startMonth: "2026-07" })), "La cantidad de cuotas debe ser mayor que cero.");
    assert.equal(validateSimulationPayload(simulationPayloadFromValues({ totalAmount: "1200", installmentCount: "61", startMonth: "2026-07" })), "La cantidad de cuotas no puede superar 60.");
    assert.equal(validateSimulationPayload(simulationPayloadFromValues({ totalAmount: "1200", installmentCount: "6", startMonth: "" })), "El mes de inicio es obligatorio.");
    assert.equal(validateSimulationPayload(simulationPayloadFromValues({ totalAmount: "1200", installmentCount: String(MAX_SIMULATOR_INSTALLMENTS), startMonth: "2026-07" })), "");
    assert.equal(validateSimulationPayload(simulationPayloadFromValues({ totalAmount: "1200", installmentCount: "6", startMonth: "2026-07" })), "");
    assert.equal(calculateMonthlyInstallment("1200", 6), 200);
    assert.deepEqual(affectedMonths("2026-11", 3), ["2026-11", "2026-12", "2027-01"]);
    assert.equal(affectedMonths("2026-11", MAX_SIMULATOR_INSTALLMENTS).length, MAX_SIMULATOR_INSTALLMENTS);
    assert.equal(affectedMonths("2026-11", MAX_SIMULATOR_INSTALLMENTS).at(-1), "2031-10");
    assert.deepEqual(buildSimulationRows({ totalAmount: "1200", installmentCount: 3, startMonth: "2026-11" }, [
        { incomeTotalPesos: 1000, expenseTotalPesos: 300, monthlyBalancePesos: 700 },
        { incomeTotalPesos: 1500, totalPesos: 500 },
        { incomeTotalPesos: 900, expenseTotalPesos: 100, monthlyBalancePesos: 800 }
    ]), [
        { month: "2026-11", monthlyIncome: 1000, currentExpenses: 300, simulatedInstallment: 400, currentBalance: 700, simulatedBalance: 300 },
        { month: "2026-12", monthlyIncome: 1500, currentExpenses: 500, simulatedInstallment: 400, currentBalance: 1000, simulatedBalance: 600 },
        { month: "2027-01", monthlyIncome: 900, currentExpenses: 100, simulatedInstallment: 400, currentBalance: 800, simulatedBalance: 400 }
    ]);

    const manualExpenseDom = fakeManualExpenseDom();
    const previousManualExpenseDocument = globalThis.document;
    const previousManualExpenseConfirm = globalThis.confirm;
    globalThis.document = manualExpenseDom.document;
    globalThis.confirm = () => true;
    try {
        let manualExpenseRefreshCount = 0;
        setupManualExpenses({
            apiClient: manualExpenseDom.api,
            onChanged: async () => {
                manualExpenseRefreshCount += 1;
                const expenses = await manualExpenseDom.api.manualExpenses({ month: "2026-07" });
                renderManualExpenses(expenses, "2026-07");
            }
        });
        setManualExpenseCategories([{ id: 4, name: "Préstamos" }]);
        assert.match(manualExpenseDom.elements.get("#manual-expense-category").innerHTML, /Préstamos/);

        manualExpenseDom.elements.get("#manual-expense-description").value = "  Préstamo personal ";
        manualExpenseDom.elements.get("#manual-expense-type").value = "LOAN";
        manualExpenseDom.elements.get("#manual-expense-amount-pesos").value = "1000.50";
        manualExpenseDom.elements.get("#manual-expense-amount-usd").value = "5.25";
        manualExpenseDom.elements.get("#manual-expense-start-month").value = "2026-07";
        manualExpenseDom.elements.get("#manual-expense-total-installments").value = "6";
        manualExpenseDom.elements.get("#manual-expense-current-installment").value = "2";
        manualExpenseDom.elements.get("#manual-expense-category").value = "4";
        manualExpenseDom.elements.get("#manual-expense-notes").value = "  Banco ";
        await manualExpenseDom.elements.get("#manual-expense-form").submit();
        assert.deepEqual(manualExpenseDom.api.calls.at(-2), {
            method: "createManualExpense",
            payload: {
                description: "Préstamo personal",
                type: "LOAN",
                amountPesos: "1000.50",
                amountUsd: "5.25",
                startMonth: "2026-07",
                totalInstallments: 6,
                currentInstallment: 2,
                categoryId: 4,
                notes: "Banco"
            }
        });
        assert.deepEqual(manualExpenseDom.api.calls.at(-1), { method: "manualExpenses", filters: { month: "2026-07" } });
        assert.equal(manualExpenseDom.elements.get("#manual-expense-form").resetCount, 1);
        assert.equal(manualExpenseDom.elements.get("#manual-expense-description").value, "");
        assert.equal(manualExpenseDom.elements.get("#manual-expense-type").value, "ONE_PAYMENT");
        assert.equal(manualExpenseRefreshCount, 1);
        assert.equal(manualExpenseDom.elements.get("#manual-expense-feedback").textContent, "Gasto manual creado correctamente.");
        assert.equal(manualExpenseDom.elements.get("#manual-expenses-table").children.length, 2);
        assert.match(manualExpenseDom.elements.get("#manual-expenses-table").children[0].innerHTML, /Préstamo personal/);
        assertResponsiveCardLabels(manualExpenseDom.elements.get("#manual-expenses-table").children[0].innerHTML, ["Mes", "Descripción", "Tipo", "Cuota", "Categoría", "Pesos", "USD", "Estado", "Notas", "Acciones"]);
        assert.equal(manualExpenseDom.elements.get("#manual-expenses-summary").textContent, "2 gastos manuales para jul 2026.");

        await manualExpenseDom.elements.get("#manual-expenses-table").children[0].deleteButton.click();
        assert.deepEqual(manualExpenseDom.api.calls.at(-2), { method: "deleteManualExpense", id: 77 });
        assert.deepEqual(manualExpenseDom.api.calls.at(-1), { method: "manualExpenses", filters: { month: "2026-07" } });
        assert.equal(manualExpenseRefreshCount, 2);
        assert.equal(manualExpenseDom.elements.get("#manual-expense-feedback").textContent, "Gasto manual eliminado.");
    } finally {
        if (previousManualExpenseDocument === undefined) {
            delete globalThis.document;
        } else {
            globalThis.document = previousManualExpenseDocument;
        }
        if (previousManualExpenseConfirm === undefined) {
            delete globalThis.confirm;
        } else {
            globalThis.confirm = previousManualExpenseConfirm;
        }
    }

    const simulatorDom = fakeSimulatorDom();
    const previousSimulatorDocument = globalThis.document;
    globalThis.document = simulatorDom.document;
    try {
        setupSimulator({ apiClient: simulatorDom.api });
        setSimulatorCategories([{ id: 4, name: "Tecnología" }]);
        assert.match(simulatorDom.elements.get("#simulator-category").innerHTML, /Tecnología/);

        await simulatorDom.elements.get("#simulator-form").submit();
        assert.equal(simulatorDom.api.calls.length, 0);
        assert.equal(simulatorDom.elements.get("#simulator-feedback").textContent, "El importe total es obligatorio.");

        simulatorDom.elements.get("#simulator-description").value = " Notebook ";
        simulatorDom.elements.get("#simulator-total-amount").value = "1200";
        simulatorDom.elements.get("#simulator-installment-count").value = "61";
        simulatorDom.elements.get("#simulator-start-month").value = "2026-11";
        await simulatorDom.elements.get("#simulator-form").submit();
        assert.equal(simulatorDom.api.calls.length, 0);
        assert.equal(simulatorDom.elements.get("#simulator-feedback").textContent, "La cantidad de cuotas no puede superar 60.");

        simulatorDom.elements.get("#simulator-installment-count").value = "3";
        simulatorDom.elements.get("#simulator-start-month").value = "2026-11";
        simulatorDom.elements.get("#simulator-category").value = "4";
        await simulatorDom.elements.get("#simulator-form").submit();
        assert.deepEqual(simulatorDom.api.calls, [
            { method: "summary", month: "2026-11" },
            { method: "summary", month: "2026-12" },
            { method: "summary", month: "2027-01" }
        ]);
        assert.equal(simulatorDom.elements.get("#simulation-results-table").children.length, 3);
        assert.match(simulatorDom.elements.get("#simulation-results-table").children[0].innerHTML, /400\.00/);
        assert.match(simulatorDom.elements.get("#simulation-results-table").children[0].innerHTML, /class="simulation-month-cell"/);
        assert.match(simulatorDom.elements.get("#simulation-results-table").children[0].innerHTML, /class="amount simulation-amount-cell"/);
        assertResponsiveCardLabels(simulatorDom.elements.get("#simulation-results-table").children[0].innerHTML, ["Mes", "Ingresos del mes", "Deuda/gastos actuales del mes", "Nueva cuota simulada", "Saldo actual sin simulación", "Saldo final con simulación"]);
        assert.match(simulatorDom.elements.get("#simulator-summary").textContent, /3 meses afectados para Notebook/);
        assert.equal(simulatorDom.elements.get("#simulator-feedback").textContent, "Simulación calculada. No se guardó en la base de datos.");

        await simulatorDom.elements.get("#clear-simulation").click();
        assert.equal(simulatorDom.elements.get("#simulator-form").resetCount, 1);
        assert.equal(simulatorDom.elements.get("#simulation-results-table").children.length, 0);
        assert.equal(simulatorDom.elements.get("#simulator-summary").textContent, "No hay una simulación activa.");
        assert.equal(simulatorDom.elements.get("#simulator-feedback").textContent, "Simulación limpiada.");

        const rows = await runPurchaseSimulation({ totalAmount: "600", installmentCount: 2, startMonth: "2026-11" }, simulatorDom.api);
        assert.equal(rows[0].simulatedInstallment, 300);
        renderSimulationResults(rows, { description: "Prueba" });
        assert.equal(simulatorDom.elements.get("#simulation-results-table").children.length, 2);
    } finally {
        if (previousSimulatorDocument === undefined) {
            delete globalThis.document;
        } else {
            globalThis.document = previousSimulatorDocument;
        }
    }

    const incomeDom = fakeIncomeDom();
    const previousIncomeDocument = globalThis.document;
    const previousConfirm = globalThis.confirm;
    const confirmMessages = [];
    globalThis.document = incomeDom.document;
    globalThis.confirm = (message) => {
        confirmMessages.push(message);
        return true;
    };
    try {
        let dashboardRefreshCount = 0;
        setupIncomes({
            apiClient: incomeDom.api,
            onChanged: async () => {
                dashboardRefreshCount += 1;
            }
        });
        assert.equal(incomeDom.elements.get("#income-filter-month").value, "");

        await loadIncomes();
        assert.deepEqual(incomeDom.api.calls[0], { method: "incomes", filters: undefined });
        assert.equal(incomeDom.elements.get("#incomes-table").children.length, 2);
        assert.match(incomeDom.elements.get("#incomes-table").children[0].innerHTML, /Sueldo principal/);
        assertResponsiveCardLabels(incomeDom.elements.get("#incomes-table").children[0].innerHTML, ["Mes", "Descripción", "Tipo", "Monto", "Recurrente", "Aplica desde", "Aplica hasta", "Estado", "Notas", "Acciones"]);
        assert.doesNotMatch(incomeDom.elements.get("#incomes-table").children[0].innerHTML, /<input|<select|Guardar cambios desde/);
        assert.match(incomeDom.elements.get("#incomes-table").children[0].innerHTML, /data-income-action="edit"/);
        assert.match(incomeDom.elements.get("#incomes-table").children[0].innerHTML, /data-income-action="delete"/);
        assert.equal(incomeDom.elements.get("#income-filters-summary").textContent, "2 ingresos cargados.");

        incomeDom.elements.get("#income-filter-month").value = "2026-08";
        await incomeDom.elements.get("#income-filter-form").submit();
        assert.deepEqual(incomeDom.api.calls.at(-1), { method: "incomes", filters: { month: "2026-08" } });
        assert.equal(incomeDom.elements.get("#income-filters-summary").textContent, "1 ingreso para ago 2026.");

        incomeDom.elements.get("#income-filter-month").value = "";
        incomeDom.elements.get("#income-description").value = "  Aguinaldo ";
        incomeDom.elements.get("#income-type").value = "VARIABLE";
        incomeDom.elements.get("#income-amount").value = "1500.25";
        incomeDom.elements.get("#income-start-month").value = "2026-12";
        incomeDom.elements.get("#income-recurring-monthly").value = "false";
        incomeDom.elements.get("#income-notes").value = "  Pago anual ";
        await incomeDom.elements.get("#income-form").submit();
        assert.deepEqual(incomeDom.api.calls.at(-2), {
            method: "createIncome",
            payload: {
                description: "Aguinaldo",
                incomeType: "VARIABLE",
                amountPesos: "1500.25",
                startMonth: "2026-12",
                endMonth: "",
                recurringMonthly: false,
                notes: "Pago anual"
            }
        });
        assert.deepEqual(incomeDom.api.calls.at(-1), { method: "incomes", filters: undefined });
        assert.equal(incomeDom.elements.get("#income-form").resetCount, 1);
        assert.equal(incomeDom.elements.get("#income-description").value, "");
        assert.equal(dashboardRefreshCount, 1);
        assert.equal(incomeDom.elements.get("#income-feedback").textContent, "Ingreso creado correctamente. Tabla actualizada.");
        assert.equal(incomeDom.elements.get("#income-table-feedback").textContent, "2 ingresos cargados.");

        await loadIncomes();
        const editRow = incomeDom.elements.get("#incomes-table").children[0];
        await incomeDom.elements.get("#incomes-table").clickTarget(fakeIncomeActionButton("edit", editRow));
        assert.equal(incomeDom.elements.get("#income-edit-modal").hidden, false);
        assert.equal(incomeDom.elements.get("#income-edit-description").value, "Sueldo principal");
        assert.equal(incomeDom.elements.get("#income-edit-effective-month-group").hidden, false);
        incomeDom.elements.get("#income-edit-description").value = "Sueldo editado";
        incomeDom.elements.get("#income-edit-type").value = "SALARY";
        incomeDom.elements.get("#income-edit-amount").value = "3000";
        incomeDom.elements.get("#income-edit-start-month").value = "2026-07";
        incomeDom.elements.get("#income-edit-recurring").value = "true";
        incomeDom.elements.get("#income-edit-notes").value = "Actualizado";
        incomeDom.elements.get("#income-edit-end-month").value = "2026-06";
        const callsBeforeInvalidModalSave = incomeDom.api.calls.length;
        await incomeDom.elements.get("#income-edit-form").submit();
        assert.equal(incomeDom.api.calls.length, callsBeforeInvalidModalSave);
        assert.equal(incomeDom.elements.get("#income-edit-feedback").textContent, "El mes de aplicación hasta no puede ser anterior al mes de inicio.");
        assert.equal(incomeDom.elements.get("#income-feedback").textContent, "Ingreso creado correctamente. Tabla actualizada.");

        incomeDom.elements.get("#income-edit-end-month").value = "2026-12";
        await incomeDom.elements.get("#income-edit-form").submit();
        assert.deepEqual(incomeDom.api.calls.at(-2), {
            method: "updateIncome",
            id: "1",
            payload: {
                description: "Sueldo editado",
                incomeType: "SALARY",
                amountPesos: "3000",
                startMonth: "2026-07",
                endMonth: "2026-12",
                recurringMonthly: true,
                notes: "Actualizado"
            }
        });
        assert.equal(dashboardRefreshCount, 2);
        assert.equal(incomeDom.elements.get("#income-edit-modal").hidden, true);
        assert.equal(incomeDom.elements.get("#income-table-feedback").textContent, "Ingreso actualizado correctamente. Tabla actualizada.");

        globalThis.confirm = (message) => {
            confirmMessages.push(message);
            return false;
        };
        const callsBeforeCancelDelete = incomeDom.api.calls.length;
        await incomeDom.elements.get("#incomes-table").clickTarget(fakeIncomeActionButton("delete", editRow));
        assert.equal(incomeDom.api.calls.length, callsBeforeCancelDelete);
        assert.equal(dashboardRefreshCount, 2);
        assert.equal(confirmMessages.at(-1), "¿Seguro que desea eliminar este ingreso? Si es recurrente, se eliminará el registro completo.");

        globalThis.confirm = (message) => {
            confirmMessages.push(message);
            return true;
        };

        await incomeDom.elements.get("#incomes-table").clickTarget(fakeIncomeActionButton("delete", editRow));
        assert.deepEqual(incomeDom.api.calls.at(-2), { method: "deleteIncome", id: "1" });
        assert.equal(dashboardRefreshCount, 3);
        assert.equal(confirmMessages.at(-1), "¿Seguro que desea eliminar este ingreso? Si es recurrente, se eliminará el registro completo.");

        await loadIncomes();
        const futureRow = incomeDom.elements.get("#incomes-table").children[0];
        await incomeDom.elements.get("#incomes-table").clickTarget(fakeIncomeActionButton("edit", futureRow));
        incomeDom.elements.get("#income-edit-description").value = "Sueldo futuro";
        incomeDom.elements.get("#income-edit-type").value = "SALARY";
        incomeDom.elements.get("#income-edit-amount").value = "3200";
        incomeDom.elements.get("#income-edit-start-month").value = "2026-07";
        incomeDom.elements.get("#income-edit-end-month").value = "2026-12";
        incomeDom.elements.get("#income-edit-recurring").value = "true";
        incomeDom.elements.get("#income-edit-effective-month").value = "2026-09";
        incomeDom.elements.get("#income-edit-notes").value = "Desde septiembre";
        await incomeDom.elements.get("#income-edit-save-from-month").click();
        assert.deepEqual(incomeDom.api.calls.at(-2), {
            method: "updateIncomeFromMonth",
            id: "1",
            yearMonth: "2026-09",
            payload: {
                description: "Sueldo futuro",
                incomeType: "SALARY",
                amountPesos: "3200",
                startMonth: "2026-09",
                endMonth: "2026-12",
                recurringMonthly: true,
                notes: "Desde septiembre"
            }
        });
        assert.equal(dashboardRefreshCount, 4);
        assert.equal(incomeDom.elements.get("#income-edit-modal").hidden, true);
        assert.equal(incomeDom.elements.get("#income-table-feedback").textContent, "Ingreso recurrente actualizado desde el mes seleccionado. Tabla actualizada.");
    } finally {
        if (previousIncomeDocument === undefined) {
            delete globalThis.document;
        } else {
            globalThis.document = previousIncomeDocument;
        }
        if (previousConfirm === undefined) {
            delete globalThis.confirm;
        } else {
            globalThis.confirm = previousConfirm;
        }
    }

    assert.equal(DEFAULT_PRIMARY_TAB_ID, "summary");
    assert.equal(DEFAULT_ROUTE_HASH, "#monthly/summary");
    assert.deepEqual(parseHash(""), { domain: "monthly", section: "summary", hash: "#monthly/summary", primaryPanel: "summary", focusSelector: "#summary-home-title" });
    assert.deepEqual(parseHash("#stock"), { domain: "stock", section: "list", hash: "#stock/list", primaryPanel: "supermarket", supermarketTab: "list", focusSelector: "#supermarket-title" });
    assert.deepEqual(parseHash("#invalid/route"), { domain: "monthly", section: "summary", hash: "#monthly/summary", primaryPanel: "summary", focusSelector: "#summary-home-title" });
    assert.deepEqual(parseHash("#supermarket"), { domain: "monthly", section: "summary", hash: "#monthly/summary", primaryPanel: "summary", focusSelector: "#summary-home-title" });

    const supermarketTabDom = fakeGenericTabDom(["list", "barcode", "tickets", "categories"], {
        buttonSelector: "[data-super-tab-target]",
        panelSelector: "[data-super-tab-panel]",
        targetDatasetKey: "superTabTarget",
        panelDatasetKey: "superTabPanel"
    });
    const previousTabDocument = globalThis.document;
    globalThis.document = supermarketTabDom.document;
    try {
        setupTabs({
            buttons: [...supermarketTabDom.buttonsById.values()],
            panels: [...supermarketTabDom.sectionsById.values()],
            defaultTabId: DEFAULT_SUPERMARKET_TAB_ID,
            targetDatasetKey: "superTabTarget",
            panelDatasetKey: "superTabPanel"
        });
        assertTabState(supermarketTabDom, "list");

        supermarketTabDom.buttonsById.get("list").keydown("End");
        assertTabState(supermarketTabDom, "categories", { focusedTabId: "categories" });

        supermarketTabDom.buttonsById.get("categories").keydown("ArrowLeft");
        assertTabState(supermarketTabDom, "tickets", { focusedTabId: "tickets" });

        supermarketTabDom.buttonsById.get("tickets").keydown("Home");
        assertTabState(supermarketTabDom, "list", { focusedTabId: "list" });

        supermarketTabDom.buttonsById.get("barcode").click();
        assertTabState(supermarketTabDom, "barcode");
    } finally {
        if (previousTabDocument === undefined) {
            delete globalThis.document;
        } else {
            globalThis.document = previousTabDocument;
        }
    }

    const shellNavigationDom = fakeShellNavigationDom();
    const previousShellDocument = globalThis.document;
    const previousShellLocation = globalThis.location;
    const previousShellHistory = globalThis.history;
    globalThis.document = shellNavigationDom.document;
    globalThis.location = shellNavigationDom.location;
    globalThis.history = shellNavigationDom.history;
    try {
        setupPrimaryTabs();
        let route = syncPrimaryRouteFromLocation({ replace: true });
        assert.deepEqual(route, { domain: "monthly", section: "summary", hash: "#monthly/summary", primaryPanel: "summary", focusSelector: "#summary-home-title" });
        assert.equal(shellNavigationDom.history.calls.at(-1).hash, "#monthly/summary");
        assert.equal(shellNavigationDom.primaryPanels.get("summary").hidden, false);
        assert.equal(shellNavigationDom.primaryPanels.get("supermarket").hidden, true);

        await shellNavigationDom.trigger.click();
        assert.equal(shellNavigationDom.drawer.hidden, false);
        assert.equal(shellNavigationDom.trigger.attributes.get("aria-expanded"), "true");
        assert.equal(shellNavigationDom.routeLinks[0].focused, true);

        shellNavigationDom.document.dispatchEvent({ type: "keydown", key: "Escape" });
        assert.equal(shellNavigationDom.drawer.hidden, true);
        assert.equal(shellNavigationDom.trigger.focused, true);

        await shellNavigationDom.trigger.click();
        await shellNavigationDom.linkByHash.get("#stock/barcode").click();
        assert.equal(shellNavigationDom.location.hash, "#stock/barcode");
        assert.equal(shellNavigationDom.primaryPanels.get("supermarket").hidden, false);
        assertTabState(shellNavigationDom.supermarketTabs, "barcode");
        assert.equal(shellNavigationDom.routeHeadings.get("#super-barcode-title").focused, true);
        assert.equal(shellNavigationDom.drawer.hidden, true);

        shellNavigationDom.barcodeInput.value = "0075012345678";
        shellNavigationDom.routeHeadings.get("#summary-home-title").focus();
        shellNavigationDom.location.hash = "#monthly/summary";
        route = syncPrimaryRouteFromLocation();
        assert.equal(route.hash, "#monthly/summary");
        assert.equal(shellNavigationDom.routeHeadings.get("#summary-home-title").focused, true);

        shellNavigationDom.location.hash = "#stock/barcode";
        route = syncPrimaryRouteFromLocation();
        assert.equal(route.hash, "#stock/barcode");
        assertTabState(shellNavigationDom.supermarketTabs, "barcode");
        assert.equal(shellNavigationDom.barcodeInput.value, "0075012345678");
        assert.equal(shellNavigationDom.routeHeadings.get("#summary-home-title").focused, true);

        shellNavigationDom.supermarketTabs.buttonsById.get("tickets").click();
        assert.equal(shellNavigationDom.location.hash, "#stock/tickets");
        assertTabState(shellNavigationDom.supermarketTabs, "tickets");

        shellNavigationDom.location.hash = "#stock";
        route = syncPrimaryRouteFromLocation();
        assert.equal(route.hash, "#stock/list");
        assert.equal(shellNavigationDom.location.hash, "#stock/list");
        assertTabState(shellNavigationDom.supermarketTabs, "list");

        shellNavigationDom.location.hash = "#supermarket";
        route = syncPrimaryRouteFromLocation();
        assert.equal(route.hash, "#monthly/summary");
        assert.equal(shellNavigationDom.location.hash, "#monthly/summary");
    } finally {
        if (previousShellDocument === undefined) {
            delete globalThis.document;
        } else {
            globalThis.document = previousShellDocument;
        }
        if (previousShellLocation === undefined) {
            delete globalThis.location;
        } else {
            globalThis.location = previousShellLocation;
        }
        if (previousShellHistory === undefined) {
            delete globalThis.history;
        } else {
            globalThis.history = previousShellHistory;
        }
    }

    const months = Array.from({ length: 10 }, (_, index) => ({
        yearMonth: `2026-${String(index + 1).padStart(2, "0")}`,
        currentReal: true,
        projectionOnly: false
    }));
    const visible = visibleMonthTabs("2026-10", months, 8);
    assert.equal(visible.length, 8);
    assert.equal(visible.some((month) => month.yearMonth === "2026-10"), true);

    assert.equal(monthTabLabel({ currentReal: true, hasProjectedData: true, projectionOnly: false }), ' <span class="tab-label">Confirmado</span>');
    assert.equal(monthTabLabel({ currentReal: false, hasProjectedData: true, projectionOnly: true }), ' <span class="tab-label">Proyección</span>');
    assert.equal(monthTabLabel({ currentReal: false, hasProjectedData: true, projectionOnly: false }), "");

    assert.equal(matchesTargetCard(
        { provider: "NARANJA_X", cardBrand: "VISA", cardAlias: "Naranja X VISA" },
        { title: "Naranja X", alias: "naranja", strongProviders: ["NARANJA_X"], providers: ["BANK_PORTAL", "NARANJA_X", "MANUAL"] }
    ), true);
    assert.equal(matchesTargetCard(
        { provider: "NARANJA_X", cardBrand: "VISA", cardAlias: "Visa" },
        { title: "Naranja X", alias: "naranja", strongProviders: ["NARANJA_X"], providers: ["BANK_PORTAL", "NARANJA_X", "MANUAL"] }
    ), true);

    const elements = fakeDashboardElements();
    const previousDocument = globalThis.document;
    globalThis.document = {
        createElement(tagName) {
            assert.ok(["article", "button", "tr"].includes(tagName));
            return fakeElement();
        },
        querySelector(selector) {
            assert.ok(elements.has(selector), `Unexpected selector: ${selector}`);
            return elements.get(selector);
        }
    };
    try {
        renderDashboard({
            month: "2026-07",
            summary: {
                totalPesos: 2012.38,
                totalUsd: 0,
                incomeTotalPesos: 700000,
                salaryIncomeTotalPesos: 600000,
                variableIncomeTotalPesos: 100000,
                projectedIncomeTotalPesos: 600000,
                monthlyBalancePesos: -1312682.98,
                estimated: true,
                statementCount: 2,
                transactionCount: 55,
                incomeCount: 2
            },
            statements: [
                {
                    provider: "SANTANDER",
                    cardBrand: "VISA",
                    cardAlias: "Santander Visa",
                    status: "CONFIRMED",
                    dueDate: "2026-07-10",
                    totalPesos: 2012.38,
                    totalUsd: 0,
                    minimumPaymentPesos: 2012.38,
                    transactionCount: 54
                },
                {
                    provider: "NARANJA_X",
                    cardBrand: "VISA",
                    cardAlias: "Visa",
                    status: "CONFIRMED",
                    totalPesos: 300,
                    totalUsd: 0,
                    transactionCount: 1
                }
            ],
            transactions: [],
            allStatements: [],
            months: [{ yearMonth: "2026-07", currentReal: true, projectionOnly: false }],
            monthDetail: {
                currentReal: true,
                projectionOnly: false,
                totalPesos: 2012682.98,
                totalUsd: 25.5,
                totalsByCard: [
                    {
                        provider: "SANTANDER",
                        cardBrand: "VISA",
                        cardAlias: "Santander Visa",
                        totalPesos: 2012382.98,
                        totalUsd: 25.5,
                        rowCount: 54
                    },
                    {
                        provider: "NARANJA_X",
                        cardBrand: "VISA",
                        cardAlias: "Visa",
                        totalPesos: 300,
                        totalUsd: 0,
                        rowCount: 1
                    }
                ],
                rows: []
            }
        });
    } finally {
        if (previousDocument === undefined) {
            delete globalThis.document;
        } else {
            globalThis.document = previousDocument;
        }
    }
    const cardGrid = elements.get("#card-detail-grid");
    assert.match(elements.get("#monthly-income-total").textContent, /700,000\.00/);
    assert.match(elements.get("#salary-income-total").textContent, /600,000\.00/);
    assert.match(elements.get("#variable-income-total").textContent, /100,000\.00/);
    assert.match(elements.get("#projected-income-total").textContent, /600,000\.00/);
    assert.equal(elements.get("#projected-income-card").hidden, false);
    assert.equal(elements.get("#summary-estimated-label").hidden, false);
    assert.match(elements.get("#total-pesos").textContent, /2,012,682\.98/);
    assert.match(elements.get("#monthly-balance-pesos").textContent, /1,312,682\.98/);
    assert.equal(elements.get("#monthly-balance-hint").textContent, "Los gastos en pesos superan los ingresos del mes.");
    assert.match(elements.get("#record-counts").innerHTML, /<strong>2<\/strong><small>Resúmenes<\/small>/);
    assert.match(elements.get("#record-counts").innerHTML, /<strong>55<\/strong><small>Transacciones<\/small>/);
    assert.match(elements.get("#record-counts").innerHTML, /<strong>2<\/strong><small>Ingresos<\/small>/);
    const santanderVisaCard = cardGrid.children[0].innerHTML;
    assert.match(santanderVisaCard, /<h3>Santander VISA<\/h3>/);
    assert.match(santanderVisaCard, /class="card-total-label">Importe total<\/span>/);
    assert.match(santanderVisaCard, /class="card-total-value">[\s\S]*2,012,382\.98[\s\S]*25\.50[\s\S]*<\/strong>/);
    assert.match(santanderVisaCard, /class="card-detail-meta"/);
    assert.match(santanderVisaCard, /<dt>Vencimiento<\/dt><dd>10 de jul de 2026<\/dd>/);
    assert.doesNotMatch(santanderVisaCard, /2,012\.38|2012\.38/);
    assert.match(santanderVisaCard, /<dt>Transacciones<\/dt><dd>54<\/dd>/);

    const naranjaCard = cardGrid.children[2].innerHTML;
    assert.match(naranjaCard, /<h3>Naranja X<\/h3>/);
    assert.match(naranjaCard, /Cargado/);
    assert.match(naranjaCard, /Confirmado/);
    assert.doesNotMatch(naranjaCard, /Faltante|Sin resumen confirmado/);
    assert.match(naranjaCard, /<dt>Transacciones<\/dt><dd>1<\/dd>/);

    assert.deepEqual(missingTransactionControlsState({ status: "DRAFT" }), {
        confirmDisabled: false,
        formHidden: false
    });
    assert.deepEqual(missingTransactionControlsState({ status: "CONFIRMED" }), {
        confirmDisabled: true,
        formHidden: true
    });
    assert.equal(draftTransactionCountLabel({ status: "DRAFT", transactions: [{}, {}] }), "Borrador · 2 filas de transacciones en borrador");

    assert.deepEqual(missingTransactionSubmitIntent({ id: 42, status: "CONFIRMED" }, "Manual fare", "10.00", ""), {
        reason: "not-draft",
        shouldSubmit: false
    });
    assert.deepEqual(missingTransactionSubmitIntent({ id: 42, status: "DRAFT" }, " ", "10.00", ""), {
        feedback: "La descripción de la transacción faltante es obligatoria.",
        reason: "missing-description",
        shouldSubmit: false
    });
    assert.deepEqual(missingTransactionSubmitIntent({ id: 42, status: "DRAFT" }, "Manual fare", "", ""), {
        feedback: "La transacción faltante requiere un importe en pesos o USD.",
        reason: "missing-amount",
        shouldSubmit: false
    });
    assert.deepEqual(missingTransactionSubmitIntent({ id: 42, status: "DRAFT" }, " Manual fare ", "0", ""), {
        clearBusyInFinally: true,
        description: "Manual fare",
        errorPrefix: "No se pudo agregar la transacción faltante:",
        loadingLabel: "Agregando...",
        notifyDraftChanged: true,
        reloadDraft: true,
        resetForm: true,
        shouldSubmit: true,
        statementId: 42,
        successFeedback: "Transacción faltante agregada al borrador."
    });
    assert.equal(parserDisplayLabel("SantanderVisaParser"), "Santander Visa");
    assert.equal(parserDisplayLabel("UnknownBankParser"), "Analizador compatible");
    assert.equal(parserDisplayLabel("Santander Visa"), "Santander Visa");
    assert.equal(parserDisplayLabel(null), "Sin analizador seleccionado");

    const transactionDom = fakeTransactionDom();
    const previousTransactionDocument = globalThis.document;
    globalThis.document = transactionDom.document;
    try {
        setTransactionCategories([{ id: 4, name: "Servicios" }]);
        assert.match(transactionDom.elements.get("#filter-category").innerHTML, /Servicios/);
        transactionDom.elements.get("#filter-month").value = "2026-08";
        assert.deepEqual(transactionFilters("2026-07"), {
            month: "2026-08",
            card: "",
            category: "",
            type: "",
            origin: ""
        });

        let result = renderTransactions({ month: "2026-08-01", rows: unifiedExpenseFixtures() }, "2026-08");
        assert.deepEqual(result, { rowCount: 3, visibleCount: 3, month: "2026-08" });
        assert.equal(transactionDom.elements.get("#transactions-table").children.length, 3);
        assert.match(transactionDom.elements.get("#transactions-table").children[0].innerHTML, /Real/);
        assertResponsiveCardLabels(transactionDom.elements.get("#transactions-table").children[0].innerHTML, ["Mes", "Fecha", "Origen", "Tarjeta / Medio", "Descripción", "Tipo", "Categoría", "Cuota", "Pesos", "USD", "Finalización", "Resumen origen", "Notas", "Acciones"]);
        assert.match(transactionDom.elements.get("#transactions-table").children[1].innerHTML, /Proyección/);
        assert.match(transactionDom.elements.get("#transactions-table").children[2].innerHTML, /Manual/);
        assert.equal(transactionDom.elements.get("#transactions-empty").hidden, true);

        transactionDom.elements.get("#filter-search").value = "próxima";
        result = renderTransactions({ month: "2026-08-01", rows: unifiedExpenseFixtures() }, "2026-08");
        assert.equal(result.visibleCount, 1);
        assert.match(transactionDom.elements.get("#transactions-table").children[0].innerHTML, /Cuota proyectada/);
        assert.match(transactionDom.elements.get("#filters-summary").textContent, /Búsqueda: "próxima"/);

        transactionDom.elements.get("#filter-search").value = "";
        transactionDom.elements.get("#filter-card").value = "VISA";
        result = renderTransactions({ month: "2026-08-01", rows: unifiedExpenseFixtures() }, "2026-08");
        assert.equal(result.visibleCount, 2);
        assert.match(transactionDom.elements.get("#filters-summary").textContent, /Tarjeta\/medio: Visa/);

        transactionDom.elements.get("#filter-card").value = "MANUAL";
        result = renderTransactions({ month: "2026-08-01", rows: unifiedExpenseFixtures() }, "2026-08");
        assert.equal(result.visibleCount, 1);
        assert.match(transactionDom.elements.get("#transactions-table").children[0].innerHTML, /Préstamo personal/);

        transactionDom.elements.get("#filter-card").value = "";
        transactionDom.elements.get("#filter-category").value = "4";
        result = renderTransactions({ month: "2026-08-01", rows: unifiedExpenseFixtures() }, "2026-08");
        assert.equal(result.visibleCount, 2);
        assert.match(transactionDom.elements.get("#filters-summary").textContent, /Categoría: Servicios/);

        transactionDom.elements.get("#filter-category").value = "";
        transactionDom.elements.get("#filter-type").value = "LOAN";
        result = renderTransactions({ month: "2026-08-01", rows: unifiedExpenseFixtures() }, "2026-08");
        assert.equal(result.visibleCount, 1);
        assert.match(transactionDom.elements.get("#filters-summary").textContent, /Tipo: Préstamo/);

        transactionDom.elements.get("#filter-type").value = "";
        transactionDom.elements.get("#filter-origin").value = "REAL";
        result = renderTransactions({ month: "2026-08-01", rows: unifiedExpenseFixtures() }, "2026-08");
        assert.equal(result.visibleCount, 1);
        assert.match(transactionDom.elements.get("#transactions-table").children[0].innerHTML, /Compra confirmada/);

        transactionDom.elements.get("#filter-origin").value = "PROJECTION";
        result = renderTransactions({ month: "2026-08-01", rows: unifiedExpenseFixtures() }, "2026-08");
        assert.equal(result.visibleCount, 2);
        assert.match(transactionDom.elements.get("#filters-summary").textContent, /Origen: Proyección/);

        transactionDom.elements.get("#filter-origin").value = "MANUAL";
        result = renderTransactions({ month: "2026-08-01", rows: unifiedExpenseFixtures() }, "2026-08");
        assert.equal(result.visibleCount, 1);
        assert.match(transactionDom.elements.get("#filters-summary").textContent, /Origen: Manual/);

        transactionDom.elements.get("#filter-origin").value = "";
        syncTransactionMonth("2026-09");
        assert.equal(transactionDom.elements.get("#filter-month").value, "2026-09");
        assert.equal(transactionFilters("2026-08").month, "2026-09");

        transactionDom.elements.get("#filter-search").value = "sin coincidencias";
        result = renderTransactions({ month: "2026-08-01", rows: unifiedExpenseFixtures() }, "2026-08");
        assert.equal(result.visibleCount, 0);
        assert.equal(transactionDom.elements.get("#transactions-empty").textContent, "No hay gastos que coincidan con los filtros seleccionados.");

        resetTransactionFilters("2026-08");
        result = renderTransactions({ month: "2026-08-01", rows: [] }, "2026-08");
        assert.equal(result.visibleCount, 0);
        assert.equal(transactionDom.elements.get("#transactions-empty").textContent, "No hay gastos reales ni proyectados para este mes.");
    } finally {
        if (previousTransactionDocument === undefined) {
            delete globalThis.document;
        } else {
            globalThis.document = previousTransactionDocument;
        }
    }

    const appDom = fakeAppDom();
    const previousAppDocument = globalThis.document;
    const previousAppFetch = globalThis.fetch;
    globalThis.document = appDom.document;
    globalThis.fetch = appDom.fetch;
    try {
        await import(`${pathToFileURL(path.join(moduleRoot, "app.js")).href}?draft-confirmation-sync`);
        appDom.dispatchDOMContentLoaded();
        await flushAsyncWork();

        appDom.elements.get("#filter-month").value = "2026-07";
        await appDom.draftReviewButton().click();
        let draftTransactionRow = appDom.elements.get("#draft-transactions-table").children[0];
        assert.ok(draftTransactionRow, "Expected draft transaction row after opening the draft");
        assertResponsiveCardLabels(draftTransactionRow.innerHTML, ["Fecha", "Descripción", "Tipo", "Categoría", "Cuota", "Total de cuotas", "Pesos", "USD", "Notas", "Acciones"]);
        assert.match(draftTransactionRow.innerHTML, /data-save-transaction/);
        assert.match(draftTransactionRow.innerHTML, /data-delete-transaction/);

        draftTransactionRow.querySelector('[name="transactionDate"]').value = "2026-11-03";
        draftTransactionRow.querySelector('[name="description"]').value = "  Café móvil  ";
        draftTransactionRow.querySelector('[name="type"]').value = "PURCHASE";
        draftTransactionRow.querySelector('[name="categoryId"]').value = "4";
        draftTransactionRow.querySelector('[name="currentInstallment"]').value = "1";
        draftTransactionRow.querySelector('[name="totalInstallments"]').value = "3";
        draftTransactionRow.querySelector('[name="amountPesos"]').value = "123.45";
        draftTransactionRow.querySelector('[name="amountUsd"]').value = "";
        draftTransactionRow.querySelector('[name="notes"]').value = "  editado  ";
        await draftTransactionRow.querySelector("[data-save-transaction]").click();
        await flushAsyncWork();

        assert.deepEqual(appDom.calls.find((call) => call.resource === "/api/transactions/301" && call.method === "PUT"), {
            method: "PUT",
            resource: "/api/transactions/301",
            body: {
                transactionDate: "2026-11-03",
                description: "Café móvil",
                type: "PURCHASE",
                categoryId: 4,
                amountPesos: "123.45",
                amountUsd: null,
                currentInstallment: 1,
                totalInstallments: 3,
                notes: "editado"
            }
        });
        assert.equal(appDom.elements.get("#draft-review-feedback").textContent, "Transacción guardada.");

        draftTransactionRow = appDom.elements.get("#draft-transactions-table").children[0];
        await draftTransactionRow.querySelector("[data-delete-transaction]").click();
        await flushAsyncWork();

        assert.deepEqual(appDom.calls.find((call) => call.resource === "/api/transactions/301" && call.method === "DELETE"), {
            method: "DELETE",
            resource: "/api/transactions/301"
        });
        assert.equal(appDom.elements.get("#draft-transactions-table").children.length, 0);
        assert.equal(appDom.elements.get("#draft-transactions-empty").hidden, false);
        await appDom.elements.get("#confirm-statement-button").click();
        await flushAsyncWork();

        assert.equal(appDom.elements.get("#month-input").value, "2026-11");
        assert.equal(appDom.elements.get("#filter-month").value, "2026-11");
        assert.deepEqual(appDom.calls.filter((call) => call.resource === "/api/dashboard/months/2026-11").at(-1), {
            method: "GET",
            resource: "/api/dashboard/months/2026-11"
        });
    } finally {
        if (previousAppDocument === undefined) {
            delete globalThis.document;
        } else {
            globalThis.document = previousAppDocument;
        }
        if (previousAppFetch === undefined) {
            delete globalThis.fetch;
        } else {
            globalThis.fetch = previousAppFetch;
        }
    }
} finally {
    await rm(moduleRoot, { force: true, recursive: true });
}

async function flushAsyncWork() {
    for (let index = 0; index < 20; index += 1) {
        await Promise.resolve();
    }
}

function fakeButton(textContent) {
    const attributes = new Map();
    return {
        attributes,
        dataset: {},
        disabled: false,
        focused: false,
        textContent,
        focus() {
            this.focused = true;
        },
        removeAttribute(name) {
            attributes.delete(name);
        },
        setAttribute(name, value) {
            attributes.set(name, value);
        }
    };
}

function fakePrimaryTabDom(tabIds) {
    const buttons = tabIds.map((tabId) => fakeTabButton(tabId));
    buttons.forEach((button) => {
        button.peerButtons = buttons;
    });
    const sections = tabIds.map((tabId) => fakeTabSection(tabId));
    return {
        buttonsById: new Map(buttons.map((button) => [button.dataset.tabTarget, button])),
        document: {
            querySelectorAll(selector) {
                if (selector === "[data-tab-target]") {
                    return buttons;
                }
                if (selector === "[data-tab-panel]") {
                    return sections;
                }
                if (selector === "[data-super-tab-target]" || selector === "[data-super-tab-panel]") {
                    return [];
                }
                throw new Error(`Unexpected tab selector: ${selector}`);
            }
        },
        sectionsById: new Map(sections.map((section) => [section.dataset.tabPanel, section]))
    };
}

function fakeGenericTabDom(tabIds, {
    buttonSelector,
    panelSelector,
    targetDatasetKey,
    panelDatasetKey
}) {
    const buttons = tabIds.map((tabId) => fakeTabButton(tabId, targetDatasetKey));
    buttons.forEach((button) => {
        button.peerButtons = buttons;
    });
    const sections = tabIds.map((tabId) => fakeTabSection(tabId, panelDatasetKey));
    return {
        buttonsById: new Map(buttons.map((button) => [button.dataset[targetDatasetKey], button])),
        document: {
            querySelectorAll(selector) {
                if (selector === buttonSelector) {
                    return buttons;
                }
                if (selector === panelSelector) {
                    return sections;
                }
                throw new Error(`Unexpected tab selector: ${selector}`);
            }
        },
        sectionsById: new Map(sections.map((section) => [section.dataset[panelDatasetKey], section]))
    };
}

function fakeShellNavigationDom() {
    const documentState = { activeElement: null };
    const listeners = new Map();
    const routeHeadings = new Map([
        ["#summary-home-title", fakeFocusableElement(documentState)],
        ["#upload-title", fakeFocusableElement(documentState)],
        ["#transactions-title", fakeFocusableElement(documentState)],
        ["#income-table-title", fakeFocusableElement(documentState)],
        ["#income-upload-title", fakeFocusableElement(documentState)],
        ["#simulator-title", fakeFocusableElement(documentState)],
        ["#categories-title", fakeFocusableElement(documentState)],
        ["#supermarket-title", fakeFocusableElement(documentState)],
        ["#super-barcode-title", fakeFocusableElement(documentState)],
        ["#super-ticket-ocr-title", fakeFocusableElement(documentState)],
        ["#super-category-title", fakeFocusableElement(documentState)]
    ]);
    const primaryPanels = new Map([
        ["summary", fakeTabSection("summary")],
        ["expenses-upload", fakeTabSection("expenses-upload")],
        ["expenses-table", fakeTabSection("expenses-table")],
        ["income-table", fakeTabSection("income-table")],
        ["income-upload", fakeTabSection("income-upload")],
        ["simulator", fakeTabSection("simulator")],
        ["categories", fakeTabSection("categories")],
        ["supermarket", fakeTabSection("supermarket")]
    ]);
    const supermarketTabs = fakeGenericTabDom(["list", "barcode", "tickets", "categories"], {
        buttonSelector: "[data-super-tab-target]",
        panelSelector: "[data-super-tab-panel]",
        targetDatasetKey: "superTabTarget",
        panelDatasetKey: "superTabPanel"
    });
    const trigger = fakeClickableButton("☰ Menú");
    const drawer = fakeFocusableElement(documentState);
    drawer.hidden = true;
    const closeButton = fakeClickableButton("×");
    const barcodeInput = fakeFocusableElement(documentState);
    const routeHashes = [
        "#monthly/summary",
        "#monthly/expenses-upload",
        "#monthly/expenses-table",
        "#monthly/income-table",
        "#monthly/income-upload",
        "#monthly/simulator",
        "#monthly/categories",
        "#stock/list",
        "#stock/barcode",
        "#stock/tickets",
        "#stock/categories"
    ];
    const routeLinks = routeHashes.map((hash) => fakeRouteLink(hash, documentState));
    const linkByHash = new Map(routeLinks.map((link) => [link.dataset.shellRoute, link]));
    const history = {
        calls: [],
        replaceState(_state, _title, hash) {
            this.calls.push({ type: "replace", hash });
        }
    };
    const location = { hash: "" };

    for (const button of supermarketTabs.buttonsById.values()) {
        button.peerButtons = [...supermarketTabs.buttonsById.values()];
    }

    const document = {
        activeElement: null,
        addEventListener(type, listener) {
            listeners.set(type, listener);
        },
        dispatchEvent(event) {
            listeners.get(event.type)?.(event);
        },
        querySelector(selector) {
            if (selector === "#app-shell-menu-button") {
                return trigger;
            }
            if (selector === "#app-shell-drawer") {
                return drawer;
            }
            if (selector === "#app-shell-drawer-close") {
                return closeButton;
            }
            if (selector === "#super-barcode-code") {
                return barcodeInput;
            }
            if (selector === "#tab-supermarket") {
                return primaryPanels.get("supermarket");
            }
            if (routeHeadings.has(selector)) {
                return routeHeadings.get(selector);
            }
            if (selector.startsWith("#super-tab-")) {
                return supermarketTabs.buttonsById.get(selector.replace("#super-tab-", "")) || null;
            }
            return null;
        },
        querySelectorAll(selector) {
            if (selector === "[data-tab-panel]") {
                return [...primaryPanels.values()];
            }
            if (selector === "[data-super-tab-target]") {
                return [...supermarketTabs.buttonsById.values()];
            }
            if (selector === "[data-super-tab-panel]") {
                return [...supermarketTabs.sectionsById.values()];
            }
            if (selector === "[data-shell-route]") {
                return routeLinks;
            }
            return [];
        }
    };

    return {
        barcodeInput,
        document,
        drawer,
        history,
        linkByHash,
        location,
        primaryPanels,
        routeHeadings,
        routeLinks,
        supermarketTabs,
        trigger
    };
}

function fakeRouteLink(hash, documentState) {
    const link = fakeFocusableElement(documentState);
    const listeners = new Map();
    link.dataset.shellRoute = hash;
    link.addEventListener = (type, listener) => {
        listeners.set(type, listener);
    };
    link.click = async () => {
        await listeners.get("click")?.({ currentTarget: link, preventDefault() {} });
    };
    return link;
}

function fakeFocusableElement(documentState) {
    const element = fakeAppElement();
    element.focused = false;
    element.focus = () => {
        if (documentState.activeElement && documentState.activeElement !== element) {
            documentState.activeElement.focused = false;
        }
        element.focused = true;
        documentState.activeElement = element;
    };
    return element;
}

function fakeTabButton(tabId, targetDatasetKey = "tabTarget") {
    const classes = new Set();
    const attributes = new Map();
    const listeners = new Map();
    return {
        attributes,
        dataset: { [targetDatasetKey]: tabId },
        focused: false,
        tabIndex: 0,
        addEventListener(type, listener) {
            listeners.set(type, listener);
        },
        classList: {
            contains(className) {
                return classes.has(className);
            },
            toggle(className, force) {
                const shouldAdd = force ?? !classes.has(className);
                if (shouldAdd) {
                    classes.add(className);
                } else {
                    classes.delete(className);
                }
                return shouldAdd;
            }
        },
        click() {
            listeners.get("click")?.({ type: "click" });
        },
        keydown(key) {
            let prevented = false;
            listeners.get("keydown")?.({
                key,
                preventDefault() {
                    prevented = true;
                }
            });
            return prevented;
        },
        focus() {
            this.peerButtons?.forEach((button) => {
                button.focused = false;
            });
            this.focused = true;
        },
        setAttribute(name, value) {
            attributes.set(name, value);
        }
    };
}

function fakeTabSection(tabId, panelDatasetKey = "tabPanel") {
    return {
        dataset: { [panelDatasetKey]: tabId },
        hidden: false
    };
}

function assertPrimaryTabState(primaryTabDom, activeTabId) {
    assertTabState(primaryTabDom, activeTabId);
}

function assertTabState(tabDom, activeTabId, { focusedTabId = "" } = {}) {
    for (const [tabId, button] of tabDom.buttonsById) {
        const selected = tabId === activeTabId;
        assert.equal(button.attributes.get("aria-selected"), String(selected), `${tabId} aria-selected`);
        assert.equal(button.classList.contains("active"), selected, `${tabId} active class`);
        assert.equal(button.tabIndex, selected ? 0 : -1, `${tabId} tabindex`);
        assert.equal(tabDom.sectionsById.get(tabId).hidden, !selected, `${tabId} panel hidden`);
        if (focusedTabId) {
            assert.equal(button.focused, tabId === focusedTabId, `${tabId} focused`);
        }
    }
}

function fakeAppDom() {
    const domContentLoadedListeners = [];
    const calls = [];
    const elements = new Map();
    const tabIds = ["summary", "expenses-upload", "expenses-table", "income-table", "income-upload", "simulator", "categories", "supermarket"];
    const tabButtons = tabIds.map((tabId) => fakeTabButton(tabId));
    const tabSections = tabIds.map((tabId) => fakeTabSection(tabId));
    let confirmed = false;

    let draftStatement = {
        id: 901,
        provider: "SANTANDER",
        cardBrand: "VISA",
        cardAlias: "Santander Visa",
        status: "DRAFT",
        paymentMonth: "2026-11-01",
        periodStart: "2026-10-01",
        periodEnd: "2026-10-31",
        closingDate: "2026-10-31",
        dueDate: "2026-11-10",
        totalPesos: 100,
        totalUsd: 0,
        minimumPaymentPesos: 50,
        transactionCount: 1,
        transactions: [draftTransactionFixture()]
    };
    const confirmedStatement = () => ({ ...draftStatement, status: "CONFIRMED" });

    for (const selector of [
        "#month-tabs",
        "#month-input",
        "#transaction-filters",
        "#filter-search",
        "#clear-transaction-filters",
        "#category-form",
        "#category-form button[type='submit']",
        "#category-name",
        "#category-color",
        "#category-list",
        "#category-feedback",
        "#statement-upload-form",
        "#statement-files",
        "#statement-upload-button",
        "#statement-upload-feedback",
        "#statement-form",
        "#statement-form button[type='submit']",
        "#missing-transaction-form",
        "#confirm-statement-button",
        "#draft-statement-list",
        "#draft-empty-state",
        "#draft-review-panel",
        "#draft-review-title",
        "#draft-review-meta",
        "#statement-provider",
        "#statement-card-brand",
        "#statement-card-alias",
        "#statement-period-start",
        "#statement-period-end",
        "#statement-closing-date",
        "#statement-due-date",
        "#statement-payment-month",
        "#statement-total-pesos",
        "#statement-total-usd",
        "#statement-minimum-payment-pesos",
        "#statement-notes-placeholder",
        "#missing-transaction-type",
        "#missing-transaction-category",
        "#missing-transaction-description",
        "#missing-transaction-amount-pesos",
        "#missing-transaction-amount-usd",
        "#draft-transactions-table",
        "#draft-transactions-empty",
        "#draft-review-feedback",
        "#monthly-income-total",
        "#salary-income-total",
        "#variable-income-total",
        "#projected-income-total",
        "#projected-income-card",
        "#summary-estimated-label",
        "#total-pesos",
        "#total-usd",
        "#monthly-balance-pesos",
        "#monthly-balance-hint",
        "#total-pesos-hint",
        "#one-payment-total",
        "#installment-total",
        "#charges-total",
        "#record-counts",
        "#card-detail-grid",
        "#transactions-table",
        "#transactions-empty",
        "#filters-summary",
        "#manual-expense-form",
        "#manual-expense-category",
        "#manual-expenses-table",
        "#manual-expenses-empty",
        "#manual-expenses-summary",
        "#income-form",
        "#income-filter-form",
        "#clear-income-filter",
        "#income-filter-month",
        "#incomes-table",
        "#incomes-empty",
        "#income-filters-summary",
        "#income-feedback",
        "#income-table-feedback",
        "#income-edit-modal",
        "#income-edit-form",
        "#income-edit-feedback",
        "#income-edit-save-from-month",
        "#income-edit-recurring",
        "#income-edit-cancel",
        "#income-edit-close",
        "#simulator-form",
        "#clear-simulation",
        "#simulator-category",
        "#super-category-form",
        "#super-category-form button[type='submit']",
        "#super-category-name",
        "#super-category-feedback",
        "#super-category-list",
        "#super-category-table-wrap",
        "#super-category-toggle",
        "#super-barcode-form",
        "#super-barcode-form button[type='submit']",
        "#super-barcode-code",
        "#super-barcode-format",
        "#super-barcode-item",
        "#super-barcode-attach",
        "#super-barcode-remove",
        "#super-barcode-scan-start",
        "#super-barcode-scan-stop",
        "#super-barcode-scanner",
        "#super-barcode-scanner-preview",
        "#super-barcode-scanner-status",
        "#super-barcode-actions",
        "#super-barcode-purchase",
        "#super-barcode-consume",
        "#super-barcode-feedback",
        "#super-barcode-result",
        "#super-session-panel",
        "#super-session-summary",
        "#super-session-status",
        "#super-session-lines",
        "#super-session-empty",
        "#super-session-draft-form",
        "#super-session-item-name",
        "#super-session-draft-type",
        "#super-session-draft-quantity",
        "#super-session-draft-notes",
        "#super-session-draft-allow-negative",
        "#super-session-cancel-draft",
        "#super-session-clear",
        "#super-session-save-draft",
        "#super-session-confirm",
        "#super-item-form",
        "#super-item-form button[type='submit']",
        "#super-item-name",
        "#super-item-category",
        "#super-item-unit",
        "#super-item-presentation-label",
        "#super-item-presentation-quantity",
        "#super-item-presentation-price-pesos",
        "#super-item-presentation-price-source",
        "#super-item-presentation-price-source-label",
        "#super-item-presentation-price-source-help",
        "#super-item-presentation-price-observed-date",
        "#super-item-objective",
        "#super-item-quick-quantity",
        "#super-item-current-stock",
        "#super-item-notes",
        "#super-item-submit",
        "#super-item-cancel-edit",
        "#super-generate-list",
        "#super-copy-list",
        "#super-download-list",
        "#super-whatsapp-list",
        "#super-uncheck-all",
        "#super-items-table",
        "#super-items-empty",
        "#super-items-summary",
        "#super-generated-list",
        "#super-feedback",
        "#super-movement-modal",
        "#super-movement-form",
        "#super-movement-title",
        "#super-movement-item-id",
        "#super-movement-type",
        "#super-movement-item-name",
        "#super-movement-quantity",
        "#super-movement-notes",
        "#super-movement-allow-negative",
        "#super-movement-conflict",
        "#super-movement-feedback",
        "#super-movement-submit",
        "#super-movement-cancel",
        "#super-movement-close",
        "#super-movement-history",
        "#super-movement-history-title",
        "#super-movement-history-table",
        "#super-movement-history-empty",
        "#super-price-observation-form",
        "#super-price-observation-item",
        "#super-price-observation-price-pesos",
        "#super-price-observation-price-source",
        "#super-price-observation-source-label",
        "#super-price-observation-observed-date",
        "#super-price-observation-feedback",
        "#super-price-observation-title",
        "#super-price-observation-context-summary",
        "#super-price-observation-global-reset",
        "#super-price-source-form",
        "#super-price-source-name",
        "#super-price-source-feedback",
        "#super-price-observation-table",
        "#super-price-observation-empty",
        "#super-ticket-ocr-form",
        "#super-ticket-ocr-file",
        "#super-ticket-ocr-feedback",
        "#super-ticket-ocr-summary",
        "#super-ticket-ocr-meta",
        "#super-ticket-ocr-warning-list",
        "#super-ticket-ocr-review-panel",
        "#super-ticket-ocr-table",
        "#super-ticket-ocr-empty",
        "#super-ticket-ocr-confirm-form",
        "#super-ticket-ocr-line-index",
        "#super-ticket-ocr-selected-line",
        "#super-ticket-ocr-description",
        "#super-ticket-ocr-price-pesos",
        "#super-ticket-ocr-product",
        "#super-ticket-ocr-date-candidate",
        "#super-ticket-ocr-date",
        "#super-ticket-ocr-source-candidate",
        "#super-ticket-ocr-source-label",
        "#super-ticket-ocr-sync-current-reference-price",
        "#super-ticket-ocr-selected-warnings",
        "#super-ticket-ocr-confirm-feedback",
        "#super-ticket-ocr-discard",
        "#app-shell-menu-button",
        "#app-shell-drawer",
        "#app-shell-drawer-close",
        "#primary-tab-supermarket",
        "#super-tab-list",
        "#super-tab-barcode",
        "#super-tab-tickets",
        "#super-tab-categories",
        "#super-panel-list",
        "#super-panel-barcode",
        "#super-panel-tickets",
        "#super-panel-categories",
        "#logout-form",
        "#app-status",
        "#card-detail-skeleton-template"
    ]) {
        elements.set(selector, fakeAppElement());
    }
    elements.get("#card-detail-skeleton-template").innerHTML = '<article class="card-detail card-detail-skeleton"></article>';
    elements.set("#filter-month", fakeInput());
    elements.set("#filter-card", fakeInput());
    elements.set("#filter-category", fakeSelect());
    elements.set("#filter-type", fakeInput());
    elements.set("#filter-origin", fakeInput());
    elements.set("#super-item-presentation-price-source", fakeClickableSelect());
    elements.set("#super-item-presentation-price-source-help", fakeElement());
    elements.set("#super-item-presentation-price-source-label", fakeClickableInput());
    elements.get("#super-category-name").dataset.superLimit = "categoryName";
    elements.get("#super-item-name").dataset.superLimit = "itemName";
    elements.get("#super-item-unit").dataset.superLimit = "itemUnit";
    elements.get("#super-item-presentation-label").dataset.superLimit = "presentationLabel";
    elements.get("#super-item-presentation-price-source-label").dataset.superLimit = "priceSourceLabel";
    elements.get("#super-price-observation-source-label").dataset.superLimit = "priceSourceLabel";
    elements.get("#super-ticket-ocr-source-label").dataset.superLimit = "priceSourceLabel";
    elements.get("#super-item-notes").dataset.superLimit = "itemNotes";
    elements.get("#super-barcode-code").dataset.superLimit = "barcodeCode";
    elements.get("#super-barcode-format").dataset.superLimit = "barcodeFormat";
    elements.get("#statement-files").files = [];
    elements.get("#super-ticket-ocr-file").files = [];
    for (const selector of ["#category-form", "#missing-transaction-form", "#income-form", "#manual-expense-form", "#simulator-form", "#super-category-form", "#super-item-form", "#super-barcode-form", "#super-price-observation-form", "#super-price-source-form", "#super-ticket-ocr-form", "#super-ticket-ocr-confirm-form"]) {
        elements.get(selector).reset = function resetForm() {
            this.resetCount += 1;
        };
    }

    return {
        calls,
        elements,
        dispatchDOMContentLoaded() {
            domContentLoadedListeners.forEach((listener) => listener({ type: "DOMContentLoaded" }));
        },
        document: {
            addEventListener(type, listener) {
                if (type === "DOMContentLoaded") {
                    domContentLoadedListeners.push(listener);
                    return;
                }
                if (type === "visibilitychange" || type === "keydown") {
                    return;
                }
                throw new Error(`Unexpected app document listener: ${type}`);
            },
            createElement(tagName) {
                if (tagName === "option") {
                    return { textContent: "", value: "" };
                }
                return fakeAppElement();
            },
            querySelector(selector) {
                const draftTransactionId = selector.match(/^\[data-transaction-id="([^"]+)"\]$/)?.[1];
                if (draftTransactionId) {
                    return elements.get("#draft-transactions-table").children.find((row) => String(row.dataset.transactionId) === draftTransactionId) || null;
                }
                assert.ok(elements.has(selector), `Unexpected app selector: ${selector}`);
                return elements.get(selector);
            },
            querySelectorAll(selector) {
                if (selector === "[data-tab-target]") {
                    return tabButtons;
                }
                if (selector === "[data-tab-panel]") {
                    return tabSections;
                }
                if (selector === "[data-super-tab-target]" || selector === "[data-super-tab-panel]") {
                    return [];
                }
                if (selector === "[data-shell-route]") {
                    return [];
                }
                if (selector === "[data-super-limit]") {
                    return supermarketLimitFields(elements);
                }
                throw new Error(`Unexpected app selectorAll: ${selector}`);
            }
        },
        draftReviewButton() {
            const draftCard = elements.get("#draft-statement-list").children[0];
            assert.ok(draftCard, "Expected a draft card after the app dashboard load");
            return draftCard.querySelector("[data-review-draft]");
        },
        async fetch(requestPath, options = {}) {
            const resource = String(requestPath);
            const method = options.method || "GET";
            const call = { method, resource };
            if (options.body) {
                call.body = typeof options.body === "string" ? JSON.parse(options.body) : options.body;
            }
            calls.push(call);

            if (resource === "/api/categories") {
                return jsonResponse([]);
            }
            if (resource.startsWith("/api/dashboard/summary")) {
                return jsonResponse(emptySummary());
            }
            if (resource === "/api/dashboard/months") {
                return jsonResponse([]);
            }
            if (resource.startsWith("/api/dashboard/months/")) {
                const month = resource.slice("/api/dashboard/months/".length);
                return jsonResponse(emptyMonthDetail(month));
            }
            if (resource.startsWith("/api/statements?month=")) {
                return jsonResponse(confirmed ? [confirmedStatement()] : []);
            }
            if (resource === "/api/statements") {
                return jsonResponse(confirmed ? [confirmedStatement()] : [draftStatement]);
            }
            if (resource === "/api/statements/901" && method === "GET") {
                return jsonResponse(confirmed ? confirmedStatement() : draftStatement);
            }
            if (resource === "/api/statements/901" && method === "PUT") {
                return jsonResponse({ ...draftStatement, ...JSON.parse(options.body) });
            }
            if (resource === "/api/statements/901/confirm" && method === "POST") {
                confirmed = true;
                return jsonResponse(confirmedStatement());
            }
            if (resource === "/api/transactions/301" && method === "PUT") {
                const payload = JSON.parse(options.body);
                draftStatement = {
                    ...draftStatement,
                    transactions: draftStatement.transactions.map((transaction) => transaction.id === 301 ? { ...transaction, ...payload } : transaction)
                };
                return jsonResponse(draftStatement.transactions[0]);
            }
            if (resource === "/api/transactions/301" && method === "DELETE") {
                draftStatement = {
                    ...draftStatement,
                    transactionCount: 0,
                    transactions: []
                };
                return { ok: true, status: 204 };
            }
            if (resource.startsWith("/api/manual-expenses")) {
                return jsonResponse([]);
            }
            if (resource.startsWith("/api/incomes")) {
                return jsonResponse([]);
            }
            if (resource.startsWith("/api/super/categories")) {
                return jsonResponse([]);
            }
            if (resource.startsWith("/api/super/movements")) {
                return jsonResponse([]);
            }
            if (resource === "/api/super/scan-sessions/active") {
                return jsonResponse({ id: 12, state: "ACTIVE", expiresAt: "2026-07-27T23:00:00Z", resolvedItems: [], drafts: [] });
            }
            if (resource.startsWith("/api/super/items")) {
                return jsonResponse([]);
            }
            throw new Error(`Unexpected app fetch: ${method} ${resource}`);
        }
    };
}

function fakeAppElement() {
    const listeners = new Map();
    const childSelectors = new Map();
    const classes = new Set();
    const element = {
        attributes: new Map(),
        children: [],
        className: "",
        dataset: {},
        disabled: false,
        files: [],
        hidden: false,
        resetCount: 0,
        style: {},
        textContent: "",
        type: "",
        value: "",
        addEventListener(type, listener) {
            listeners.set(type, listener);
        },
        append(child) {
            this.children.push(child);
        },
        async click() {
            await listeners.get("click")?.({ currentTarget: this, target: this, preventDefault() {} });
        },
        classList: {
            contains(className) {
                return classes.has(className);
            },
            toggle(className, force) {
                const shouldAdd = force ?? !classes.has(className);
                if (shouldAdd) {
                    classes.add(className);
                } else {
                    classes.delete(className);
                }
                return shouldAdd;
            }
        },
        dispatchEvent(event) {
            listeners.get(event.type)?.(event);
        },
        querySelector(selector) {
            if (!childSelectors.has(selector)) {
                childSelectors.set(selector, fakeAppElement());
            }
            return childSelectors.get(selector);
        },
        removeAttribute(name) {
            this.attributes.delete(name);
        },
        reset() {
            this.resetCount += 1;
        },
        setAttribute(name, value) {
            this.attributes.set(name, value);
        },
        async submit() {
            await listeners.get("submit")?.({ preventDefault() {} });
        }
    };
    Object.defineProperty(element, "innerHTML", {
        get() {
            return this._innerHTML || "";
        },
        set(value) {
            this._innerHTML = value;
            if (value === "") {
                this.children = [];
            }
            if (value.includes("data-save-transaction") || value.includes("data-delete-transaction")) {
                registerDraftTransactionControls(value, childSelectors);
            }
        }
    });
    return element;
}

function registerDraftTransactionControls(html, childSelectors) {
    childSelectors.clear();
    for (const match of html.matchAll(/<input\b([^>]*)>/g)) {
        const name = attributeValue(match[1], "name");
        if (!name) {
            continue;
        }
        registerNamedControl(childSelectors, name, fakeInput(attributeValue(match[1], "value") || ""));
    }
    for (const match of html.matchAll(/<select\b([^>]*)>([\s\S]*?)<\/select>/g)) {
        const name = attributeValue(match[1], "name");
        if (!name) {
            continue;
        }
        const selectedValue = match[2].match(/<option\b[^>]*value="([^"]*)"[^>]*\bselected\b/)?.[1]
            || match[2].match(/<option\b[^>]*value="([^"]*)"/)?.[1]
            || "";
        registerNamedControl(childSelectors, name, fakeInput(selectedValue));
    }
    if (html.includes("data-save-transaction")) {
        childSelectors.set("[data-save-transaction]", fakeClickableButton("Guardar fila"));
    }
    if (html.includes("data-delete-transaction")) {
        childSelectors.set("[data-delete-transaction]", fakeClickableButton("Eliminar fila"));
    }
}

function registerNamedControl(childSelectors, name, control) {
    childSelectors.set(`[name="${name}"]`, control);
    childSelectors.set(`[name='${name}']`, control);
}

function draftTransactionFixture() {
    return {
        id: 301,
        transactionDate: "2026-11-02",
        description: "Compra original",
        type: "PURCHASE",
        category: { id: 4, name: "Cafetería" },
        currentInstallment: null,
        totalInstallments: null,
        amountPesos: "99.90",
        amountUsd: null,
        notes: "Original"
    };
}

function emptySummary() {
    return {
        totalPesos: 0,
        totalUsd: 0,
        incomeTotalPesos: 0,
        salaryIncomeTotalPesos: 0,
        variableIncomeTotalPesos: 0,
        projectedIncomeTotalPesos: 0,
        monthlyBalancePesos: 0,
        estimated: false,
        statementCount: 0,
        transactionCount: 0,
        incomeCount: 0
    };
}

function emptyMonthDetail(month) {
    return {
        month,
        currentReal: false,
        projectionOnly: false,
        totalPesos: 0,
        totalUsd: 0,
        totalsByCard: [],
        rows: []
    };
}

function jsonResponse(body) {
    return {
        ok: true,
        status: 200,
        async json() {
            return body;
        }
    };
}

function fakeDashboardElements() {
    return new Map([
        "#month-tabs",
        "#month-input",
        "#monthly-income-total",
        "#salary-income-total",
        "#variable-income-total",
        "#projected-income-total",
        "#projected-income-card",
        "#summary-estimated-label",
        "#total-pesos",
        "#total-usd",
        "#monthly-balance-pesos",
        "#monthly-balance-hint",
        "#total-pesos-hint",
        "#one-payment-total",
        "#installment-total",
        "#charges-total",
        "#record-counts",
        "#card-detail-grid"
    ].map((selector) => [selector, fakeElement()]));
}

function fakeTransactionDom() {
    const elements = new Map([
        ["#filter-month", fakeInput()],
        ["#filter-card", fakeInput()],
        ["#filter-category", fakeSelect()],
        ["#filter-type", fakeInput()],
        ["#filter-origin", fakeInput()],
        ["#filter-search", fakeInput()],
        ["#transactions-table", fakeManualExpenseTable()],
        ["#transactions-empty", fakeElement()],
        ["#filters-summary", fakeElement()]
    ]);
    return {
        document: {
            createElement(tagName) {
                assert.ok(["option", "tr"].includes(tagName));
                return tagName === "option" ? { value: "", textContent: "" } : fakeElement();
            },
            querySelector(selector) {
                assert.ok(elements.has(selector), `Unexpected transaction selector: ${selector}`);
                return elements.get(selector);
            }
        },
        elements
    };
}

function unifiedExpenseFixtures() {
    return [
        {
            kind: "ACTUAL",
            source: "STATEMENT",
            sourceStatementId: 10,
            sourceStatementMonth: "2026-08-01",
            sourceTransactionId: 20,
            transactionDate: "2026-07-21",
            month: "2026-08-01",
            description: "Compra confirmada",
            provider: "SANTANDER",
            cardBrand: "VISA",
            cardAlias: "Santander Visa",
            type: "PURCHASE",
            categoryId: 4,
            categoryName: "Servicios",
            amountPesos: 100,
            amountUsd: 0,
            notes: "Desde resumen"
        },
        {
            kind: "PROJECTION",
            source: "STATEMENT",
            sourceStatementId: 10,
            sourceStatementMonth: "2026-07-01",
            sourceTransactionId: 21,
            transactionDate: "2026-07-10",
            month: "2026-08-01",
            description: "Cuota proyectada",
            provider: "SANTANDER",
            cardBrand: "VISA",
            cardAlias: "Santander Visa",
            type: "INSTALLMENT",
            categoryId: 4,
            categoryName: "Servicios",
            installmentNumber: 2,
            totalInstallments: 3,
            amountPesos: 200,
            amountUsd: 0,
            estimatedFinishMonth: "2026-09-01",
            notes: "Próxima cuota"
        },
        {
            kind: "PROJECTION",
            source: "MANUAL_EXPENSE",
            sourceStatementMonth: "2026-07-01",
            sourceTransactionId: 30,
            month: "2026-08-01",
            description: "Préstamo personal",
            provider: "MANUAL",
            cardBrand: "OTHER",
            cardAlias: "Préstamo manual",
            type: "LOAN",
            categoryId: null,
            categoryName: null,
            installmentNumber: 2,
            totalInstallments: 6,
            amountPesos: 300,
            amountUsd: 0,
            notes: "Banco"
        }
    ];
}

function fakeElement() {
    const classes = new Set();
    return {
        attributes: new Map(),
        children: [],
        className: "",
        dataset: {},
        hidden: false,
        innerHTML: "",
        textContent: "",
        classList: {
            contains(className) {
                return classes.has(className);
            },
            toggle(className, force) {
                const shouldAdd = force ?? !classes.has(className);
                if (shouldAdd) {
                    classes.add(className);
                } else {
                    classes.delete(className);
                }
                return shouldAdd;
            }
        },
        addEventListener() {},
        append(child) {
            this.children.push(child);
        },
        setAttribute(name, value) {
            this.attributes.set(name, value);
        }
    };
}

function fakeVideoElement() {
    const video = fakeElement();
    video.srcObject = null;
    video.play = async () => {};
    return video;
}

function fakeLoginDom(search) {
    const feedbackClasses = new Set();
    const feedback = {
        textContent: "",
        classList: {
            add(name) {
                feedbackClasses.add(name);
            },
            remove(name) {
                feedbackClasses.delete(name);
            },
            contains(name) {
                return feedbackClasses.has(name);
            }
        }
    };

    return {
        feedback,
        document: {
            addEventListener() {},
            querySelector(selector) {
                if (selector === "#login-feedback") {
                    return feedback;
                }
                if (selector === "#login-form") {
                    return fakeForm();
                }
                throw new Error(`Unexpected login selector: ${selector}`);
            }
        },
        window: {
            location: { search }
        }
    };
}

function fakeIncomeDom() {
    const elements = new Map();
    const table = fakeIncomeTable();
    const incomeForm = fakeIncomeForm(elements);

    for (const selector of [
        "#income-filter-month",
        "#income-description",
        "#income-type",
        "#income-amount",
        "#income-start-month",
        "#income-recurring-monthly",
        "#income-notes",
        "#income-edit-id",
        "#income-edit-description",
        "#income-edit-type",
        "#income-edit-amount",
        "#income-edit-recurring",
        "#income-edit-start-month",
        "#income-edit-end-month",
        "#income-edit-effective-month",
        "#income-edit-notes"
    ]) {
        elements.set(selector, fakeInput());
    }
    elements.get("#income-type").value = "SALARY";
    elements.get("#income-recurring-monthly").value = "true";
    elements.get("#income-edit-type").value = "SALARY";
    elements.get("#income-edit-recurring").value = "true";
    elements.set("#income-form", incomeForm);
    elements.set("#income-filter-form", fakeForm());
    elements.set("#clear-income-filter", fakeClickable());
    elements.set("#incomes-table", table);
    elements.set("#incomes-empty", fakeElement());
    elements.set("#income-filters-summary", fakeElement());
    elements.set("#income-feedback", fakeElement());
    elements.set("#income-table-feedback", fakeElement());
    elements.set("#income-edit-modal", fakeElement());
    elements.get("#income-edit-modal").hidden = true;
    elements.set("#income-edit-form", fakeForm());
    elements.set("#income-edit-feedback", fakeElement());
    elements.set("#income-edit-save", fakeButton("Guardar cambios"));
    elements.set("#income-edit-save-from-month", fakeClickableButton("Guardar cambios desde el mes seleccionado"));
    elements.set("#income-edit-recurring", fakeClickableInput("true"));
    elements.set("#income-edit-effective-month-group", fakeElement());
    elements.set("#income-edit-cancel", fakeClickableButton("Cancelar"));
    elements.set("#income-edit-close", fakeClickableButton("Cerrar"));

    const calls = [];
    const api = {
        calls,
        async incomes(filters) {
            calls.push({ method: "incomes", filters });
            if (filters?.month) {
                return [incomeFixture({ id: 3, description: "Ingreso proyectado", projected: true, startMonth: "2026-07" })];
            }
            return [
                incomeFixture({ id: 1, description: "Sueldo principal", startMonth: "2026-07" }),
                incomeFixture({ id: 2, description: "Honorarios", incomeType: "VARIABLE", recurringMonthly: false, startMonth: "2026-08" })
            ];
        },
        async createIncome(payload) {
            calls.push({ method: "createIncome", payload });
        },
        async updateIncome(id, payload) {
            calls.push({ method: "updateIncome", id, payload });
        },
        async updateIncomeFromMonth(id, yearMonth, payload) {
            calls.push({ method: "updateIncomeFromMonth", id, yearMonth, payload });
        },
        async deleteIncome(id) {
            calls.push({ method: "deleteIncome", id });
        }
    };

    return {
        api,
        document: {
            createElement(tagName) {
                assert.equal(tagName, "tr");
                return fakeElement();
            },
            querySelector(selector) {
                assert.ok(elements.has(selector), `Unexpected income selector: ${selector}`);
                return elements.get(selector);
            }
        },
        elements
    };
}

function fakeManualExpenseDom() {
    const elements = new Map();
    const table = fakeManualExpenseTable();
    const manualExpenseForm = fakeManualExpenseForm(elements);

    for (const selector of [
        "#manual-expense-description",
        "#manual-expense-type",
        "#manual-expense-amount-pesos",
        "#manual-expense-amount-usd",
        "#manual-expense-start-month",
        "#manual-expense-total-installments",
        "#manual-expense-current-installment",
        "#manual-expense-category",
        "#manual-expense-notes"
    ]) {
        elements.set(selector, fakeInput());
    }
    elements.get("#manual-expense-type").value = "ONE_PAYMENT";
    elements.set("#manual-expense-form", manualExpenseForm);
    elements.set("#manual-expense-form button[type='submit']", manualExpenseForm.submitButton);
    elements.set("#manual-expenses-table", table);
    elements.set("#manual-expenses-empty", fakeElement());
    elements.set("#manual-expenses-summary", fakeElement());
    elements.set("#manual-expense-feedback", fakeElement());

    const calls = [];
    const api = {
        calls,
        async manualExpenses(filters) {
            calls.push({ method: "manualExpenses", filters });
            return [
                manualExpenseFixture({ id: 77, description: "Préstamo personal", type: "LOAN", totalInstallments: 6, installmentNumber: 2, category: { id: 4, name: "Préstamos" } }),
                manualExpenseFixture({ id: 78, description: "Efectivo", type: "CASH", amountPesos: 500, amountUsd: 0 })
            ];
        },
        async createManualExpense(payload) {
            calls.push({ method: "createManualExpense", payload });
        },
        async deleteManualExpense(id) {
            calls.push({ method: "deleteManualExpense", id });
        }
    };

    return {
        api,
        document: {
            createElement(tagName) {
                assert.equal(tagName, "tr");
                return fakeManualExpenseRow();
            },
            querySelector(selector) {
                assert.ok(elements.has(selector), `Unexpected manual expense selector: ${selector}`);
                return elements.get(selector);
            }
        },
        elements
    };
}

function fakeSupermarketDom() {
    const elements = new Map();
    const documentListeners = new Map();
    const table = fakeIncomeTable();
    const superItemForm = fakeSuperItemForm(elements);
    const superCategoryForm = fakeSuperCategoryForm(elements);
    const supermarketTabButtons = ["list", "barcode", "tickets", "categories"].map((tabId) => fakeTabButton(tabId, "superTabTarget"));
    supermarketTabButtons.forEach((button) => {
        button.peerButtons = supermarketTabButtons;
    });
    const supermarketTabPanels = ["list", "barcode", "tickets", "categories"].map((tabId) => fakeTabSection(tabId, "superTabPanel"));
    elements.set("#primary-tab-supermarket", fakeClickableButton("Lista del super"));
    elements.set("#super-tab-list", supermarketTabButtons[0]);
    elements.set("#super-tab-barcode", supermarketTabButtons[1]);
    elements.set("#super-tab-tickets", supermarketTabButtons[2]);
    elements.set("#super-tab-categories", supermarketTabButtons[3]);
    elements.set("#super-panel-list", supermarketTabPanels[0]);
    elements.set("#super-panel-barcode", supermarketTabPanels[1]);
    elements.set("#super-panel-tickets", supermarketTabPanels[2]);
    elements.set("#super-panel-categories", supermarketTabPanels[3]);

    for (const selector of [
        "#super-category-name",
        "#super-item-name",
        "#super-item-unit",
        "#super-item-presentation-label",
        "#super-item-presentation-quantity",
        "#super-item-presentation-price-pesos",
        "#super-item-presentation-price-observed-date",
        "#super-item-objective",
        "#super-item-notes",
        "#super-item-quick-quantity",
        "#super-item-current-stock",
        "#super-price-observation-price-pesos",
        "#super-price-observation-source-label",
        "#super-price-source-name",
        "#super-price-observation-observed-date",
        "#super-price-observation-sync-current-reference-price",
        "#super-ticket-ocr-file",
        "#super-ticket-ocr-line-index",
        "#super-ticket-ocr-selected-line",
        "#super-ticket-ocr-description",
        "#super-ticket-ocr-price-pesos",
        "#super-ticket-ocr-date",
        "#super-ticket-ocr-source-label",
        "#super-ticket-ocr-sync-current-reference-price",
        "#super-barcode-code",
        "#super-barcode-format",
        "#super-session-item-name",
        "#super-session-draft-quantity",
        "#super-session-draft-notes",
        "#super-session-draft-allow-negative"
    ]) {
        elements.set(selector, fakeInput());
    }
    elements.get("#super-category-name").dataset.superLimit = "categoryName";
    elements.get("#super-item-name").dataset.superLimit = "itemName";
    elements.get("#super-item-unit").dataset.superLimit = "itemUnit";
    elements.get("#super-item-presentation-label").dataset.superLimit = "presentationLabel";
    elements.set("#super-item-presentation-price-source", fakeClickableSelect());
    elements.set("#super-item-presentation-price-source-help", fakeElement());
    elements.set("#super-item-presentation-price-source-label", fakeClickableInput());
    elements.get("#super-item-presentation-price-source-label").dataset.superLimit = "priceSourceLabel";
    elements.get("#super-price-observation-source-label").dataset.superLimit = "priceSourceLabel";
    elements.get("#super-price-source-name").dataset.superLimit = "priceSourceLabel";
    elements.get("#super-price-source-name").dataset.superLimit = "priceSourceLabel";
    elements.get("#super-ticket-ocr-source-label").dataset.superLimit = "priceSourceLabel";
    elements.get("#super-item-notes").dataset.superLimit = "itemNotes";
    elements.get("#super-barcode-code").dataset.superLimit = "barcodeCode";
    elements.get("#super-barcode-format").dataset.superLimit = "barcodeFormat";
    elements.set("#super-item-category", fakeSelect());
    elements.get("#super-ticket-ocr-file").files = [];
    elements.set("#super-ticket-ocr-form", fakeSuperTicketOcrForm(elements));
    elements.set("#super-ticket-ocr-form button[type='submit']", elements.get("#super-ticket-ocr-form").submitButton);
    elements.set("#super-ticket-ocr-feedback", fakeElement());
    elements.set("#super-ticket-ocr-summary", fakeElement());
    elements.set("#super-ticket-ocr-meta", fakeElement());
    elements.set("#super-ticket-ocr-warning-list", fakeElement());
    elements.set("#super-ticket-ocr-review-panel", fakeElement());
    elements.set("#super-ticket-ocr-table", fakeIncomeTable());
    elements.set("#super-ticket-ocr-empty", fakeElement());
    elements.set("#super-ticket-ocr-confirm-form", fakeSuperTicketOcrConfirmForm(elements));
    elements.set("#super-ticket-ocr-confirm-form button[type='submit']", elements.get("#super-ticket-ocr-confirm-form").submitButton);
    elements.set("#super-ticket-ocr-product", fakeSelect());
    elements.set("#super-ticket-ocr-date-candidate", fakeClickableSelect());
    elements.set("#super-ticket-ocr-source-candidate", fakeClickableSelect());
    elements.set("#super-ticket-ocr-selected-warnings", fakeElement());
    elements.set("#super-ticket-ocr-confirm-feedback", fakeElement());
    elements.set("#super-ticket-ocr-discard", fakeClickableButton("Descartar revisión"));
    elements.set("#super-price-observation-form", fakeSuperPriceObservationForm(elements));
    elements.set("#super-price-observation-form button[type='submit']", elements.get("#super-price-observation-form").submitButton);
    elements.set("#super-price-observation-item", fakeClickableSelect());
    elements.set("#super-price-observation-price-source", fakeSelect());
    elements.set("#super-price-observation-feedback", fakeElement());
    elements.set("#super-price-observation-title", fakeElement());
    elements.set("#super-price-observation-context-summary", fakeElement());
    elements.set("#super-price-observation-global-reset", fakeClickableButton("Ver historial global"));
    elements.set("#super-price-source-form", fakeSuperPriceSourceForm(elements));
    elements.set("#super-price-source-form button[type='submit']", elements.get("#super-price-source-form").submitButton);
    elements.set("#super-price-source-feedback", fakeElement());
    elements.set("#super-price-observation-table", fakeIncomeTable());
    elements.set("#super-price-observation-empty", fakeElement());
    elements.set("#super-barcode-form", fakeSuperBarcodeForm(elements));
    elements.set("#super-barcode-form button[type='submit']", elements.get("#super-barcode-form").submitButton);
    elements.set("#super-barcode-item", fakeSelect());
    elements.set("#super-barcode-attach", fakeClickableButton("Asociar a producto existente"));
    elements.set("#super-barcode-remove", fakeClickableButton("Quitar alias"));
    elements.set("#super-barcode-scan-start", fakeClickableButton("Escanear código"));
    elements.set("#super-barcode-scan-stop", fakeClickableButton("Detener escaneo"));
    elements.get("#super-barcode-scan-stop").disabled = true;
    elements.set("#super-barcode-scanner", fakeElement());
    elements.get("#super-barcode-scanner").hidden = true;
    elements.set("#super-barcode-scanner-preview", fakeVideoElement());
    elements.get("#super-barcode-scanner-preview").hidden = true;
    elements.set("#super-barcode-scanner-status", fakeElement());
    elements.set("#super-barcode-actions", fakeElement());
    elements.get("#super-barcode-actions").hidden = true;
    elements.set("#super-barcode-purchase", fakeClickableButton("Registrar compra"));
    elements.get("#super-barcode-purchase").dataset.superBarcodeStockAction = "purchase";
    elements.get("#super-barcode-purchase").disabled = true;
    elements.set("#super-barcode-consume", fakeClickableButton("Registrar consumo"));
    elements.get("#super-barcode-consume").dataset.superBarcodeStockAction = "consume";
    elements.get("#super-barcode-consume").disabled = true;
    elements.set("#super-barcode-feedback", fakeElement());
    elements.set("#super-barcode-result", fakeElement());
    elements.set("#super-session-panel", fakeElement());
    elements.set("#super-session-summary", fakeElement());
    elements.set("#super-session-status", fakeElement());
    elements.set("#super-session-lines", fakeIncomeTable());
    elements.set("#super-session-empty", fakeElement());
    elements.set("#super-session-draft-form", fakeForm(elements));
    elements.set("#super-session-draft-type", fakeClickableSelect("PURCHASE"));
    elements.set("#super-session-cancel-draft", fakeClickableButton("Cancelar edición"));
    elements.set("#super-session-clear", fakeClickableButton("Vaciar sesión"));
    elements.set("#super-session-save-draft", fakeClickableButton("Guardar borrador"));
    elements.set("#super-session-confirm", fakeClickableButton("Confirmar sesión"));
    elements.set("#super-category-form", superCategoryForm);
    elements.set("#super-category-form button[type='submit']", superCategoryForm.submitButton);
    elements.set("#super-category-feedback", fakeElement());
    elements.set("#super-category-list", fakeIncomeTable());
    elements.set("#super-category-table-wrap", fakeElement());
    elements.set("#super-category-toggle", fakeClickableButton("Mostrar categorías"));
    elements.set("#super-item-form", superItemForm);
    elements.set("#super-item-form button[type='submit']", superItemForm.submitButton);
    elements.set("#super-item-submit", superItemForm.submitButton);
    elements.set("#super-item-cancel-edit", fakeClickableButton("Cancelar edición"));
    elements.set("#super-generate-list", fakeClickableButton("Generar lista"));
    elements.set("#super-uncheck-all", fakeClickableButton("Desmarcar todos"));
    elements.set("#super-copy-list", fakeClickableButton("Copiar"));
    elements.set("#super-download-list", fakeClickableButton("Descargar TXT"));
    elements.set("#super-whatsapp-list", fakeClickableButton("Compartir por WhatsApp"));
    elements.set("#super-items-table", table);
    elements.set("#super-items-empty", fakeElement());
    elements.set("#super-items-summary", fakeElement());
    elements.set("#super-suggested-list", fakeElement());
    elements.set("#super-suggested-empty", fakeElement());
    elements.set("#super-suggested-summary", fakeElement());
    elements.set("#super-generated-list", fakeElement());
    elements.set("#super-feedback", fakeElement());
    elements.set("#super-movement-modal", fakeElement());
    elements.get("#super-movement-modal").hidden = true;
    elements.set("#super-movement-form", fakeSuperMovementForm(elements));
    elements.set("#super-movement-title", fakeElement());
    elements.set("#super-movement-item-id", fakeInput());
    elements.set("#super-movement-type", fakeInput());
    elements.set("#super-movement-item-name", fakeElement());
    elements.set("#super-movement-quantity", fakeInput());
    elements.set("#super-movement-notes", fakeInput());
    elements.set(".super-movement-negative-field", fakeElement());
    elements.set("#super-movement-allow-negative", fakeInput());
    elements.get("#super-movement-allow-negative").checked = false;
    elements.set("#super-movement-conflict", fakeElement());
    elements.set("#super-movement-feedback", fakeElement());
    elements.set("#super-movement-submit", elements.get("#super-movement-form").submitButton);
    elements.set("#super-movement-cancel", fakeClickableButton("Cancelar"));
    elements.set("#super-movement-close", fakeClickableButton("Cerrar"));
    elements.set("#super-movement-history", fakeElement());
    elements.set("#super-movement-history-title", fakeElement());
    elements.set("#super-movement-history-table", fakeIncomeTable());
    elements.set("#super-movement-history-empty", fakeElement());

    const calls = [];
    const categories = [
        { id: 4, name: "Almacén", active: true },
        { id: 5, name: "Verdulería", active: true }
    ];
    const items = [
        superItemFixture({ id: 10, name: "Arroz", categoryId: 4, categoryName: "Almacén", checked: true, notes: "Doble carolina", unit: "kg", habitualObjective: "2.000", quickQuantity: "1.000", currentStock: null, commercialPresentationLabel: "Pack x 6", commercialPresentationQuantity: "6.000", commercialPresentationPricePesos: "1250.50", commercialPresentationPriceSourceId: 7, commercialPresentationPriceSourceLabel: "Ticket proveedor", commercialPresentationPriceObservedDate: "2026-07-18", configured: true }),
        superItemFixture({ id: 11, name: "Banana", categoryId: 5, categoryName: "Verdulería", checked: true, unit: null, habitualObjective: null, currentStock: "0", configured: false }),
        superItemFixture({ id: 12, name: "Zanahoria", categoryId: 5, categoryName: "Verdulería", checked: false, commercialPresentationLabel: "Atado", commercialPresentationPricePesos: "800.00", commercialPresentationPriceSourceLabel: "Feria barrial", configured: true })
    ];
    const suggestedItems = [
        { itemId: 10, name: "Arroz", categoryId: 4, categoryName: "Almacén", unit: "kg", habitualObjective: "3.000", currentStock: "1.000", suggestedQuantity: "2.000" }
    ];
    const priceObservations = [
        { id: 301, itemId: 10, itemName: "Arroz", pricePesos: "1250.50", sourceLabel: "Ticket proveedor", observedDate: "2026-07-18", presentationLabelSnapshot: "Pack x 6", presentationQuantitySnapshot: "6.000", createdAt: "2026-07-18T12:30:00" },
        { id: 302, itemId: 11, itemName: "Banana", pricePesos: "900.00", sourceLabel: null, observedDate: null, presentationLabelSnapshot: "Unidad", presentationQuantitySnapshot: null, createdAt: "2026-07-17T11:00:00" }
    ];
    const priceSources = [
        { id: 7, name: "Ticket proveedor", active: true },
        { id: 8, name: "Lista mayorista", active: true }
    ];
    const ticketOcrResponse = {
        checksumSha256: "abc123",
        originalFilename: "ticket.png",
        contentType: "image/png",
        sizeBytes: 12345,
        ocrConfidence: "0.87",
        dateCandidates: [{ value: "2026-07-18", confidence: "0.80", warnings: [] }],
        sourceCandidates: [{ label: "Ticket proveedor", confidence: "0.75", warnings: [] }],
        lineCandidates: [
            { rawText: "ARROZ 1250,50", descriptionCandidate: "Arroz", pricePesos: "1250.50", confidence: "0.87", warnings: [] },
            { rawText: "BANANA ???", descriptionCandidate: "Banana", pricePesos: "900.00", confidence: "0.42", warnings: ["Low OCR confidence; review this line manually"] }
        ],
        warnings: ["Review OCR output before saving"]
    };
    let activeSession = { id: 12, state: "ACTIVE", expiresAt: "2026-07-27T23:00:00Z", resolvedItems: [], drafts: [] };
    let nextResolvedItemId = 801;
    let nextDraftId = 901;
    const api = {
        calls,
        async superCategories() {
            calls.push({ method: "superCategories" });
            return categories;
        },
        async createSuperCategory(payload) {
            calls.push({ method: "createSuperCategory", payload });
        },
        async updateSuperCategory(id, payload) {
            calls.push({ method: "updateSuperCategory", id, payload });
        },
        async deleteSuperCategory(id) {
            calls.push({ method: "deleteSuperCategory", id });
        },
        async superItems() {
            calls.push({ method: "superItems" });
            return items;
        },
        async superSuggestedList() {
            calls.push({ method: "superSuggestedList" });
            return suggestedItems;
        },
        async createSuperItem(payload) {
            calls.push({ method: "createSuperItem", payload });
            return { id: 99, ...payload };
        },
        async updateSuperItem(id, payload) {
            calls.push({ method: "updateSuperItem", id, payload });
        },
        async adjustSuperItemStock(id, currentStock) {
            calls.push({ method: "adjustSuperItemStock", id, currentStock });
        },
        async purchaseSuperItem(id, payload) {
            calls.push({ method: "purchaseSuperItem", id, payload });
            return { ...items.find((item) => String(item.id) === String(id)), currentStock: "2.000" };
        },
        async consumeSuperItem(id, payload) {
            calls.push({ method: "consumeSuperItem", id, payload });
            if (!payload.allowNegativeStock && payload.quantity === "5.000") {
                const error = new Error("Reintente con allowNegativeStock=true para confirmar.");
                error.status = 409;
                error.body = {
                    itemId: Number(id),
                    itemName: "Arroz",
                    currentStock: "1.000",
                    quantity: payload.quantity,
                    resultingStock: "-4.000",
                    movementType: "CONSUMPTION"
                };
                error.details = ["Reintente con allowNegativeStock=true para confirmar."];
                error.resultingStock = "-4.000";
                error.movementType = "CONSUMPTION";
                throw error;
            }
            return { ...items.find((item) => String(item.id) === String(id)), currentStock: payload.allowNegativeStock ? "-4.000" : "1.000" };
        },
        async quickConsumeSuperItem(id, payload) {
            calls.push({ method: "quickConsumeSuperItem", id, payload });
            if (!payload.allowNegativeStock && String(id) === "10") {
                const error = new Error("Reintente con allowNegativeStock=true para confirmar.");
                error.status = 409;
                error.body = {
                    itemId: Number(id),
                    itemName: "Arroz",
                    currentStock: "1.000",
                    quantity: "2.000",
                    resultingStock: "-1.000",
                    movementType: "QUICK_CONSUMPTION"
                };
                error.details = ["Reintente con allowNegativeStock=true para confirmar."];
                error.resultingStock = "-1.000";
                error.movementType = "QUICK_CONSUMPTION";
                throw error;
            }
            return { ...items.find((item) => String(item.id) === String(id)), currentStock: "0.000" };
        },
        async createSuperItemPriceObservation(id, payload) {
            calls.push({ method: "createSuperItemPriceObservation", id, payload });
            return { id: 399, itemId: Number(id), itemName: "Arroz", ...payload, presentationLabelSnapshot: "Pack x 6", presentationQuantitySnapshot: "6.000", createdAt: "2026-07-18T13:00:00" };
        },
        async superPriceSources() {
            calls.push({ method: "superPriceSources" });
            return priceSources;
        },
        async activeSuperScanSession() {
            calls.push({ method: "activeSuperScanSession" });
            return structuredClone(activeSession);
        },
        async createActiveSuperScanSession() {
            calls.push({ method: "createActiveSuperScanSession" });
            activeSession = { id: activeSession.id + 1, state: "ACTIVE", expiresAt: "2026-07-28T00:00:00Z", resolvedItems: [], drafts: [] };
            return structuredClone(activeSession);
        },
        async queueSuperScanSessionResolvedItem(sessionId, payload) {
            calls.push({ method: "queueSuperScanSessionResolvedItem", sessionId, payload });
            activeSession = {
                ...activeSession,
                id: Number(sessionId),
                resolvedItems: [...activeSession.resolvedItems, { id: nextResolvedItemId++, itemId: payload.itemId, itemName: items.find((item) => item.id === payload.itemId)?.name || "Producto", barcodeCode: payload.barcodeCode || null }]
            };
            return structuredClone(activeSession);
        },
        async createSuperScanSessionDraft(sessionId, payload) {
            calls.push({ method: "createSuperScanSessionDraft", sessionId, payload });
            activeSession = {
                ...activeSession,
                id: Number(sessionId),
                drafts: [...activeSession.drafts, { id: nextDraftId++, itemId: payload.itemId, itemName: items.find((item) => item.id === payload.itemId)?.name || "Producto", type: payload.type, quantity: payload.quantity, notes: payload.notes || "", allowNegativeStock: Boolean(payload.allowNegativeStock) }]
            };
            return structuredClone(activeSession);
        },
        async updateSuperScanSessionDraft(sessionId, draftId, payload) {
            calls.push({ method: "updateSuperScanSessionDraft", sessionId, draftId, payload });
            activeSession = {
                ...activeSession,
                id: Number(sessionId),
                drafts: activeSession.drafts.map((draft) => draft.id === Number(draftId) ? { ...draft, ...payload, itemName: draft.itemName, itemId: draft.itemId } : draft)
            };
            return structuredClone(activeSession);
        },
        async deleteSuperScanSessionDraft(sessionId, draftId) {
            calls.push({ method: "deleteSuperScanSessionDraft", sessionId, draftId });
            activeSession = {
                ...activeSession,
                id: Number(sessionId),
                drafts: activeSession.drafts.filter((draft) => draft.id !== Number(draftId))
            };
            return structuredClone(activeSession);
        },
        async confirmSuperScanSession(sessionId) {
            calls.push({ method: "confirmSuperScanSession", sessionId });
            const movements = activeSession.drafts.map((draft) => ({ itemId: draft.itemId, itemName: draft.itemName, movementType: draft.type, quantity: draft.quantity, resultingStock: draft.type === "PURCHASE" ? "3.000" : "0.000", source: "SCAN_SESSION" }));
            activeSession = { ...activeSession, id: Number(sessionId), state: "CONFIRMED" };
            return { session: structuredClone(activeSession), movements };
        },
        async createSuperPriceSource(payload) {
            calls.push({ method: "createSuperPriceSource", payload });
            const source = { id: 9, name: payload.name, active: true };
            priceSources.push(source);
            return source;
        },
        async uploadSuperTicketOcrCandidates(file) {
            calls.push({ method: "uploadSuperTicketOcrCandidates", file });
            return { ...ticketOcrResponse, originalFilename: file?.name || ticketOcrResponse.originalFilename, contentType: file?.type || ticketOcrResponse.contentType, sizeBytes: file?.size || ticketOcrResponse.sizeBytes };
        },
        async superPriceObservations(filters = {}) {
            calls.push({ method: "superPriceObservations", filters });
            if (filters.itemId) {
                return priceObservations.filter((observation) => String(observation.itemId) === String(filters.itemId));
            }
            return priceObservations;
        },
        async superStockMovements(filters = {}) {
            calls.push({ method: "superStockMovements", filters });
            return [
                { id: 1, itemId: 10, itemName: "Arroz", itemUnit: "kg", movementType: "PURCHASE", quantity: "2.000", previousStock: "1.000", resultingStock: "3.000", notes: "Reposición", createdAt: "2026-07-14T20:00:00" },
                { id: 2, itemId: 10, itemName: "Arroz", itemUnit: "kg", movementType: "QUICK_CONSUMPTION", quantity: "1.000", previousStock: "3.000", resultingStock: "2.000", notes: "", createdAt: "2026-07-14T21:00:00" }
            ];
        },
        async lookupSuperItemBarcodeAlias(code) {
            calls.push({ method: "lookupSuperItemBarcodeAlias", code });
            if (code === "0075012345678") {
                return { found: true, code, aliasId: 44, format: "EAN_13", item: items[0] };
            }
            return { found: false, code };
        },
        async attachSuperItemBarcodeAlias(id, payload) {
            calls.push({ method: "attachSuperItemBarcodeAlias", id, payload });
            return { id: 45, itemId: Number(id), code: payload.code, format: payload.format || null, active: true };
        },
        async removeSuperItemBarcodeAlias(itemId, aliasId) {
            calls.push({ method: "removeSuperItemBarcodeAlias", itemId, aliasId });
        },
        async deleteSuperItem(id) {
            calls.push({ method: "deleteSuperItem", id });
        },
        async updateSuperItemChecked(id, checked) {
            calls.push({ method: "updateSuperItemChecked", id, checked });
        },
        async uncheckAllSuperItems() {
            calls.push({ method: "uncheckAllSuperItems" });
        }
    };

    return {
        api,
        copiedText: "",
        document: {
            hidden: false,
            addEventListener(type, listener) {
                documentListeners.set(type, listener);
            },
            createElement(tagName) {
                assert.ok(["a", "form", "option", "p", "tr"].includes(tagName), `Unexpected supermarket element: ${tagName}`);
                if (tagName === "option") {
                    return { value: "", textContent: "" };
                }
                if (tagName === "form") {
                    return fakeAppElement();
                }
                return fakeElement();
            },
            querySelector(selector) {
                if (selector.startsWith("#super-category-edit-")) {
                    return { focus() {} };
                }
                assert.ok(elements.has(selector), `Unexpected supermarket selector: ${selector}`);
                return elements.get(selector);
            },
            querySelectorAll(selector) {
                if (selector === "[data-super-limit]") {
                    return supermarketLimitFields(elements);
                }
                if (selector === "[data-super-tab-target]") {
                    return supermarketTabButtons;
                }
                if (selector === "[data-super-tab-panel]") {
                    return supermarketTabPanels;
                }
                throw new Error(`Unexpected supermarket selectorAll: ${selector}`);
            },
            dispatchEvent(event) {
                documentListeners.get(event.type)?.(event);
            }
        },
        elements,
        supermarketTabs: {
            buttonsById: new Map(supermarketTabButtons.map((button) => [button.dataset.superTabTarget, button])),
            sectionsById: new Map(supermarketTabPanels.map((panel) => [panel.dataset.superTabPanel, panel]))
        }
    };
}

function superItemFixture(overrides = {}) {
    return {
        id: 10,
        name: "Arroz",
        categoryId: 4,
        categoryName: "Almacén",
        checked: false,
        notes: "",
        unit: null,
        habitualObjective: null,
        currentStock: null,
        quickQuantity: null,
        commercialPresentationLabel: null,
        commercialPresentationQuantity: null,
        commercialPresentationPricePesos: null,
        commercialPresentationPriceSourceId: null,
        commercialPresentationPriceSourceLabel: null,
        commercialPresentationPriceObservedDate: null,
        configured: false,
        active: true,
        ...overrides
    };
}

async function readSupermarketLimitConstants() {
    const source = await readFile(path.resolve("src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java"), "utf8");
    return {
        CATEGORY_NAME_MAX_LENGTH: javaIntConstant(source, "CATEGORY_NAME_MAX_LENGTH"),
        ITEM_NAME_MAX_LENGTH: javaIntConstant(source, "ITEM_NAME_MAX_LENGTH"),
        ITEM_NOTES_MAX_LENGTH: javaIntConstant(source, "ITEM_NOTES_MAX_LENGTH"),
        ITEM_UNIT_MAX_LENGTH: javaIntConstant(source, "ITEM_UNIT_MAX_LENGTH"),
        BARCODE_CODE_MAX_LENGTH: javaIntConstant(source, "BARCODE_CODE_MAX_LENGTH"),
        BARCODE_FORMAT_MAX_LENGTH: javaIntConstant(source, "BARCODE_FORMAT_MAX_LENGTH"),
        ITEM_PRESENTATION_LABEL_MAX_LENGTH: javaIntConstant(source, "ITEM_PRESENTATION_LABEL_MAX_LENGTH"),
        ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH: javaIntConstant(source, "ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH")
    };
}

function javaIntConstant(source, constantName) {
    const match = source.match(new RegExp(`public\\s+static\\s+final\\s+int\\s+${constantName}\\s*=\\s*(\\d+)\\s*;`));
    assert.ok(match, `Missing Java limit constant ${constantName}`);
    return Number(match[1]);
}

function fakeSuperItemForm(elements) {
    const form = fakeForm(elements);
    form.submitButton = fakeButton("Crear producto");
    form.querySelector = (selector) => {
        assert.equal(selector, "button[type='submit']");
        return form.submitButton;
    };
    form.reset = function resetSuperItemForm() {
        this.resetCount += 1;
        for (const selector of ["#super-item-name", "#super-item-category", "#super-item-unit", "#super-item-presentation-label", "#super-item-presentation-quantity", "#super-item-presentation-price-pesos", "#super-item-presentation-price-source", "#super-item-presentation-price-source-label", "#super-item-presentation-price-observed-date", "#super-item-objective", "#super-item-notes", "#super-item-quick-quantity", "#super-item-current-stock"]) {
            elements.get(selector).value = "";
        }
    };
    return form;
}

function fakeSuperPriceObservationForm(elements) {
    const form = fakeForm(elements);
    form.submitButton = fakeButton("Registrar observación");
    form.querySelector = (selector) => {
        assert.equal(selector, "button[type='submit']");
        return form.submitButton;
    };
    form.reset = function resetSuperPriceObservationForm() {
        this.resetCount += 1;
        for (const selector of ["#super-price-observation-item", "#super-price-observation-price-pesos", "#super-price-observation-price-source", "#super-price-observation-source-label", "#super-price-observation-observed-date"]) {
            elements.get(selector).value = "";
        }
        elements.get("#super-price-observation-sync-current-reference-price").checked = false;
    };
    return form;
}

function fakeSuperPriceSourceForm(elements) {
    const form = fakeForm(elements);
    form.submitButton = fakeButton("Crear fuente");
    form.querySelector = (selector) => {
        assert.equal(selector, "button[type='submit']");
        return form.submitButton;
    };
    form.reset = function resetSuperPriceSourceForm() {
        this.resetCount += 1;
        elements.get("#super-price-source-name").value = "";
    };
    return form;
}

function fakeSuperTicketOcrForm(elements) {
    const form = fakeForm(elements);
    form.submitButton = fakeButton("Extraer candidatos");
    form.querySelector = (selector) => {
        assert.equal(selector, "button[type='submit']");
        return form.submitButton;
    };
    form.reset = function resetSuperTicketOcrForm() {
        this.resetCount += 1;
        elements.get("#super-ticket-ocr-file").files = [];
    };
    return form;
}

function fakeSuperTicketOcrConfirmForm(elements) {
    const form = fakeForm(elements);
    form.submitButton = fakeButton("Confirmar observación");
    form.querySelector = (selector) => {
        assert.equal(selector, "button[type='submit']");
        return form.submitButton;
    };
    form.reset = function resetSuperTicketOcrConfirmForm() {
        this.resetCount += 1;
        for (const selector of ["#super-ticket-ocr-line-index", "#super-ticket-ocr-selected-line", "#super-ticket-ocr-description", "#super-ticket-ocr-price-pesos", "#super-ticket-ocr-product", "#super-ticket-ocr-date-candidate", "#super-ticket-ocr-date", "#super-ticket-ocr-source-candidate", "#super-ticket-ocr-source-label"]) {
            elements.get(selector).value = "";
        }
        elements.get("#super-ticket-ocr-sync-current-reference-price").checked = false;
    };
    return form;
}

function fakeSuperCategoryForm(elements) {
    const form = fakeForm(elements);
    form.submitButton = fakeButton("Crear categoría");
    form.querySelector = (selector) => {
        assert.equal(selector, "button[type='submit']");
        return form.submitButton;
    };
    form.reset = function resetSuperCategoryForm() {
        this.resetCount += 1;
        elements.get("#super-category-name").value = "";
    };
    return form;
}

function fakeSuperMovementForm(elements) {
    const form = fakeForm(elements);
    form.submitButton = fakeButton("Registrar movimiento");
    form.querySelector = (selector) => {
        assert.equal(selector, "button[type='submit']");
        return form.submitButton;
    };
    form.reset = function resetSuperMovementForm() {
        this.resetCount += 1;
        for (const selector of ["#super-movement-item-id", "#super-movement-type", "#super-movement-quantity", "#super-movement-notes"]) {
            elements.get(selector).value = "";
        }
        elements.get("#super-movement-allow-negative").checked = false;
    };
    return form;
}

function fakeSuperBarcodeForm(elements) {
    const form = fakeForm(elements);
    form.submitButton = fakeButton("Buscar código local");
    form.querySelector = (selector) => {
        assert.equal(selector, "button[type='submit']");
        return form.submitButton;
    };
    form.reset = function resetSuperBarcodeForm() {
        this.resetCount += 1;
        for (const selector of ["#super-barcode-code", "#super-barcode-format", "#super-barcode-item"]) {
            elements.get(selector).value = "";
        }
    };
    return form;
}

function fakeSuperItemActionButton(action, row, id) {
    const button = fakeButton(action);
    button.dataset.superAction = action;
    button.dataset.superItemId = id;
    button.closest = (selector) => {
        if (selector === "button[data-super-action]") {
            return button;
        }
        if (selector === "tr[data-super-item-id]") {
            return row;
        }
        return null;
    };
    return button;
}

function fakeSuperTicketOcrActionButton(action, index) {
    const button = fakeButton(action);
    button.dataset.superTicketOcrAction = action;
    button.dataset.superTicketOcrLineIndex = index;
    button.closest = (selector) => selector === "button[data-super-ticket-ocr-action]" ? button : null;
    return button;
}

function fakeSuperCategoryActionButton(action, row, id) {
    const button = fakeButton(action);
    button.dataset.superCategoryAction = action;
    button.dataset.superCategoryId = id;
    button.closest = (selector) => {
        if (selector === "button[data-super-category-action]") {
            return button;
        }
        if (selector === "tr[data-super-category-id]") {
            return row;
        }
        return null;
    };
    return button;
}

function fakeSimulatorDom() {
    const elements = new Map();
    const table = fakeManualExpenseTable();
    const simulatorForm = fakeSimulatorForm(elements);

    for (const selector of [
        "#simulator-description",
        "#simulator-total-amount",
        "#simulator-installment-count",
        "#simulator-start-month",
        "#simulator-category"
    ]) {
        elements.set(selector, fakeInput());
    }
    elements.set("#simulator-form", simulatorForm);
    elements.set("#simulator-form button[type='submit']", simulatorForm.submitButton);
    elements.set("#clear-simulation", fakeClickable());
    elements.set("#simulation-results-table", table);
    elements.set("#simulation-empty", fakeElement());
    elements.set("#simulator-summary", fakeElement());
    elements.set("#simulator-feedback", fakeElement());

    const calls = [];
    const summaries = new Map([
        ["2026-11", { incomeTotalPesos: 1000, expenseTotalPesos: 300, monthlyBalancePesos: 700 }],
        ["2026-12", { incomeTotalPesos: 1500, expenseTotalPesos: 500, monthlyBalancePesos: 1000 }],
        ["2027-01", { incomeTotalPesos: 900, expenseTotalPesos: 100, monthlyBalancePesos: 800 }]
    ]);
    const api = {
        calls,
        async summary(month) {
            calls.push({ method: "summary", month });
            return summaries.get(month) || { incomeTotalPesos: 0, expenseTotalPesos: 0, monthlyBalancePesos: 0 };
        }
    };

    return {
        api,
        document: {
            createElement(tagName) {
                assert.equal(tagName, "tr");
                return fakeElement();
            },
            querySelector(selector) {
                assert.ok(elements.has(selector), `Unexpected simulator selector: ${selector}`);
                return elements.get(selector);
            }
        },
        elements
    };
}

function fakeSimulatorForm(elements) {
    const form = fakeForm(elements);
    form.submitButton = fakeButton("Simular compra");
    form.querySelector = (selector) => {
        assert.equal(selector, "button[type='submit']");
        return form.submitButton;
    };
    form.reset = function resetSimulatorForm() {
        this.resetCount += 1;
        for (const selector of [
            "#simulator-description",
            "#simulator-total-amount",
            "#simulator-installment-count",
            "#simulator-start-month",
            "#simulator-category"
        ]) {
            elements.get(selector).value = "";
        }
    };
    return form;
}

function fakeManualExpenseForm(elements) {
    const form = fakeForm(elements);
    form.submitButton = fakeButton("Crear gasto manual");
    form.querySelector = (selector) => {
        assert.equal(selector, "button[type='submit']");
        return form.submitButton;
    };
    form.reset = function resetManualExpenseForm() {
        this.resetCount += 1;
        for (const selector of [
            "#manual-expense-description",
            "#manual-expense-amount-pesos",
            "#manual-expense-amount-usd",
            "#manual-expense-start-month",
            "#manual-expense-total-installments",
            "#manual-expense-current-installment",
            "#manual-expense-category",
            "#manual-expense-notes"
        ]) {
            elements.get(selector).value = "";
        }
        elements.get("#manual-expense-type").value = "ONE_PAYMENT";
    };
    return form;
}

function fakeManualExpenseTable() {
    const element = fakeElement();
    Object.defineProperty(element, "innerHTML", {
        get() {
            return this._innerHTML || "";
        },
        set(value) {
            this._innerHTML = value;
            if (value === "") {
                this.children = [];
            }
        }
    });
    return element;
}

function fakeManualExpenseRow() {
    const row = fakeElement();
    const deleteButton = fakeManualExpenseDeleteButton();
    row.deleteButton = deleteButton;
    row.querySelector = (selector) => {
        assert.equal(selector, "[data-delete-manual-expense]");
        return deleteButton;
    };
    return row;
}

function fakeManualExpenseDeleteButton() {
    const listeners = new Map();
    return {
        addEventListener(type, listener) {
            listeners.set(type, listener);
        },
        async click() {
            await listeners.get("click")?.({});
        }
    };
}

function manualExpenseFixture(overrides = {}) {
    return {
        id: 77,
        description: "Préstamo personal",
        type: "LOAN",
        amountPesos: 1000.5,
        amountUsd: 5.25,
        startMonth: "2026-07",
        totalInstallments: 6,
        currentInstallment: 2,
        installmentNumber: 2,
        projected: false,
        category: null,
        notes: "Banco",
        ...overrides
    };
}

function fakeInput(value = "") {
    return {
        attributes: new Map(),
        dataset: {},
        focused: false,
        maxLength: -1,
        value,
        focus() {
            this.focused = true;
        },
        setAttribute(name, attributeValue) {
            this.attributes.set(name, attributeValue);
        }
    };
}

function fakeMediaStream(onStop) {
    const track = {
        stopped: false,
        stop() {
            this.stopped = true;
            onStop?.();
        }
    };
    return {
        getTracks() {
            return [track];
        }
    };
}

function supermarketLimitFields(elements) {
    return ["#super-category-name", "#super-item-name", "#super-item-unit", "#super-item-notes", "#super-item-presentation-label", "#super-item-presentation-price-source-label", "#super-price-observation-source-label", "#super-price-source-name", "#super-ticket-ocr-source-label", "#super-barcode-code", "#super-barcode-format"].map((selector) => elements.get(selector));
}

function assertNoUnsupportedSuperInventorySemantics(source) {
    const allowedReferencePriceSource = source
        .replaceAll("super-item-presentation-price-pesos", "")
        .replaceAll("super-item-presentation-price-source", "")
        .replaceAll("super-item-presentation-price-source-label", "")
        .replaceAll("super-item-presentation-price-observed-date", "")
        .replaceAll("commercialPresentationPricePesos", "")
        .replaceAll("commercialPresentationPriceSourceId", "")
        .replaceAll("commercialPresentationPriceSourceLabel", "")
        .replaceAll("commercialPresentationPriceObservedDate", "")
        .replaceAll("selectedPriceSourceId", "")
        .replaceAll("priceSourceLabel", "")
        .replaceAll("superItemCommercialPresentationPriceLabel", "")
        .replaceAll("superItemCommercialPresentationPriceSourceLabel", "")
        .replaceAll("superItemCommercialPresentationPriceObservedDateLabel", "")
        .replaceAll("superItemCommercialPresentationPriceHtml", "")
        .replaceAll("super-price-observation-form", "")
        .replaceAll("super-price-observation-item", "")
        .replaceAll("super-price-observation-price-pesos", "")
        .replaceAll("super-price-observation-source-label", "")
        .replaceAll("super-price-observation-price-source", "")
        .replaceAll("super-price-source-form", "")
        .replaceAll("super-price-source-name", "")
        .replaceAll("super-price-source-feedback", "")
        .replaceAll("super-price-observation-observed-date", "")
        .replaceAll("super-price-observation-sync-current-reference-price", "")
        .replaceAll("super-price-observation-table", "")
        .replaceAll("super-price-observation-empty", "")
        .replaceAll("super-price-observation-feedback", "")
        .replaceAll("super-price-observation-title", "")
        .replaceAll("super-price-observation-context-summary", "")
        .replaceAll("super-price-observation-global-reset", "")
        .replaceAll("price-history", "")
        .replaceAll("selectedPriceObservationItem", "")
        .replaceAll("renderSuperPriceObservationContext", "")
        .replaceAll("resetSuperPriceObservationContext", "")
        .replaceAll("superPriceObservationPayloadFromValues", "")
        .replaceAll("validateSuperPriceObservationPayload", "")
        .replaceAll("superPriceSourcePayloadFromValues", "")
        .replaceAll("validateSuperPriceSourcePayload", "")
        .replaceAll("renderSuperItemPriceSourceOptions", "")
        .replaceAll("refreshSuperItemPriceSourceOptions", "")
        .replaceAll("syncSuperItemPriceSourceInputs", "")
        .replaceAll("submitSuperPriceSourceForm", "")
        .replaceAll("loadSuperPriceSources", "")
        .replaceAll("renderSuperPriceSources", "")
        .replaceAll("createSuperPriceSource", "")
        .replaceAll("superPriceSources", "")
        .replaceAll("priceSourceId", "")
        .replaceAll("priceSource", "")
        .replaceAll("SuperPriceSource", "")
        .replaceAll("superPriceObservationPresentationLabel", "")
        .replaceAll("superPriceObservationRowHtml", "")
        .replaceAll("submitSuperPriceObservationForm", "")
        .replaceAll("loadSuperPriceObservations", "")
        .replaceAll("renderSuperPriceObservations", "")
        .replaceAll("renderSuperPriceObservationItemOptions", "")
        .replaceAll("prefillSuperPriceObservationForm", "")
        .replaceAll("createSuperItemPriceObservation", "")
        .replaceAll("superPriceObservations", "")
        .replaceAll("syncCurrentReferencePrice", "")
        .replaceAll("pricePesos", "")
        .replaceAll("20260718-super-inventory-stage10-price-observations-api", "")
        .replaceAll("20260718-super-inventory-stage10-price-observations-ui", "")
        .replaceAll("20260718-super-inventory-stage11-price-sources-api", "")
        .replaceAll("20260718-super-inventory-stage11-price-sources-ui", "")
        .replaceAll("20260725-super-inventory-stage15-ticket-ocr-ui-api", "")
        .replaceAll("20260725-super-inventory-stage15-ticket-ocr-ui", "")
        .replaceAll("super-ticket-ocr", "")
        .replaceAll("TicketOcr", "")
        .replaceAll("submitSuperTicketOcrUploadForm", "")
        .replaceAll("submitSuperTicketOcrConfirmForm", "")
        .replaceAll("renderSuperTicketOcrReview", "")
        .replaceAll("clearSuperTicketOcrReview", "")
        .replaceAll("uploadSuperTicketOcrCandidates", "")
        .replaceAll("ticketOcr", "")
        .replaceAll("OCR", "")
        .replaceAll("ocr", "")
        .replaceAll("ticket image", "")
        .replaceAll("ticket", "")
        .replaceAll("price-observations", "")
        .replaceAll("price-source", "")
        .replaceAll("price-sources", "")
        .replaceAll("/api/super/price-observations", "")
        .replaceAll("/api/super/price-sources", "")
        .replaceAll("Precio", "")
        .replaceAll("Precio ref.", "");
    const unsupportedTerms = [
        "OpenFoodFacts",
        "Tesseract",
        "externalLookup",
        "store",
        "shop",
        "shops",
        "commerce",
        "comparison",
        "chart",
        "charts",
        "scraping",
        "automation",
        "total",
        "presentations",
        "multiplePresentations",
        "autoPurchase",
        "purchaseAutomation",
        "persistSuggestion",
        "suggestionPersistence",
        "saveSuggestion"
    ];
    for (const term of unsupportedTerms) {
        assert.equal(allowedReferencePriceSource.includes(term), false, `Unexpected unsupported super inventory term: ${term}`);
    }
}

function assertSupermarketMutationAfter(supermarketDom, startIndex, expected) {
    assertSupermarketMutationsAfter(supermarketDom, startIndex, [expected]);
}

function assertSupermarketMutationsAfter(supermarketDom, startIndex, expected) {
    const callsAfterAction = supermarketDom.api.calls.slice(startIndex);
    const mutationCalls = callsAfterAction.filter((call) => !["superCategories", "superItems", "superSuggestedList", "superStockMovements", "superPriceSources", "superPriceObservations", "activeSuperScanSession"].includes(call.method));
    assert.deepEqual(mutationCalls, expected);
}

function fakeClickableInput(value = "") {
    const input = fakeInput(value);
    const listeners = new Map();
    input.addEventListener = (type, listener) => {
        listeners.set(type, listener);
    };
    input.change = async () => {
        await listeners.get("change")?.({ currentTarget: input });
    };
    input.input = async () => {
        await listeners.get("input")?.({ currentTarget: input });
    };
    return input;
}

function fakeSelect(value = "") {
    const options = [];
    return {
        value,
        innerHTML: "",
        get selectedOptions() {
            const selected = options.find((option) => String(option.value) === String(this.value));
            return selected ? [selected] : [];
        },
        append(option) {
            options.push(option);
            this.innerHTML += `<option value="${option.value}">${option.textContent}</option>`;
        }
    };
}

function fakeClickableSelect(value = "") {
    const select = fakeSelect(value);
    const listeners = new Map();
    select.addEventListener = (type, listener) => {
        listeners.set(type, listener);
    };
    select.change = async () => {
        await listeners.get("change")?.({ currentTarget: select });
    };
    return select;
}

function extractPrimaryTabButtons(html) {
    const buttons = [];
    const buttonPattern = /<button\b([^>]*)>([^<]+)<\/button>/g;
    let match;
    while ((match = buttonPattern.exec(html)) !== null) {
        const [, attributes, label] = match;
        const id = attributeValue(attributes, "data-tab-target");
        if (!id) {
            continue;
        }
        buttons.push({
            id,
            label,
            buttonId: attributeValue(attributes, "id"),
            controls: attributeValue(attributes, "aria-controls"),
            selected: attributeValue(attributes, "aria-selected")
        });
    }
    return buttons;
}

function attributeValue(attributes, name) {
    return attributes.match(new RegExp(`${name}="([^"]+)"`))?.[1] ?? "";
}

function assertCssRuleHasDeclarations(css, selector, expectedDeclarations) {
    const declarationMaps = cssRuleBodies(css, selector).map(cssDeclarationMap);
    const hasExpectedDeclarations = declarationMaps.some((declarations) => Object.entries(expectedDeclarations).every(([property, value]) => declarations.get(property) === value));
    assert.ok(hasExpectedDeclarations, `Expected ${selector} to include ${JSON.stringify(expectedDeclarations)}`);
}

function assertCssMediaRuleHasDeclarations(css, mediaHeader, selector, expectedDeclarations) {
    const mediaCss = cssAtRuleBlocks(css, mediaHeader).join("\n");
    assert.ok(mediaCss.length > 0, `Expected ${mediaHeader} to exist`);
    assertCssRuleHasDeclarations(mediaCss, selector, expectedDeclarations);
}

function assertResponsiveCardTableMobileCssContract(css) {
    const mediaHeader = "@media (max-width: 680px)";
    const cardCellColumns = "minmax(6.5rem, var(--responsive-card-label-width)) minmax(0, 1fr)";
    assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table td", {
        "grid-template-columns": cardCellColumns,
        "white-space": "normal",
        "overflow-wrap": "anywhere"
    });
    assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table td::before", { content: "attr(data-label)" });
    assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table .super-category-group-row", {
        padding: "0",
        overflow: "hidden"
    });
    assertNoResponsiveCardCellDeclarationOutsideMedia(css, mediaHeader, "grid-template-columns", cardCellColumns);
    assertNoResponsiveCardCellDeclarationOutsideMedia(css, mediaHeader, "white-space", "normal");
    assertNoResponsiveCardCellDeclarationOutsideMedia(css, mediaHeader, "overflow-wrap", "anywhere");
    assertNoResponsiveCardCellDeclarationOutsideMedia(css, mediaHeader, "content", "attr(data-label)");
    assertNoSupermarketGroupRowDeclarationOutsideMedia(css, mediaHeader, "padding", "0");
    assertNoSupermarketGroupRowDeclarationOutsideMedia(css, mediaHeader, "overflow", "hidden");
    assert.throws(
        () => assertNoResponsiveCardCellDeclarationOutsideMedia(`${css}\n.responsive-card-table td { grid-template-columns: ${cardCellColumns}; }`, mediaHeader, "grid-template-columns", cardCellColumns),
        /Unexpected grid-template-columns: minmax\(6\.5rem, var\(--responsive-card-label-width\)\) minmax\(0, 1fr\) in \.responsive-card-table td/
    );
    assert.throws(
        () => assertNoResponsiveCardCellDeclarationOutsideMedia(`${css}\n.super-items-table-wrap.responsive-card-table [data-label] { overflow-wrap: anywhere; }`, mediaHeader, "overflow-wrap", "anywhere"),
        /Unexpected overflow-wrap: anywhere in \.super-items-table-wrap\.responsive-card-table \[data-label\]/
    );
    assert.throws(
        () => assertNoResponsiveCardCellDeclarationOutsideMedia(`${css}\nbody .responsive-card-table td::before { content: attr(data-label); }`, mediaHeader, "content", "attr(data-label)"),
        /Unexpected content: attr\(data-label\) in body \.responsive-card-table td::before/
    );
    assert.throws(
        () => assertNoSupermarketGroupRowDeclarationOutsideMedia(`${css}\n.responsive-card-table .super-category-group-row { padding: 0; }`, mediaHeader, "padding", "0"),
        /Unexpected padding: 0 in \.responsive-card-table \.super-category-group-row/
    );
}

function assertNoResponsiveCardCellDeclarationOutsideMedia(css, mediaHeader, property, value) {
    assertNoTargetedCssDeclarationOutsideMedia(css, mediaHeader, selectorTargetsResponsiveCardCell, property, value);
}

function assertNoSupermarketGroupRowDeclarationOutsideMedia(css, mediaHeader, property, value) {
    assertNoTargetedCssDeclarationOutsideMedia(css, mediaHeader, selectorTargetsSupermarketGroupRow, property, value);
}

function assertNoTargetedCssDeclarationOutsideMedia(css, mediaHeader, selectorPredicate, property, value) {
    for (const { selectorList, ruleBody } of cssRules(cssWithoutAtRuleBlocks(css, mediaHeader))) {
        const declarations = cssDeclarationMap(ruleBody);
        if (declarations.get(property) !== value) {
            continue;
        }
        const matchingSelector = cssSelectors(selectorList).find(selectorPredicate);
        assert.equal(matchingSelector, undefined, `Unexpected ${property}: ${value} in ${matchingSelector}`);
    }
}

function assertSimulatorResultsCellMobileOverflowContract(css) {
    const mediaHeader = "@media (max-width: 680px)";
    assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-card-table .simulator-results-table td", {
        overflow: "visible",
        "text-overflow": "clip"
    });
    assertNoSimulatorResultsCellDeclarationOutsideMedia(css, mediaHeader, "overflow", "visible");
    assertNoSimulatorResultsCellDeclarationOutsideMedia(css, mediaHeader, "text-overflow", "clip");
    assert.throws(
        () => assertNoSimulatorResultsCellDeclarationOutsideMedia(`${css}\n.simulator-results-table td { overflow: visible; }`, mediaHeader, "overflow", "visible"),
        /Unexpected overflow: visible in \.simulator-results-table td/
    );
    assert.throws(
        () => assertNoSimulatorResultsCellDeclarationOutsideMedia(`${css}\n.simulator-table-wrap \[data-label\] { text-overflow: clip; }`, mediaHeader, "text-overflow", "clip"),
        /Unexpected text-overflow: clip in \.simulator-table-wrap \[data-label\]/
    );
}

function assertDraftEditTableMobileCssContract(css) {
    const mediaHeader = "@media (max-width: 680px)";
    assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table input", {
        "min-width": "0",
        width: "100%"
    });
    assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table select", {
        "min-width": "0",
        width: "100%"
    });
    assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table .row-actions", {
        display: "grid",
        "grid-template-columns": "1fr",
        width: "100%"
    });
    assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table .row-actions button", { width: "100%" });
    assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table .row-actions .secondary-button", { width: "100%" });
    assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table .row-actions .danger-button", { width: "100%" });
    assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table [data-save-transaction]", { width: "100%" });
    assertCssMediaRuleHasDeclarations(css, mediaHeader, ".responsive-edit-table [data-delete-transaction]", { width: "100%" });
    assertNoDraftEditTableDeclarationOutsideMedia(css, mediaHeader, "min-width", "0");
    assertNoDraftEditTableDeclarationOutsideMedia(css, mediaHeader, "width", "100%");
    assertNoDraftEditTableDeclarationOutsideMedia(css, mediaHeader, "grid-template-columns", "1fr");
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\n.responsive-edit-table input { min-width: 0; }`, mediaHeader, "min-width", "0"),
        /Unexpected min-width: 0 in \.responsive-edit-table input/
    );
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\n.draft-table-wrap .row-actions { grid-template-columns: 1fr; }`, mediaHeader, "grid-template-columns", "1fr"),
        /Unexpected grid-template-columns: 1fr in \.draft-table-wrap \.row-actions/
    );
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\n.draft-table-wrap button { width: 100%; }`, mediaHeader, "width", "100%"),
        /Unexpected width: 100% in \.draft-table-wrap button/
    );
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\n.draft-table-wrap .secondary-button { width: 100%; }`, mediaHeader, "width", "100%"),
        /Unexpected width: 100% in \.draft-table-wrap \.secondary-button/
    );
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\n.draft-table-wrap .danger-button { width: 100%; }`, mediaHeader, "width", "100%"),
        /Unexpected width: 100% in \.draft-table-wrap \.danger-button/
    );
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\n.draft-table-wrap [data-save-transaction] { width: 100%; }`, mediaHeader, "width", "100%"),
        /Unexpected width: 100% in \.draft-table-wrap \[data-save-transaction\]/
    );
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\n.draft-table-wrap [data-delete-transaction] { width: 100%; }`, mediaHeader, "width", "100%"),
        /Unexpected width: 100% in \.draft-table-wrap \[data-delete-transaction\]/
    );
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\nbutton { width: 100%; }`, mediaHeader, "width", "100%"),
        /Unexpected width: 100% in button/
    );
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\n.secondary-button { width: 100%; }`, mediaHeader, "width", "100%"),
        /Unexpected width: 100% in \.secondary-button/
    );
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\n.danger-button { width: 100%; }`, mediaHeader, "width", "100%"),
        /Unexpected width: 100% in \.danger-button/
    );
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\n.row-actions button { width: 100%; }`, mediaHeader, "width", "100%"),
        /Unexpected width: 100% in \.row-actions button/
    );
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\n[data-save-transaction] { width: 100%; }`, mediaHeader, "width", "100%"),
        /Unexpected width: 100% in \[data-save-transaction\]/
    );
    assert.throws(
        () => assertNoDraftEditTableDeclarationOutsideMedia(`${css}\n[data-delete-transaction] { width: 100%; }`, mediaHeader, "width", "100%"),
        /Unexpected width: 100% in \[data-delete-transaction\]/
    );
}

function assertNoDraftEditTableDeclarationOutsideMedia(css, mediaHeader, property, value) {
    assertNoTargetedCssDeclarationOutsideMedia(css, mediaHeader, selectorTargetsDraftEditTable, property, value);
}

function assertNoSimulatorResultsCellDeclarationOutsideMedia(css, mediaHeader, property, value) {
    for (const { selectorList, ruleBody } of cssRules(cssWithoutAtRuleBlocks(css, mediaHeader))) {
        const declarations = cssDeclarationMap(ruleBody);
        if (declarations.get(property) !== value) {
            continue;
        }
        const matchingSelector = cssSelectors(selectorList).find(selectorTargetsSimulatorResultsCell);
        assert.equal(matchingSelector, undefined, `Unexpected ${property}: ${value} in ${matchingSelector}`);
    }
}

function assertNoCssDeclarationOutsideMedia(css, mediaHeader, selectors, property, value) {
    assertNoCssDeclaration(cssWithoutAtRuleBlocks(css, mediaHeader), selectors, property, value);
}

function assertNoCssDeclaration(css, selectors, property, value) {
    for (const selector of selectors) {
        for (const declarations of cssRuleBodies(css, selector).map(cssDeclarationMap)) {
            assert.notEqual(declarations.get(property), value, `Unexpected ${property}: ${value} in ${selector}`);
        }
    }
}

function assertNoPageOverflowMask(css) {
    assertNoCssDeclaration(css, ["html", "body"], "overflow-x", "hidden");
    assertNoCssDeclaration(css, ["html", "body"], "overflow", "hidden");
}

function assertResponsiveCardTableAdopterContract(html) {
    assert.equal(responsiveCardTableAdopterIsSafe('<div class="table-wrap responsive-card-table"><table><tbody><tr><td data-label="Amount">ARS 1</td></tr></tbody></table></div>'), true);
    assert.equal(responsiveCardTableAdopterIsSafe('<div class="table-wrap responsive-card-table"><table><tbody><tr><td>ARS 1</td></tr></tbody></table></div>'), false);

    for (const adopter of responsiveCardTableAdopters(html)) {
        const cells = responsiveCardTableCells(adopter);
        if (cells.length > 0) {
            assert.ok(responsiveCardTableAdopterIsSafe(adopter), "responsive-card-table adopters must provide data-label on card cells");
        }
    }
}

function assertResponsiveCardTableMarkup(html) {
    assert.match(html, /class="table-wrap expenses-table-wrap responsive-card-table"/);
    assert.match(html, /class="table-wrap income-table-wrap responsive-card-table"/);
    assert.match(html, /class="table-wrap manual-expense-table-wrap responsive-card-table"/);
    assert.match(html, /class="table-wrap draft-table-wrap responsive-card-table responsive-edit-table"/);
    assert.match(html, /class="table-wrap simulator-table-wrap responsive-card-table"/);
    assert.match(html, /class="table-wrap super-items-table-wrap responsive-card-table"/);
    assert.match(html, /class="table-wrap super-category-table-wrap responsive-card-table"/);
}

function assertResponsiveCardLabels(rowHtml, labels) {
    const cells = responsiveCardTableCells(rowHtml);
    assert.ok(cells.length >= labels.length, `Expected at least ${labels.length} labeled cells`);
    for (const label of labels) {
        assert.match(rowHtml, new RegExp(`\\bdata-label="${escapeRegExp(label)}"`));
    }
    assert.ok(cells.every((cell) => /\bdata-label="[^"]+"/.test(cell[1])), "Every responsive card cell needs a data-label");
}

function responsiveCardTableAdopterIsSafe(html) {
    const cells = responsiveCardTableCells(html);
    return cells.length > 0 && cells.every((cell) => /\bdata-label="[^"]+"/.test(cell[1]));
}

function responsiveCardTableCells(html) {
    return Array.from(html.matchAll(/<td\b([^>]*)>/g));
}

function escapeRegExp(value) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function responsiveCardTableAdopters(html) {
    return Array.from(html.matchAll(/<div\b(?=[^>]*class="[^"]*\bresponsive-card-table\b[^"]*")[^>]*>[\s\S]*?<\/div>/g)).map((match) => match[0]);
}

function cssRuleBodies(css, selector) {
    return cssRules(css)
        .filter(({ selectorList }) => selectorListContains(selectorList, selector))
        .map(({ ruleBody }) => ruleBody);
}

function cssRules(css) {
    return Array.from(css.matchAll(/([^{}]+)\{([^{}]*)\}/g))
        .map((match) => ({ selectorList: match[1], ruleBody: match[2].trim() }));
}

function cssAtRuleBlocks(css, atRuleHeader) {
    const blocks = [];
    let searchIndex = 0;
    while (searchIndex < css.length) {
        const headerIndex = css.indexOf(atRuleHeader, searchIndex);
        if (headerIndex === -1) {
            break;
        }
        const openingBraceIndex = css.indexOf("{", headerIndex + atRuleHeader.length);
        assert.notEqual(openingBraceIndex, -1, `Expected ${atRuleHeader} to have an opening brace`);
        const closingBraceIndex = findMatchingClosingBrace(css, openingBraceIndex);
        blocks.push(css.slice(openingBraceIndex + 1, closingBraceIndex));
        searchIndex = closingBraceIndex + 1;
    }
    return blocks;
}

function cssWithoutAtRuleBlocks(css, atRuleHeader) {
    let result = "";
    let searchIndex = 0;
    while (searchIndex < css.length) {
        const headerIndex = css.indexOf(atRuleHeader, searchIndex);
        if (headerIndex === -1) {
            result += css.slice(searchIndex);
            break;
        }
        const openingBraceIndex = css.indexOf("{", headerIndex + atRuleHeader.length);
        assert.notEqual(openingBraceIndex, -1, `Expected ${atRuleHeader} to have an opening brace`);
        const closingBraceIndex = findMatchingClosingBrace(css, openingBraceIndex);
        result += css.slice(searchIndex, headerIndex);
        searchIndex = closingBraceIndex + 1;
    }
    return result;
}

function findMatchingClosingBrace(css, openingBraceIndex) {
    let depth = 0;
    for (let index = openingBraceIndex; index < css.length; index += 1) {
        if (css[index] === "{") {
            depth += 1;
        } else if (css[index] === "}") {
            depth -= 1;
            if (depth === 0) {
                return index;
            }
        }
    }
    assert.fail("Expected CSS block to have a matching closing brace");
}

function selectorListContains(selectorList, selector) {
    return cssSelectors(selectorList).includes(selector);
}

function cssSelectors(selectorList) {
    return selectorList.split(",").map((part) => part.trim()).filter(Boolean);
}

function selectorTargetsSimulatorResultsCell(selector) {
    const normalizedSelector = selector.replace(/\s+/g, " ").trim();
    const isSimulatorSelector = normalizedSelector.includes(".simulator-results-table")
        || normalizedSelector.includes("#simulation-results-table")
        || normalizedSelector.includes(".simulator-table-wrap");

    return isSimulatorSelector && (
        selectorContainsType(normalizedSelector, "td")
        || normalizedSelector.includes(".simulation-month-cell")
        || normalizedSelector.includes(".simulation-amount-cell")
        || normalizedSelector.includes("[data-label")
        || selectorContainsUniversalTarget(normalizedSelector)
    );
}

function selectorTargetsResponsiveCardCell(selector) {
    const normalizedSelector = selector.replace(/\s+/g, " ").trim();
    return normalizedSelector.includes(".responsive-card-table") && (
        selectorContainsType(normalizedSelector, "td")
        || normalizedSelector.includes("[data-label")
        || selectorContainsUniversalTarget(normalizedSelector)
    );
}

function selectorTargetsSupermarketGroupRow(selector) {
    const normalizedSelector = selector.replace(/\s+/g, " ").trim();
    return normalizedSelector.includes(".responsive-card-table")
        && normalizedSelector.includes(".super-category-group-row");
}

function selectorTargetsDraftEditTable(selector) {
    const normalizedSelector = selector.replace(/\s+/g, " ").trim();
    if (selectorTargetsBroadDraftActionControl(normalizedSelector)) {
        return true;
    }

    const isDraftEditSelector = normalizedSelector.includes(".responsive-edit-table")
        || normalizedSelector.includes(".draft-table-wrap")
        || normalizedSelector.includes("#draft-transactions-table");

    return isDraftEditSelector && (
        selectorContainsType(normalizedSelector, "input")
        || selectorContainsType(normalizedSelector, "select")
        || selectorContainsType(normalizedSelector, "button")
        || normalizedSelector.includes(".row-actions")
        || normalizedSelector.includes(".secondary-button")
        || normalizedSelector.includes(".danger-button")
        || normalizedSelector.includes("[data-save-transaction")
        || normalizedSelector.includes("[data-delete-transaction")
        || normalizedSelector.includes("[name=")
        || selectorContainsUniversalTarget(normalizedSelector)
    );
}

function selectorTargetsBroadDraftActionControl(selector) {
    return selectorIsBareDraftActionControl(selector)
        || selectorTargetsRootDraftActionControl(selector)
        || selectorTargetsRowActionControl(selector);
}

function selectorIsBareDraftActionControl(selector) {
    return selectorMatchesTypePrefix(selector, "button")
        || selectorMatchesClassPrefix(selector, "secondary-button")
        || selectorMatchesClassPrefix(selector, "danger-button")
        || selectorMatchesAttributePrefix(selector, "data-save-transaction")
        || selectorMatchesAttributePrefix(selector, "data-delete-transaction");
}

function selectorTargetsRootDraftActionControl(selector) {
    const rootMatch = selector.match(/^(?:html|body|\*)\s+(.+)$/);
    return rootMatch !== null && selectorIsBareDraftActionControl(rootMatch[1]);
}

function selectorTargetsRowActionControl(selector) {
    return selectorMatchesClassPrefix(selector, "row-actions")
        || selector.includes(".row-actions ") && selectorIncludesDraftActionControl(selector);
}

function selectorIncludesDraftActionControl(selector) {
    return selectorContainsType(selector, "button")
        || selector.includes(".secondary-button")
        || selector.includes(".danger-button")
        || selector.includes("[data-save-transaction")
        || selector.includes("[data-delete-transaction");
}

function selectorMatchesTypePrefix(selector, type) {
    return selectorHasPrefixBoundary(selector, type);
}

function selectorMatchesClassPrefix(selector, className) {
    return selectorHasPrefixBoundary(selector, `.${className}`);
}

function selectorMatchesAttributePrefix(selector, attributeName) {
    if (!selector.startsWith(`[${attributeName}`)) {
        return false;
    }
    const closingBracketIndex = selector.indexOf("]");
    return closingBracketIndex !== -1 && selectorHasPrefixBoundary(selector, selector.slice(0, closingBracketIndex + 1));
}

function selectorHasPrefixBoundary(selector, prefix) {
    if (selector === prefix) {
        return true;
    }
    if (!selector.startsWith(prefix)) {
        return false;
    }
    return [".", "#", ":", "[", " ", ">", "+", "~"].includes(selector[prefix.length]);
}

function selectorContainsType(selector, type) {
    return new RegExp(`(^|[\\s>+~])${type}(?=$|[.#:\\[\\s>+~])`).test(selector);
}

function selectorContainsUniversalTarget(selector) {
    return /(^|[\s>+~])\*(?=$|[.#:\[\s>+~])/.test(selector);
}

function cssDeclarationMap(ruleBody) {
    return new Map(ruleBody
        .split(";")
        .map((declaration) => declaration.trim())
        .filter(Boolean)
        .map((declaration) => {
            const separatorIndex = declaration.indexOf(":");
            return [declaration.slice(0, separatorIndex).trim(), declaration.slice(separatorIndex + 1).trim()];
        }));
}

function createServiceWorkerRuntime(source) {
    const listeners = new Map();
    const stores = new Map();
    const self = {
        location: { origin: "https://example.com" },
        skipWaiting() {
            return Promise.resolve();
        },
        clients: {
            claim() {
                return Promise.resolve();
            }
        },
        addEventListener(type, listener) {
            listeners.set(type, listener);
        }
    };
    const caches = {
        async open(name) {
            if (!stores.has(name)) {
                stores.set(name, new Map());
            }
            const store = stores.get(name);
            return {
                async put(key, response) {
                    store.set(normalizeCacheKey(key), response.clone());
                },
                async match(key) {
                    const response = store.get(normalizeCacheKey(key));
                    return response ? response.clone() : undefined;
                }
            };
        },
        async keys() {
            return [...stores.keys()];
        },
        async delete(name) {
            return stores.delete(name);
        },
        async match(key) {
            const normalized = normalizeCacheKey(key);
            for (const store of stores.values()) {
                const response = store.get(normalized);
                if (response) {
                    return response.clone();
                }
            }
            return undefined;
        }
    };
    const context = vm.createContext({
        self,
        caches,
        URL,
        Request,
        Response,
        Headers,
        console,
        fetch: async () => new Response("<html><body>Sin conexión</body></html>", { status: 200, headers: { "content-type": "text/html" } })
    });
    new vm.Script(source).runInContext(context);

    async function dispatchLifecycleEvent(type) {
        const listener = listeners.get(type);
        const waits = [];
        listener?.({
            waitUntil(promise) {
                waits.push(Promise.resolve(promise));
            }
        });
        await Promise.all(waits);
    }

    return {
        dispatchInstall() {
            return dispatchLifecycleEvent("install");
        },
        dispatchActivate() {
            return dispatchLifecycleEvent("activate");
        },
        async dispatchFetch(input, { method = "GET", mode = "same-origin", accept = "", network } = {}) {
            const listener = listeners.get("fetch");
            context.fetch = async (request) => network?.(normalizeRequest(request));
            const request = new Request(input, { method, headers: accept ? { accept } : undefined });
            Object.defineProperty(request, "mode", { configurable: true, value: mode });
            let responsePromise = null;
            listener?.({
                request,
                respondWith(promise) {
                    responsePromise = Promise.resolve(promise);
                }
            });
            return responsePromise ? responsePromise : context.fetch(request);
        },
        wasCached(cacheKey) {
            const normalized = normalizeCacheKey(cacheKey);
            for (const store of stores.values()) {
                if (store.has(normalized)) {
                    return true;
                }
            }
            return false;
        }
    };
}

function normalizeCacheKey(key) {
    const url = typeof key === "string" ? new URL(key, "https://example.com") : new URL(key.url);
    return `${url.pathname}${url.search}` || "/";
}

function normalizeRequest(request) {
    return request instanceof Request ? request : new Request(request);
}

function fakeForm(elements = new Map()) {
    const listeners = new Map();
    const submitButton = fakeButton("Crear ingreso");
    return {
        listeners,
        resetCount: 0,
        addEventListener(type, listener) {
            listeners.set(type, listener);
        },
        querySelector(selector) {
            assert.equal(selector, "button[type='submit']");
            return submitButton;
        },
        reset() {
            this.resetCount += 1;
            for (const selector of ["#income-description", "#income-amount", "#income-start-month", "#income-notes"]) {
                if (elements.has(selector)) {
                    elements.get(selector).value = "";
                }
            }
            if (elements.has("#income-type")) {
                elements.get("#income-type").value = "SALARY";
            }
            if (elements.has("#income-recurring-monthly")) {
                elements.get("#income-recurring-monthly").value = "true";
            }
        },
        async submit() {
            await listeners.get("submit")?.({
                preventDefault() {}
            });
        }
    };
}

function fakeIncomeForm(elements) {
    return fakeForm(elements);
}

function fakeClickable() {
    const listeners = new Map();
    return {
        addEventListener(type, listener) {
            listeners.set(type, listener);
        },
        async click() {
            await listeners.get("click")?.({ currentTarget: this });
        }
    };
}

function fakeClickableButton(textContent) {
    const button = fakeButton(textContent);
    const listeners = new Map();
    button.addEventListener = (type, listener) => {
        listeners.set(type, listener);
    };
    button.click = async () => {
        await listeners.get("click")?.({ currentTarget: button });
    };
    return button;
}

function fakeIncomeTable() {
    const element = fakeElement();
    const listeners = new Map();
    Object.defineProperty(element, "innerHTML", {
        get() {
            return this._innerHTML || "";
        },
        set(value) {
            this._innerHTML = value;
            if (value === "") {
                this.children = [];
            }
        }
    });
    element.addEventListener = (type, listener) => {
        listeners.set(type, listener);
    };
    element.clickTarget = async (target) => {
        await listeners.get("click")?.({ target });
    };
    element.changeTarget = async (target) => {
        await listeners.get("change")?.({ target });
    };
    return element;
}

function fakeSuperItemCheckedCheckbox(id, checked) {
    return {
        checked,
        dataset: { superItemId: id },
        closest(selector) {
            return selector === "input[data-super-action='checked']" ? this : null;
        }
    };
}

function incomeFixture(overrides = {}) {
    return {
        id: 1,
        description: "Sueldo principal",
        incomeType: "SALARY",
        amountPesos: "2500.00",
        startMonth: "2026-07",
        endMonth: "",
        recurringMonthly: true,
        projected: false,
        notes: "",
        ...overrides
    };
}

function fakeIncomeActionRow(id, values) {
    const controls = new Map(Object.entries(values).map(([name, value]) => [name, fakeInput(value)]));
    return {
        dataset: { incomeId: id },
        querySelector(selector) {
            const name = selector.match(/\[name='([^']+)'\]/)?.[1];
            assert.ok(name, `Unexpected row selector: ${selector}`);
            assert.ok(controls.has(name), `Missing row control: ${name}`);
            return controls.get(name);
        }
    };
}

function fakeIncomeActionButton(action, row) {
    const button = fakeButton(action);
    button.dataset.incomeAction = action;
    button.closest = (selector) => {
        if (selector === "button[data-income-action]") {
            return button;
        }
        if (selector === "tr[data-income-id]") {
            return row;
        }
        return null;
    };
    return button;
}
