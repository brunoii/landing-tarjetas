import assert from "node:assert/strict";
import { copyFile, mkdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { pathToFileURL } from "node:url";

const sourceRoot = path.resolve("src/main/resources/static/js");
const moduleRoot = path.join(tmpdir(), `landing-tarjetas-static-ui-${process.pid}`);

await rm(moduleRoot, { force: true, recursive: true });
await mkdir(moduleRoot, { recursive: true });
await writeFile(path.join(moduleRoot, "package.json"), JSON.stringify({ type: "module" }));

for (const fileName of ["api.js", "app.js", "categories.js", "dashboard.js", "statements.js", "transactions.js", "utils.js"]) {
    await copyFile(path.join(sourceRoot, fileName), path.join(moduleRoot, fileName));
}

try {
    const { formatDate, formatMonth, setButtonBusy } = await import(pathToFileURL(path.join(moduleRoot, "utils.js")));
    const { matchesTargetCard, monthTabLabel, renderDashboard, visibleMonthTabs } = await import(pathToFileURL(path.join(moduleRoot, "dashboard.js")));
    const {
        draftTransactionCountLabel,
        missingTransactionControlsState,
        missingTransactionSubmitIntent,
        parserDisplayLabel
    } = await import(pathToFileURL(path.join(moduleRoot, "statements.js")));

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
            summary: { totalPesos: 2012.38, totalUsd: 0, statementCount: 2, transactionCount: 55 },
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
    const santanderVisaCard = cardGrid.children[0].innerHTML;
    assert.match(santanderVisaCard, /<h3>Santander VISA<\/h3>/);
    assert.match(santanderVisaCard, /<dt>Total<\/dt><dd>[\s\S]*2,012,382\.98[\s\S]*25\.50[\s\S]*<\/dd>/);
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
} finally {
    await rm(moduleRoot, { force: true, recursive: true });
}

function fakeButton(textContent) {
    const attributes = new Map();
    return {
        attributes,
        dataset: {},
        disabled: false,
        textContent,
        removeAttribute(name) {
            attributes.delete(name);
        },
        setAttribute(name, value) {
            attributes.set(name, value);
        }
    };
}

function fakeDashboardElements() {
    return new Map([
        "#month-tabs",
        "#month-input",
        "#total-pesos",
        "#total-usd",
        "#total-pesos-hint",
        "#one-payment-total",
        "#installment-total",
        "#charges-total",
        "#record-counts",
        "#card-detail-grid",
        "#month-detail-table",
        "#month-detail-empty",
        "#month-detail-label"
    ].map((selector) => [selector, fakeElement()]));
}

function fakeElement() {
    return {
        attributes: new Map(),
        children: [],
        className: "",
        dataset: {},
        hidden: false,
        innerHTML: "",
        textContent: "",
        classList: {
            toggle() {}
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
