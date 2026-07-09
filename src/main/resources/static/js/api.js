const headers = {
    "Content-Type": "application/json",
    "Accept": "application/json"
};

async function request(path, options = {}) {
    const response = await fetch(path, {
        headers,
        ...options
    });

    if (!response.ok) {
        throw new Error(await errorMessage(response));
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

async function errorMessage(response) {
    try {
        const body = await response.json();
        if (Array.isArray(body.details) && body.details.length > 0) {
            return safeApiMessage(body.details.join(" "), response.status);
        }
        return safeApiMessage(body.error, response.status);
    } catch {
        return safeApiMessage("", response.status);
    }
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
    }
};

async function uploadRequest(path, body) {
    const response = await fetch(path, {
        method: "POST",
        headers: { "Accept": "application/json" },
        body
    });

    if (!response.ok) {
        throw new Error(await errorMessage(response));
    }

    return response.json();
}
