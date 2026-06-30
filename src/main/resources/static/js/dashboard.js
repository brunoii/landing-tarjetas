import { addAmounts, cardLabel, emptyTotals, escapeHtml, formatDate, formatMoneyPair, formatMonth, formatPesos, formatUsd, toYearMonth } from "./utils.js";

const targetCards = [
    {
        title: "Santander VISA",
        cardBrand: "VISA",
        alias: "santander",
        providers: ["VISA_HOME", "BANK_PORTAL", "MANUAL"]
    },
    {
        title: "Santander AMEX",
        cardBrand: "AMERICAN_EXPRESS",
        alias: "santander",
        providers: ["BANK_PORTAL", "MANUAL"]
    },
    {
        title: "Naranja X",
        cardBrand: "OTHER",
        alias: "naranja",
        providers: ["BANK_PORTAL", "MANUAL"]
    }
];

const MAX_VISIBLE_MONTH_TABS = 8;

export function renderDashboard({ month, summary, statements, transactions, allStatements }) {
    renderMonthTabs(month, allStatements);
    renderSummary(summary, transactions);
    renderCardDetails(statements);
}

function renderMonthTabs(selectedMonth, allStatements) {
    const tabs = document.querySelector("#month-tabs");
    const months = [...new Set(allStatements.map((statement) => toYearMonth(statement.paymentMonth)).filter(Boolean))]
        .sort()
        .reverse();

    if (!months.includes(selectedMonth)) {
        months.unshift(selectedMonth);
    }

    tabs.innerHTML = "";
    months.slice(0, MAX_VISIBLE_MONTH_TABS).forEach((month) => {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = formatMonth(month);
        button.dataset.month = month;
        button.className = month === selectedMonth ? "active" : "";
        button.addEventListener("click", () => {
            document.querySelector("#month-input").value = month;
            document.querySelector("#month-input").dispatchEvent(new Event("change"));
        });
        tabs.append(button);
    });

    const placeholder = document.createElement("button");
    placeholder.type = "button";
    placeholder.className = "placeholder-tab";
    placeholder.disabled = true;
    placeholder.textContent = "Future projections unavailable in Etapa 3";
    tabs.append(placeholder);
}

function renderSummary(summary, transactions) {
    const onePaymentTotals = totalByType(transactions, ["PURCHASE"]);
    const installmentTotals = totalByType(transactions, ["INSTALLMENT"]);
    const chargesTotals = totalByType(transactions, ["FEE", "TAX"]);

    document.querySelector("#total-pesos").textContent = formatPesos(summary?.totalPesos);
    document.querySelector("#total-usd").textContent = formatUsd(summary?.totalUsd);
    document.querySelector("#total-pesos-hint").textContent = transactions.length > 0
        ? "Loaded from current month transactions."
        : "No loaded transactions for this month.";
    document.querySelector("#one-payment-total").textContent = formatMoneyPair(onePaymentTotals);
    document.querySelector("#installment-total").textContent = formatMoneyPair(installmentTotals);
    document.querySelector("#charges-total").textContent = formatMoneyPair(chargesTotals);
    document.querySelector("#record-counts").textContent = `${summary?.statementCount || 0} / ${summary?.transactionCount || 0}`;
}

function renderCardDetails(statements) {
    const container = document.querySelector("#card-detail-grid");
    container.innerHTML = "";

    targetCards.forEach((target) => {
        const matches = statements.filter((statement) => matchesTargetCard(statement, target));
        const isLoaded = matches.length > 0;
        const totals = matches.reduce((current, statement) => addAmounts(current, statement), emptyTotals());
        const primary = matches[0];

        const article = document.createElement("article");
        article.className = "card-detail";
        article.innerHTML = `
            <header>
                <div>
                    <h3>${escapeHtml(target.title)}</h3>
                    <p class="muted">${escapeHtml(isLoaded ? (primary.cardAlias || cardLabel(primary.cardBrand)) : "No local statement loaded")}</p>
                </div>
                <span class="status-chip ${isLoaded ? "loaded" : ""}">${isLoaded ? "Loaded" : "Pending"}</span>
            </header>
            <dl>
                <div><dt>Total</dt><dd>${formatMoneyPair(totals)}</dd></div>
                <div><dt>Status</dt><dd>${escapeHtml(primary?.status || "Pending")}</dd></div>
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

function aliasIncludes(statement, text) {
    return String(statement.cardAlias || "").toLowerCase().includes(text);
}

function matchesTargetCard(statement, target) {
    return statement.cardBrand === target.cardBrand
        && target.providers.includes(statement.provider)
        && aliasIncludes(statement, target.alias);
}
