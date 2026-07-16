const headers = {
    "Content-Type": "application/json",
    "Accept": "application/json"
};

async function request(path, options = {}) {
    const method = String(options.method || "GET").toUpperCase();
    const response = await fetch(path, {
        ...options,
        credentials: "same-origin",
        headers: {
            ...headers,
            ...csrfHeaders(method),
            ...(options.headers || {})
        }
    });

    if (!response.ok) {
        throw await apiError(response);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

async function apiError(response) {
    const body = await errorBody(response);
    const error = new Error(errorMessage(body, response.status));
    error.status = response.status;
    error.body = body;
    error.details = Array.isArray(body?.details) ? body.details : [];
    for (const key of ["itemId", "itemName", "currentStock", "quantity", "resultingStock", "movementType"]) {
        if (body?.[key] !== undefined) {
            error[key] = body[key];
        }
    }
    return error;
}

async function errorBody(response) {
    try {
        return await response.json();
    } catch {
        return null;
    }
}

function errorMessage(body, status) {
    if (Array.isArray(body?.details) && body.details.length > 0) {
        return safeApiMessage(body.details.join(" "), status);
    }
    return safeApiMessage(body?.error, status);
}

function safeApiMessage(message, status) {
    const text = String(message || "").trim();
    if (!text) {
        return `La solicitud falló con estado ${status}. No se expuso texto del resumen ni contenido del PDF.`;
    }
    return text.length > 260 ? `${text.slice(0, 260)}...` : text;
}

function withQuery(path, params) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== "") {
            query.set(key, value);
        }
    });
    const suffix = query.toString();
    return suffix ? `${path}?${suffix}` : path;
}

