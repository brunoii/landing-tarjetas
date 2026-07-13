import { addAmounts, cardLabel, emptyTotals, escapeHtml, formatDate, formatMoneyPair, formatMonth, formatPesos, formatUsd, toYearMonth } from "./utils.js";

const targetCards = [
    {
        title: "Santander VISA",
        cardBrand: "VISA",
        alias: "santander",
        strongProviders: ["SANTANDER"],
        providers: ["VISA_HOME", "BANK_PORTAL", "SANTANDER", "MANUAL"]
    },
    {
        title: "Santander AMEX",
        cardBrand: "AMERICAN_EXPRESS",
        alias: "santander",
        strongProviders: ["SANTANDER"],
        providers: ["BANK_PORTAL", "SANTANDER", "MANUAL"]
    },
    {
        title: "Naranja X",
        alias: "naranja",
        strongProviders: ["NARANJA_X"],
        providers: ["BANK_PORTAL", "NARANJA_X", "MANUAL"]
    }
];

const MAX_VISIBLE_MONTH_TABS = 8;

export function renderDashboard({ month, summary, statements, transactions, allStatements, months = [], monthDetail = null }) {
    renderMonthTabs(month, months, allStatements);
    renderSummary(summary, transactions, monthDetail);
    renderCardDetails(statements, month, monthDetail);
}

function renderMonthTabs(selectedMonth, months, allStatements) {
    const tabs = document.querySelector("#month-tabs");
    const apiMonths = months.map((month) => ({ ...month, yearMonth: month.yearMonth || toYearMonth(month.month) })).filter((month) => month.yearMonth);
    const fallbackMonths = [...new Set(allStatements.map((statement) => toYearMonth(statement.paymentMonth)).filter(Boolean))]
        .sort()
        .reverse()
        .map((yearMonth) => ({ yearMonth, currentReal: true, projectionOnly: false }));
    const visibleMonths = visibleMonthTabs(selectedMonth, apiMonths.length ? apiMonths : fallbackMonths);

    tabs.innerHTML = "";
    if (visibleMonths.length === 0) {
        tabs.innerHTML = '<span class="empty-state">Todavía no hay meses con resúmenes cargados. Cargue y confirme un borrador para crear pestañas de meses.</span>';
        return;
    }
    visibleMonths.forEach((month) => {
        const button = document.createElement("button");
        button.type = "button";
        button.innerHTML = `${escapeHtml(formatMonth(month.yearMonth))}${monthTabLabel(month)}`;
        button.dataset.month = month.yearMonth;
        button.className = month.yearMonth === selectedMonth ? "active" : "";
        button.classList.toggle("projection-tab", Boolean(month.projectionOnly));
        button.setAttribute("aria-pressed", String(month.yearMonth === selectedMonth));
        button.addEventListener("click", () => {
            document.querySelector("#month-input").value = month.yearMonth;
            document.querySelector("#month-input").dispatchEvent(new Event("change"));
        });
        tabs.append(button);
    });
}

export function visibleMonthTabs(selectedMonth, months, maxVisible = MAX_VISIBLE_MONTH_TABS) {
    const monthList = [...months];
    if (!monthList.some((month) => month.yearMonth === selectedMonth)) {
        monthList.unshift({ yearMonth: selectedMonth, currentReal: false, projectionOnly: false });
    }
    const visibleMonths = monthList.slice(0, maxVisible);
    if (selectedMonth && !visibleMonths.some((month) => month.yearMonth === selectedMonth)) {
        const selected = monthList.find((month) => month.yearMonth === selectedMonth);
        if (selected) {
            visibleMonths[visibleMonths.length - 1] = selected;
        }
    }
    return visibleMonths;
}

export function monthTabLabel(month) {
    if (month.projectionOnly) {
        return ' <span class="tab-label">Proyección</span>';
    }
    if (month.currentReal) {
        return ' <span class="tab-label">Confirmado</span>';
    }
    return "";
}

