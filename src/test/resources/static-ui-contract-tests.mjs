import assert from "node:assert/strict";
import { copyFile, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { pathToFileURL } from "node:url";

const sourceRoot = path.resolve("src/main/resources/static/js");
const moduleRoot = path.join(tmpdir(), `landing-tarjetas-static-ui-${process.pid}`);

await rm(moduleRoot, { force: true, recursive: true });
await mkdir(moduleRoot, { recursive: true });
await writeFile(path.join(moduleRoot, "package.json"), JSON.stringify({ type: "module" }));

for (const fileName of ["api.js", "app.js", "categories.js", "dashboard.js", "incomes.js", "manual-expenses.js", "navigation.js", "simulator.js", "statements.js", "transactions.js", "utils.js"]) {
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
    const { DEFAULT_PRIMARY_TAB_ID, primaryTabs, primaryTabViewState, setupPrimaryTabs } = await import(pathToFileURL(path.join(moduleRoot, "navigation.js")));
    const {
        draftTransactionCountLabel,
        missingTransactionControlsState,
        missingTransactionSubmitIntent,
        parserDisplayLabel
    } = await import(pathToFileURL(path.join(moduleRoot, "statements.js")));

    const indexHtml = await readFile(path.resolve("src/main/resources/static/index.html"), "utf8");
    const primaryTabButtons = extractPrimaryTabButtons(indexHtml);
    assert.deepEqual(primaryTabButtons.map(({ id, label }) => ({ id, label })), primaryTabs);
    assert.deepEqual(primaryTabButtons.map(({ buttonId, controls, selected }) => ({ buttonId, controls, selected })), primaryTabs.map((tab) => ({
        buttonId: `primary-tab-${tab.id}`,
        controls: `tab-${tab.id}`,
        selected: tab.id === DEFAULT_PRIMARY_TAB_ID ? "true" : "false"
    })));

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

    assert.equal(incomeTypeLabel("SALARY"), "Sueldo");
    assert.equal(incomeTypeLabel("VARIABLE"), "Variable");
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
        ["/api/dashboard/summary?month=2026-09", "GET"]
    ]);
    assert.deepEqual(JSON.parse(apiCalls[2].options.body), { description: "Sueldo", amountPesos: "100", startMonth: "2026-07" });
    assert.deepEqual(JSON.parse(apiCalls[7].options.body), { description: "Préstamo", amountPesos: "100", startMonth: "2026-07" });

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
    globalThis.document = incomeDom.document;
    globalThis.confirm = () => true;
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

        const editRow = fakeIncomeActionRow("21", {
            description: "Sueldo editado",
            incomeType: "SALARY",
            amountPesos: "3000",
            startMonth: "2026-07",
            endMonth: "",
            recurringMonthly: "true",
            notes: "Actualizado"
        });
        await incomeDom.elements.get("#incomes-table").clickTarget(fakeIncomeActionButton("save", editRow));
        assert.deepEqual(incomeDom.api.calls.at(-2), {
            method: "updateIncome",
            id: "21",
            payload: {
                description: "Sueldo editado",
                incomeType: "SALARY",
                amountPesos: "3000",
                startMonth: "2026-07",
                endMonth: "",
                recurringMonthly: true,
                notes: "Actualizado"
            }
        });
        assert.equal(dashboardRefreshCount, 2);

        await incomeDom.elements.get("#incomes-table").clickTarget(fakeIncomeActionButton("delete", editRow));
        assert.deepEqual(incomeDom.api.calls.at(-2), { method: "deleteIncome", id: "21" });
        assert.equal(dashboardRefreshCount, 3);

        const futureRow = fakeIncomeActionRow("22", {
            description: "Sueldo futuro",
            incomeType: "SALARY",
            amountPesos: "3200",
            startMonth: "2026-07",
            endMonth: "",
            recurringMonthly: "true",
            notes: "Desde septiembre",
            effectiveMonth: "2026-09"
        });
        await incomeDom.elements.get("#incomes-table").clickTarget(fakeIncomeActionButton("save-from-month", futureRow));
        assert.deepEqual(incomeDom.api.calls.at(-2), {
            method: "updateIncomeFromMonth",
            id: "22",
            yearMonth: "2026-09",
            payload: {
                description: "Sueldo futuro",
                incomeType: "SALARY",
                amountPesos: "3200",
                startMonth: "2026-09",
                endMonth: "",
                recurringMonthly: true,
                notes: "Desde septiembre"
            }
        });
        assert.equal(dashboardRefreshCount, 4);
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
    assert.deepEqual(primaryTabs.map((tab) => tab.label), [
        "Resumen",
        "Cargar Gastos",
        "Tabla Gastos",
        "Tabla Ingresos",
        "Cargar Ingresos",
        "Simulador",
        "Categorías"
    ]);
    assert.deepEqual(primaryTabViewState().map(({ id, selected, panelHidden }) => ({ id, selected, panelHidden })), [
        { id: "summary", selected: true, panelHidden: false },
        { id: "expenses-upload", selected: false, panelHidden: true },
        { id: "expenses-table", selected: false, panelHidden: true },
        { id: "income-table", selected: false, panelHidden: true },
        { id: "income-upload", selected: false, panelHidden: true },
        { id: "simulator", selected: false, panelHidden: true },
        { id: "categories", selected: false, panelHidden: true }
    ]);
    assert.equal(primaryTabViewState("expenses-table").find((tab) => tab.id === "expenses-table").selected, true);

    const primaryTabDom = fakePrimaryTabDom(primaryTabs.slice(0, 3).map((tab) => tab.id));
    const previousTabDocument = globalThis.document;
    globalThis.document = primaryTabDom.document;
    try {
        setupPrimaryTabs();
        assertPrimaryTabState(primaryTabDom, "summary");

        primaryTabDom.buttonsById.get("expenses-table").click();
        assertPrimaryTabState(primaryTabDom, "expenses-table");
    } finally {
        if (previousTabDocument === undefined) {
            delete globalThis.document;
        } else {
            globalThis.document = previousTabDocument;
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
    assert.equal(elements.get("#record-counts").textContent, "2 / 55 / 2");
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

function fakePrimaryTabDom(tabIds) {
    const buttons = tabIds.map((tabId) => fakeTabButton(tabId));
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
                throw new Error(`Unexpected tab selector: ${selector}`);
            }
        },
        sectionsById: new Map(sections.map((section) => [section.dataset.tabPanel, section]))
    };
}