export const api = {
    summary(month) {
        return request(withQuery("/api/dashboard/summary", { month }));
    },
    dashboardMonths() {
        return request("/api/dashboard/months");
    },
    dashboardMonthDetail(yearMonth) {
        return request(`/api/dashboard/months/${yearMonth}`);
    },
    statements(filters = {}) {
        return request(withQuery("/api/statements", filters));
    },
    statement(id) {
        return request(`/api/statements/${id}`);
    },
    uploadStatements(files) {
        const formData = new FormData();
        [...files].forEach((file) => formData.append("files", file));
        return uploadRequest("/api/statements/upload", formData);
    },
    updateStatement(id, payload) {
        return request(`/api/statements/${id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });
    },
    confirmStatement(id) {
        return request(`/api/statements/${id}/confirm`, { method: "POST" });
    },
    createStatementTransaction(statementId, payload) {
        return request(`/api/statements/${statementId}/transactions`, {
            method: "POST",
            body: JSON.stringify(payload)
        });
    },
    transactions(filters = {}) {
        return request(withQuery("/api/transactions", filters));
    },
    manualExpenses(filters = {}) {
        return request(withQuery("/api/manual-expenses", filters));
    },
    createManualExpense(payload) {
        return request("/api/manual-expenses", {
            method: "POST",
            body: JSON.stringify(payload)
        });
    },
    updateManualExpense(id, payload) {
        return request(`/api/manual-expenses/${id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });
    },
    deleteManualExpense(id) {
        return request(`/api/manual-expenses/${id}`, { method: "DELETE" });
    },
    incomes(filters = {}) {
        return request(withQuery("/api/incomes", filters));
    },
    createIncome(payload) {
        return request("/api/incomes", {
            method: "POST",
            body: JSON.stringify(payload)
        });
    },
    updateIncome(id, payload) {
        return request(`/api/incomes/${id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });
    },
    updateIncomeFromMonth(id, yearMonth, payload) {
        return request(`/api/incomes/${id}/from-month/${yearMonth}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });
    },
    deleteIncome(id) {
        return request(`/api/incomes/${id}`, { method: "DELETE" });
    },
    updateTransaction(id, payload) {
        return request(`/api/transactions/${id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });
    },
    deleteTransaction(id) {
        return request(`/api/transactions/${id}`, { method: "DELETE" });
    },
    categories() {
        return request("/api/categories");
    },
    createCategory(payload) {
        return request("/api/categories", {
            method: "POST",
            body: JSON.stringify(payload)
        });
    },
    updateCategory(id, payload) {
        return request(`/api/categories/${id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });
    },
    deleteCategory(id) {
        return request(`/api/categories/${id}`, { method: "DELETE" });
    },
    superCategories() {
        return request("/api/super/categories");
    },
    createSuperCategory(payload) {
        return request("/api/super/categories", {
            method: "POST",
            body: JSON.stringify(payload)
        });
    },
    updateSuperCategory(id, payload) {
        return request(`/api/super/categories/${id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });
    },
    deleteSuperCategory(id) {
        return request(`/api/super/categories/${id}`, { method: "DELETE" });
    },
    superItems() {
        return request("/api/super/items");
    },
    createSuperItem(payload) {
        return request("/api/super/items", {
            method: "POST",
            body: JSON.stringify(payload)
        });
    },
    updateSuperItem(id, payload) {
        return request(`/api/super/items/${id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });
    },
    adjustSuperItemStock(id, currentStock) {
        return request(`/api/super/items/${id}/stock-adjustments`, {
            method: "POST",
            body: JSON.stringify({ currentStock })
        });
    },
    purchaseSuperItem(id, payload) {
        return request(`/api/super/items/${id}/purchases`, {
            method: "POST",
            body: JSON.stringify(payload)
        });
    },
    consumeSuperItem(id, payload) {
        return request(`/api/super/items/${id}/consumptions`, {
            method: "POST",
            body: JSON.stringify(payload)
        });
    },
    quickConsumeSuperItem(id, payload) {
        return request(`/api/super/items/${id}/quick-consumptions`, {
            method: "POST",
            body: JSON.stringify(payload)
        });
    },
    superStockMovements(filters = {}) {
        return request(withQuery("/api/super/movements", filters));
    },
    lookupSuperItemBarcodeAlias(code) {
        return request(withQuery("/api/super/barcode-aliases", { code }));
    },
    attachSuperItemBarcodeAlias(id, payload) {
        return request(`/api/super/items/${id}/barcode-aliases`, {
            method: "POST",
            body: JSON.stringify(payload)
        });
    },
    removeSuperItemBarcodeAlias(itemId, aliasId) {
        return request(`/api/super/items/${itemId}/barcode-aliases/${aliasId}`, { method: "DELETE" });
    },
    deleteSuperItem(id) {
        return request(`/api/super/items/${id}`, { method: "DELETE" });
    },
    updateSuperItemChecked(id, checked) {
        return request(`/api/super/items/${id}/checked`, {
            method: "PATCH",
            body: JSON.stringify({ checked })
        });
    },
    uncheckAllSuperItems() {
        return request("/api/super/items/uncheck-all", { method: "POST" });
    }
};

async function uploadRequest(path, body) {
    const response = await fetch(path, {
        method: "POST",
        credentials: "same-origin",
        headers: { "Accept": "application/json", ...csrfHeaders("POST") },
        body
    });

    if (!response.ok) {
        throw await apiError(response);
    }

    return response.json();
}

export function appendCsrfField(form) {
    if (!form) {
        return;
    }
    const token = csrfToken();
    if (!token) {
        return;
    }
    let input = form.querySelector("input[name='_csrf']");
    if (!input) {
        input = document.createElement("input");
        input.type = "hidden";
        input.name = "_csrf";
        form.append(input);
    }
    input.value = token;
}

function csrfHeaders(method) {
    if (!isUnsafeMethod(method)) {
        return {};
    }
    const token = csrfToken();
    return token ? { "X-XSRF-TOKEN": token } : {};
}

function csrfToken() {
    if (typeof document === "undefined" || typeof document.cookie !== "string") {
        return "";
    }
    const cookie = document.cookie
            .split(";")
            .map((cookie) => cookie.trim())
            .find((cookie) => cookie.startsWith("XSRF-TOKEN="));
    return cookie ? decodeURIComponent(cookie.slice("XSRF-TOKEN=".length)) : "";
}

function isUnsafeMethod(method) {
    return !["GET", "HEAD", "OPTIONS", "TRACE"].includes(method);
}