function renderSummary(summary, transactions, monthDetail) {
    const detailRows = monthDetail?.rows || [];
    const rowsForTypeTotals = detailRows.length ? detailRows : transactions;
    const onePaymentTotals = totalByType(rowsForTypeTotals, ["PURCHASE", "ONE_PAYMENT", "CASH"]);
    const installmentTotals = totalByType(rowsForTypeTotals, ["INSTALLMENT", "LOAN"]);
    const chargesTotals = totalByType(rowsForTypeTotals, ["FEE", "TAX"]);
    const detailTotals = {
        pesos: monthDetail?.totalPesos ?? summary?.totalPesos,
        usd: monthDetail?.totalUsd ?? summary?.totalUsd
    };
    const incomeTotalPesos = Number(summary?.incomeTotalPesos || 0);
    const salaryIncomeTotalPesos = Number(summary?.salaryIncomeTotalPesos || 0);
    const variableIncomeTotalPesos = Number(summary?.variableIncomeTotalPesos || 0);
    const projectedIncomeTotalPesos = Number(summary?.projectedIncomeTotalPesos || 0);
    const balancePesos = Number(summary?.monthlyBalancePesos ?? (incomeTotalPesos - Number(detailTotals.pesos || 0)));
    const estimatedLabel = document.querySelector("#summary-estimated-label");
    const projectedIncomeCard = document.querySelector("#projected-income-card");

    document.querySelector("#monthly-income-total").textContent = formatPesos(incomeTotalPesos);
    document.querySelector("#salary-income-total").textContent = formatPesos(salaryIncomeTotalPesos);
    document.querySelector("#variable-income-total").textContent = formatPesos(variableIncomeTotalPesos);
    document.querySelector("#projected-income-total").textContent = formatPesos(projectedIncomeTotalPesos);
    projectedIncomeCard.hidden = projectedIncomeTotalPesos <= 0;
    estimatedLabel.hidden = !summary?.estimated;
    document.querySelector("#total-pesos").textContent = formatPesos(detailTotals.pesos);
    document.querySelector("#total-usd").textContent = formatUsd(detailTotals.usd);
    document.querySelector("#monthly-balance-pesos").textContent = formatPesos(balancePesos);
    document.querySelector("#monthly-balance-hint").textContent = balancePesos >= 0
        ? "Ingresos del mes menos gastos en pesos."
        : "Los gastos en pesos superan los ingresos del mes.";
    document.querySelector("#total-pesos-hint").textContent = monthDetail?.projectionOnly
        ? "Mes solo con proyección de cuotas pendientes."
        : monthDetail?.currentReal
            ? "Datos reales de resúmenes confirmados. Las proyecciones de este mes se ocultan para evitar doble conteo."
            : detailRows.some((row) => row.kind === "PROJECTION")
                ? "Gastos proyectados visibles en Tabla Gastos."
                : transactions.length > 0
                    ? "Cargado desde las transacciones del mes actual."
                    : "No hay transacciones cargadas para este mes.";
    document.querySelector("#one-payment-total").textContent = formatMoneyPair(onePaymentTotals);
    document.querySelector("#installment-total").textContent = formatMoneyPair(installmentTotals);
    document.querySelector("#charges-total").textContent = formatMoneyPair(chargesTotals);
    renderRecordCounts({
        statements: summary?.statementCount || 0,
        transactions: detailRows.length || summary?.transactionCount || 0,
        incomes: summary?.incomeCount || 0
    });
}

export function renderCardDetails(statements, selectedMonth, monthDetail) {
    const container = document.querySelector("#card-detail-grid");
    container.innerHTML = "";

    targetCards.forEach((target) => {
        const matches = statements.filter((statement) => matchesTargetCard(statement, target));
        const detailTotals = (monthDetail?.totalsByCard || []).filter((total) => matchesTargetCard(total, target));
        const loadedFromDetail = Boolean(monthDetail?.currentReal && detailTotals.length);
        const isLoaded = matches.length > 0 || loadedFromDetail;
        const primary = matches[0] || detailTotals[0];
        const totals = (detailTotals.length ? detailTotals : matches)
            .reduce((current, item) => addAmounts(current, item), emptyTotals());
        const transactionCount = detailTotals.length
            ? detailTotals.reduce((count, total) => count + Number(total.rowCount || 0), 0)
            : primary?.transactionCount || 0;

        const statusText = matches[0]?.status ? statementStatusText(matches[0].status) : loadedFromDetail ? "Confirmado" : "Sin resumen confirmado";
        const missingMessage = `Falta ${target.title} para ${formatMonth(selectedMonth)}.`;
        const article = document.createElement("article");
        article.className = "card-detail";
        article.innerHTML = `
            <header>
                <div>
                    <h3>${escapeHtml(target.title)}</h3>
                    <p class="muted">${escapeHtml(isLoaded ? (primary.cardAlias || cardLabel(primary.cardBrand)) : missingMessage)}</p>
                </div>
                <span class="status-chip ${isLoaded ? "loaded" : "empty"}">${isLoaded ? "Cargado" : "Faltante"}</span>
            </header>
            <dl>
                <div><dt>Total</dt><dd>${formatMoneyPair(totals)}</dd></div>
                <div><dt>Estado</dt><dd>${escapeHtml(statusText)}</dd></div>
                <div><dt>Vencimiento</dt><dd>${formatDate(matches[0]?.dueDate)}</dd></div>
                <div><dt>Transacciones</dt><dd>${transactionCount}</dd></div>
            </dl>
        `;
        container.append(article);
    });
}

function totalByType(transactions, types) {
    return transactions
        .filter((transaction) => types.includes(transaction.type))
        .reduce((current, transaction) => addAmounts(current, transaction), emptyTotals());
}

function renderRecordCounts({ statements, transactions, incomes }) {
    document.querySelector("#record-counts").innerHTML = `
        <span><strong>${Number(statements || 0)}</strong><small>Resúmenes</small></span>
        <span><strong>${Number(transactions || 0)}</strong><small>Transacciones</small></span>
        <span><strong>${Number(incomes || 0)}</strong><small>Ingresos</small></span>
    `;
}

function aliasIncludes(statement, text) {
    return String(statement.cardAlias || "").toLowerCase().includes(text);
}

export function matchesTargetCard(statement, target) {
    const provider = statement.provider;
    const providerMatches = !provider || target.providers.includes(provider);
    const strongProviderMatches = provider && (target.strongProviders || []).includes(provider);
    return (!target.cardBrand || statement.cardBrand === target.cardBrand)
        && providerMatches
        && (aliasIncludes(statement, target.alias) || strongProviderMatches);
}

function statementStatusText(status) {
    const labels = {
        CONFIRMED: "Confirmado",
        DRAFT: "Borrador"
    };
    return labels[status] || status || "Sin resumen confirmado";
}