function fakeTabButton(tabId) {
    const classes = new Set();
    const attributes = new Map();
    const listeners = new Map();
    return {
        attributes,
        dataset: { tabTarget: tabId },
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
        focus() {
            this.focused = true;
        },
        setAttribute(name, value) {
            attributes.set(name, value);
        }
    };
}

function fakeTabSection(tabId) {
    return {
        dataset: { tabPanel: tabId },
        hidden: false
    };
}

function assertPrimaryTabState(primaryTabDom, activeTabId) {
    for (const [tabId, button] of primaryTabDom.buttonsById) {
        const selected = tabId === activeTabId;
        assert.equal(button.attributes.get("aria-selected"), String(selected), `${tabId} aria-selected`);
        assert.equal(button.classList.contains("active"), selected, `${tabId} active class`);
        assert.equal(button.tabIndex, selected ? 0 : -1, `${tabId} tabindex`);
        assert.equal(primaryTabDom.sectionsById.get(tabId).hidden, !selected, `${tabId} panel hidden`);
    }
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
        "#income-notes"
    ]) {
        elements.set(selector, fakeInput());
    }
    elements.get("#income-type").value = "SALARY";
    elements.get("#income-recurring-monthly").value = "true";
    elements.set("#income-form", incomeForm);
    elements.set("#income-filter-form", fakeForm());
    elements.set("#clear-income-filter", fakeClickable());
    elements.set("#incomes-table", table);
    elements.set("#incomes-empty", fakeElement());
    elements.set("#income-filters-summary", fakeElement());
    elements.set("#income-feedback", fakeElement());

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
    return { value };
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
            await listeners.get("click")?.({});
        }
    };
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
    return element;
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
