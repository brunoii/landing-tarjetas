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
    renderMonthDetail(monthDetail, month);
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
    const onePaymentTotals = totalByType(transactions, ["PURCHASE"]);
    const installmentTotals = totalByType(transactions, ["INSTALLMENT"]);
    const chargesTotals = totalByType(transactions, ["FEE", "TAX"]);
    const detailRows = monthDetail?.rows || [];
    const detailTotals = {
        pesos: monthDetail?.totalPesos ?? summary?.totalPesos,
        usd: monthDetail?.totalUsd ?? summary?.totalUsd
    };

    document.querySelector("#total-pesos").textContent = formatPesos(detailTotals.pesos);
    document.querySelector("#total-usd").textContent = formatUsd(detailTotals.usd);
    document.querySelector("#total-pesos-hint").textContent = monthDetail?.projectionOnly
        ? "Mes solo con proyección de cuotas pendientes."
        : monthDetail?.currentReal
            ? "Datos reales de resúmenes confirmados. Las proyecciones de este mes se ocultan para evitar doble conteo."
            : detailRows.some((row) => row.kind === "PROJECTION")
                ? "Detalle de proyección de cuotas pendientes."
                : transactions.length > 0
                    ? "Cargado desde las transacciones del mes actual."
                    : "No hay transacciones cargadas para este mes.";
    document.querySelector("#one-payment-total").textContent = formatMoneyPair(onePaymentTotals);
    document.querySelector("#installment-total").textContent = formatMoneyPair(installmentTotals);
    document.querySelector("#charges-total").textContent = formatMoneyPair(chargesTotals);
    document.querySelector("#record-counts").textContent = `${summary?.statementCount || 0} / ${detailRows.length || summary?.transactionCount || 0}`;
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

function renderMonthDetail(monthDetail, selectedMonth) {
    const table = document.querySelector("#month-detail-table");
    const empty = document.querySelector("#month-detail-empty");
    const label = document.querySelector("#month-detail-label");
    const rows = monthDetail?.rows || [];
    table.innerHTML = "";
    label.textContent = monthDetail?.projectionOnly ? "Mes solo proyectado" : monthDetail?.currentReal ? "Detalle del mes confirmado" : "Sin datos confirmados del mes";
    label.classList.toggle("projection", Boolean(monthDetail?.projectionOnly));

    rows.forEach((row) => {
        const tr = document.createElement("tr");
        tr.className = row.kind === "PROJECTION" ? "projection-row" : "actual-row";
        tr.innerHTML = `
            <td><span class="status-chip ${row.kind === "PROJECTION" ? "projection" : "loaded"}">${escapeHtml(row.kind === "PROJECTION" ? "Proyección" : "Real")}</span></td>
            <td>${escapeHtml(row.description || "—")}</td>
            <td>${escapeHtml(row.cardAlias || cardLabel(row.cardBrand))}</td>
            <td>${installmentText(row)}</td>
            <td class="amount">${formatPesos(row.amountPesos)}</td>
            <td class="amount">${formatUsd(row.amountUsd)}</td>
            <td>${escapeHtml(row.categoryName || "Sin categoría")}</td>
            <td>${formatMonth(row.estimatedFinishMonth)}</td>
            <td>${sourceText(row)}</td>
        `;
        table.append(tr);
    });

    empty.hidden = rows.length > 0;
    empty.textContent = monthDetail?.projectionOnly
        ? "Este mes futuro está marcado como solo proyectado, pero no hay cuotas pendientes disponibles."
        : monthDetail?.currentReal
            ? "Este mes confirmado no tiene filas de detalle de cuotas para mostrar."
            : `No hay datos de resúmenes confirmados para ${formatMonth(selectedMonth)}. Cargue y confirme un borrador, o elija un mes con proyecciones.`;
}

function installmentText(row) {
    if (!row.installmentNumber && !row.totalInstallments) {
        return "—";
    }
    return `${row.installmentNumber || "?"}/${row.totalInstallments || "?"}`;
}

function sourceText(row) {
    if (!row.sourceStatementId) {
        return "—";
    }
    return `Resumen #${row.sourceStatementId} · ${formatMonth(row.sourceStatementMonth)}`;
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
