import { addAmounts, cardLabel, emptyTotals, escapeHtml, formatDate, formatMoneyPair, formatMonth, formatPesos, formatUsd, toYearMonth } from "./utils.js";

const targetCards = [
    {
        title: "Santander VISA",
        cardBrand: "VISA",
        alias: "santander",
        providers: ["VISA_HOME", "BANK_PORTAL", "SANTANDER", "MANUAL"]
    },
    {
        title: "Santander AMEX",
        cardBrand: "AMERICAN_EXPRESS",
        alias: "santander",
        providers: ["BANK_PORTAL", "SANTANDER", "MANUAL"]
    },
    {
        title: "Naranja X",
        cardBrand: "OTHER",
        alias: "naranja",
        providers: ["BANK_PORTAL", "NARANJA_X", "MANUAL"]
    }
];

const MAX_VISIBLE_MONTH_TABS = 8;

export function renderDashboard({ month, summary, statements, transactions, allStatements, months = [], monthDetail = null }) {
    renderMonthTabs(month, months, allStatements);
    renderSummary(summary, transactions, monthDetail);
    renderCardDetails(statements, month);
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
        tabs.innerHTML = '<span class="empty-state">No statement months are loaded yet. Upload and confirm a draft to create month tabs.</span>';
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
        return ' <span class="tab-label">Projection</span>';
    }
    if (month.currentReal) {
        return ' <span class="tab-label">Actual</span>';
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
        ? "Projection-only month from remaining installments."
        : monthDetail?.currentReal
            ? "Actual confirmed statement data. Projections for this month are suppressed to avoid double-counting."
            : detailRows.some((row) => row.kind === "PROJECTION")
                ? "Projection detail from remaining installments."
                : transactions.length > 0
                    ? "Loaded from current month transactions."
                    : "No loaded transactions for this month.";
    document.querySelector("#one-payment-total").textContent = formatMoneyPair(onePaymentTotals);
    document.querySelector("#installment-total").textContent = formatMoneyPair(installmentTotals);
    document.querySelector("#charges-total").textContent = formatMoneyPair(chargesTotals);
    document.querySelector("#record-counts").textContent = `${summary?.statementCount || 0} / ${detailRows.length || summary?.transactionCount || 0}`;
}

function renderCardDetails(statements, selectedMonth) {
    const container = document.querySelector("#card-detail-grid");
    container.innerHTML = "";

    targetCards.forEach((target) => {
        const matches = statements.filter((statement) => matchesTargetCard(statement, target));
        const isLoaded = matches.length > 0;
        const totals = matches.reduce((current, statement) => addAmounts(current, statement), emptyTotals());
        const primary = matches[0];

        const missingMessage = `Missing ${target.title} for ${formatMonth(selectedMonth)}.`;
        const article = document.createElement("article");
        article.className = "card-detail";
        article.innerHTML = `
            <header>
                <div>
                    <h3>${escapeHtml(target.title)}</h3>
                    <p class="muted">${escapeHtml(isLoaded ? (primary.cardAlias || cardLabel(primary.cardBrand)) : missingMessage)}</p>
                </div>
                <span class="status-chip ${isLoaded ? "loaded" : "empty"}">${isLoaded ? "Loaded" : "Missing"}</span>
            </header>
            <dl>
                <div><dt>Total</dt><dd>${formatMoneyPair(totals)}</dd></div>
                <div><dt>Status</dt><dd>${escapeHtml(primary?.status || "No confirmed statement")}</dd></div>
                <div><dt>Due date</dt><dd>${formatDate(primary?.dueDate)}</dd></div>
                <div><dt>Transactions</dt><dd>${primary?.transactionCount || 0}</dd></div>
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
    label.textContent = monthDetail?.projectionOnly ? "Projection-only month" : monthDetail?.currentReal ? "Actual month detail" : "No confirmed month data";
    label.classList.toggle("projection", Boolean(monthDetail?.projectionOnly));

    rows.forEach((row) => {
        const tr = document.createElement("tr");
        tr.className = row.kind === "PROJECTION" ? "projection-row" : "actual-row";
        tr.innerHTML = `
            <td><span class="status-chip ${row.kind === "PROJECTION" ? "projection" : "loaded"}">${escapeHtml(row.kind === "PROJECTION" ? "Projection" : "Actual")}</span></td>
            <td>${escapeHtml(row.description || "—")}</td>
            <td>${escapeHtml(row.cardAlias || cardLabel(row.cardBrand))}</td>
            <td>${installmentText(row)}</td>
            <td class="amount">${formatPesos(row.amountPesos)}</td>
            <td class="amount">${formatUsd(row.amountUsd)}</td>
            <td>${escapeHtml(row.categoryName || "Uncategorized")}</td>
            <td>${formatMonth(row.estimatedFinishMonth)}</td>
            <td>${sourceText(row)}</td>
        `;
        table.append(tr);
    });

    empty.hidden = rows.length > 0;
    empty.textContent = monthDetail?.projectionOnly
        ? "This future month is marked as projection-only, but no remaining installment rows are available."
        : monthDetail?.currentReal
            ? "This confirmed month has no installment detail rows to show."
            : `No confirmed statement data is available for ${formatMonth(selectedMonth)}. Upload and confirm a draft, or choose a month with projections.`;
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
    return `Statement #${row.sourceStatementId} · ${formatMonth(row.sourceStatementMonth)}`;
}

function aliasIncludes(statement, text) {
    return String(statement.cardAlias || "").toLowerCase().includes(text);
}

function matchesTargetCard(statement, target) {
    return statement.cardBrand === target.cardBrand
        && target.providers.includes(statement.provider)
        && aliasIncludes(statement, target.alias);
}
