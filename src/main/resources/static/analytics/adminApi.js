const API_BASE = "/api/v1/admin";
const ROUTES_BASE = "/api/v1/routes";

async function request(path, options = {}) {
    const token = localStorage.getItem("authToken");

    const headers = {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.headers || {})
    };

    const response = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers
    });

    if (!response.ok) {
        const errorBody = await response.json().catch(() => ({}));
        throw new Error(errorBody.message || `Request failed: ${response.status}`);
    }

    const contentType = response.headers.get("content-type") || "";

    if (contentType.includes("application/json")) {
        const json = await response.json();
        return json.data !== undefined ? json.data : json;
    }

    return response.blob();
}

async function routeRequest(path, options = {}) {
    const token = localStorage.getItem("authToken");

    const headers = {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.headers || {})
    };

    const response = await fetch(`${ROUTES_BASE}${path}`, {
        ...options,
        headers
    });

    if (!response.ok) {
        const errorBody = await response.json().catch(() => ({}));
        throw new Error(errorBody.message || `Request failed: ${response.status}`);
    }

    const contentType = response.headers.get("content-type") || "";

    if (contentType.includes("application/json")) {
        return await response.json();
    }

    return true;
}

function toQuery(params = {}) {
    const query = Object.entries(params)
        .filter(([, value]) => value !== undefined && value !== null && value !== "")
        .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
        .join("&");

    return query ? `?${query}` : "";
}

const adminApi = {
    getDashboardSummary() {
        return request("/dashboard/summary");
    },

    getRevenueSummary(filters = {}) {
        return request(`/dashboard/revenue${toQuery(filters)}`);
    },

    getRoutePerformance(filters = {}) {
        return request(`/dashboard/routes/performance${toQuery(filters)}`);
    },

    exportCsv(filters = {}) {
        return request(`/dashboard/export/csv${toQuery(filters)}`);
    },

    exportPdf(filters = {}) {
        return request(`/dashboard/export/pdf${toQuery(filters)}`);
    },

    dispatchSummary(recipientRole) {
        return request(`/dashboard/dispatch-summary${toQuery({ recipientRole })}`, {
            method: "POST"
        });
    },

    createRoute(data) {
        return routeRequest("", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });
    },

    updateRoute(routeId, data) {
        return routeRequest(`/${routeId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });
    },

    deleteRoute(routeId) {
        return routeRequest(`/${routeId}`, {
            method: "DELETE"
        });
    }
};

window.adminApi = adminApi;