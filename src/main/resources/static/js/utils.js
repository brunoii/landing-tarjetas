export function currentYearMonth() {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

export function toYearMonth(value) {
    if (!value) {
        return "";
    }
    return String(value).slice(0, 7);
}

export function formatMonth(value) {
    const yearMonth = toYearMonth(value);
    if (!yearMonth) {
        return "No month";
    }
    const [year, month] = yearMonth.split("-");
    const date = new Date(Number(year), Number(month) - 1, 1);
    return new Intl.DateTimeFormat("en", { month: "short", year: "numeric" }).format(date);
}

export function formatDate(value) {
    if (!value) {
        return "—";
    }
    const date = new Date(`${value}T00:00:00`);
    return new Intl.DateTimeFormat("en", { day: "2-digit", month: "short", year: "numeric" }).format(date);
}

export function formatPesos(value) {
    return formatCurrency(value, "ARS");
}

export function formatUsd(value) {
    return formatCurrency(value, "USD");
}

export function formatMoneyPair(totals) {
    return `${formatPesos(totals.pesos)} / ${formatUsd(totals.usd)}`;
}

export function emptyTotals() {
    return { pesos: 0, usd: 0 };
}

export function addAmounts(target, item) {
    target.pesos += Number(item.amountPesos || item.totalPesos || 0);
    target.usd += Number(item.amountUsd || item.totalUsd || 0);
    return target;
}

export function cardLabel(cardBrand) {
    const labels = {
        VISA: "Visa",
        AMERICAN_EXPRESS: "American Express",
        MASTERCARD: "Mastercard",
        OTHER: "Other"
    };
    return labels[cardBrand] || cardBrand || "Unknown card";
}

export function typeLabel(type) {
    return String(type || "Unknown").toLowerCase().replaceAll("_", " ").replace(/^./, (letter) => letter.toUpperCase());
}

export function escapeHtml(value) {
    return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

export function isSafeHexColor(value) {
    return /^#[0-9A-Fa-f]{6}$/.test(String(value || "").trim());
}

export function safeHexColor(value, fallback = "#38bdf8") {
    const color = String(value || "").trim();
    return isSafeHexColor(color) ? color : fallback;
}

function formatCurrency(value, currency) {
    const amount = Number(value || 0);
    return new Intl.NumberFormat("en", {
        style: "currency",
        currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(amount);
}
