// Core app logic

async function loadDataForHost(host, totalHosts, completedCallback) {
    try {
        const reportsUrl = `http://localhost:8082/api/v1/report/get-report/${host}`;
        const reportsResponse = await fetch(reportsUrl);

        const contentType = reportsResponse.headers.get("Content-Type") || "";

        let reportData;
        if (contentType.startsWith("application/json")) {
            try {
                reportData = await reportsResponse.json();
            } catch (parseError) {
                const responseText = await reportsResponse.text();
                reportData = { successful: false, errorMessage: responseText || "Invalid JSON response" };
            }
        } else {
            const responseText = await reportsResponse.text();
            reportData = { successful: false, errorMessage: responseText };
        }

        displaySingleReport(reportData, host);

    } catch (error) {
        displaySingleReport({ successful: false, errorMessage: "Failed to fetch report" }, host);
    } finally {
        completedCallback();
    }
}

function updateStatusMessage(message, color = "grey") {
    const statusMessage = document.getElementById("statusMessage");
    statusMessage.textContent = message;
    statusMessage.style.color = color;
    statusMessage.style.display = "block";
}

function loadData() {
    const hostList = getFilteredHostList();
 
    if (hostList.length === 0) {
        updateStatusMessage("Please enter a Host:port list in the settings to start fetching data.", "red");
        return;
    }

    updateStatusMessage("Loading...", "grey");

    const reportContainer = document.getElementById("reportContainer");
    reportContainer.innerHTML = "";

    let completedRequests = 0;
    const totalHosts = hostList.length;

    hostList.forEach(host => {
        createPlaceholderForHost(host);
        loadDataForHost(host, totalHosts, () => {
            completedRequests += 1;
            if (completedRequests === totalHosts) {
                document.getElementById("statusMessage").style.display = "none";
            }
        });
    });
}

