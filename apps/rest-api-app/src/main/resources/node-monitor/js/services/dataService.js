// js/services/dataService.js
App.Services = App.Services || {};

App.Services.DataService = class {
    async fetchReportData(host) {
        const url = `${App.Constants.API_URL_GET_REPORT}/${host}`;
        try {
            const response = await fetch(url);
            if (!response.ok) {
                const statusText = response.statusText || `Status code: ${response.status}`;
                throw new Error(`Error fetching report for ${host}: ${statusText}`);
            }
            const contentType = response.headers.get("Content-Type") || "";
            if (!contentType.startsWith("application/json")) {
                const errorMessage = await response.text();
                throw new Error(`Unexpected content type for ${host}: ${errorMessage}`);
            }
            const data = await response.json();
            return { success: true, data: data };
        } catch (error) {
            throw new Error(`Network error: Unable to fetch report for ${host}: ${error.message}`);
        }
    }

    async fetchHostList() {
        try {
            const response = await fetch(App.Constants.API_URL_GET_ADDRESS_LIST);
            if (!response.ok) {
                const statusText = response.statusText || `Status code: ${response.status}`;
                throw new Error(`Server error while fetching host list: ${statusText}`);
            }
            const contentType = response.headers.get("Content-Type") || "";
            if (!contentType.startsWith("application/json")) {
                const errorMessage = await response.text();
                throw new Error(`Unexpected content type for ${host}: ${errorMessage}`);
            }
            const data = await response.json();
            if (!data || !Array.isArray(data)) {
                throw new Error("Invalid data format: expected an array.");
            }
            return data;
        } catch (error) {
            throw new Error("Network error: Unable to fetch host list: " + error.message);
        }
    }

    //////////////////////
    // PRIVATE METHODS
    //////////////////////
};
