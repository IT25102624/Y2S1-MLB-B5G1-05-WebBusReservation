const API_BASE = "/api/v1/admin";


async function request(path, options = {}) {

    const token =
        localStorage.getItem("authToken");

    const headers = {
        ...(token
            ? { Authorization: `Bearer ${token}` }
            : {}),
        ...(options.headers || {})
    };


    const response = await fetch(
        `${API_BASE}${path}`,
        {
            ...options,
            headers
        }
    );


    if (!response.ok) {

        const errorBody =
            await response.json().catch(() => ({}));

        throw new Error(
            errorBody.message ||
            `Request failed: ${response.status}`
        );

    }


    const contentType =
        response.headers.get("content-type") || "";


    if (contentType.includes("application/json")) {

        const json =
            await response.json();

        return json.data !== undefined
            ? json.data
            : json;

    }


    return response.blob();
}


function toQuery(params = {}) {

    const query =
        Object.entries(params)

            .filter(
                ([, value]) =>
                    value !== undefined &&
                    value !== null &&
                    value !== ""
            )

            .map(
                ([key, value]) =>
                    `${encodeURIComponent(key)}=${encodeURIComponent(value)}`
            )

            .join("&");


    return query
        ? `?${query}`
        : "";
}


const adminApi = {

    getDashboardSummary() {

        return request(
            "/dashboard/summary"
        );

    },


    getRevenueSummary(filters = {}) {

        return request(
            `/dashboard/revenue${toQuery(filters)}`
        );

    },


    getRoutePerformance(filters = {}) {

        return request(
            `/dashboard/routes/performance${toQuery(filters)}`
        );

    },


    exportCsv(filters = {}) {

        return request(
            `/dashboard/export/csv${toQuery(filters)}`
        );

    },


    exportPdf(filters = {}) {

        return request(
            `/dashboard/export/pdf${toQuery(filters)}`
        );

    },


    dispatchSummary(recipientRole) {

        return request(
            `/dashboard/dispatch-summary${toQuery({
                recipientRole
            })}`,
            {
                method: "POST"
            }
        );

    },


    updateRoute(routeId, data) {

        return request(
            `/routes/${routeId}`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(data)
            }
        );

    },


    deleteRoute(routeId) {

        return request(
            `/routes/${routeId}`,
            {
                method: "DELETE"
            }
        );

    }

};


window.adminApi = adminApi;

