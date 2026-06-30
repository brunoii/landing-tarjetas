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
            return body.details.join(" ");
        }
        return body.error || `Request failed with status ${response.status}`;
    } catch {
        return `Request failed with status ${response.status}`;
    }
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
    statements(filters = {}) {
        return request(withQuery("/api/statements", filters));
    },
    transactions(filters = {}) {
        return request(withQuery("/api/transactions", filters));
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
