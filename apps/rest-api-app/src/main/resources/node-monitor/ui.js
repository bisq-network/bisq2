// UI related functions

function toggleSettingsMenu() {
    const settingsPanel = document.getElementById("settingsPanel");
    settingsPanel.style.display = settingsPanel.style.display === "none" || !settingsPanel.style.display ? "block" : "none";
}

function createPlaceholderForHost(host) {
    const reportContainer = document.getElementById("reportContainer");

    const nodeBlock = document.createElement('div');
    nodeBlock.classList.add('node-block');
    nodeBlock.id = `report-${host.replace(/[:.]/g, '-')}`;

    const header = document.createElement('div');
    header.classList.add('node-header');

    const statusCircle = document.createElement('span');
    statusCircle.classList.add('status-circle', 'status-loading');
    header.appendChild(statusCircle);

    const hostText = document.createElement('span');
    hostText.textContent = host;
    header.appendChild(hostText);

    nodeBlock.appendChild(header);
    reportContainer.appendChild(nodeBlock);
}

function displaySingleReport(data, host) {
    const nodeBlock = document.getElementById(`report-${host.replace(/[:.]/g, '-')}`);
    const statusCircle = nodeBlock.querySelector('.status-circle');

    statusCircle.classList.remove('status-loading');

    if (data.successful && data.report) {
        statusCircle.classList.add('status-ok');
        const mainTable = createTable(data.report, "Report", 0);
        nodeBlock.appendChild(mainTable);
    } else {
        statusCircle.classList.add('status-error');
        const errorDiv = document.createElement('div');
        errorDiv.classList.add('error');

        errorDiv.textContent = data.errorMessage || "Failed to fetch report";
        nodeBlock.appendChild(errorDiv);
    }
}

function createTable(data, title, depth = 0) {
    const tableDiv = document.createElement('div');
    tableDiv.classList.add('table-container');
    tableDiv.style.marginLeft = `${depth * 20}px`;

    const tableTitle = document.createElement('div');
    tableTitle.classList.add('sub-structure-title');
    tableTitle.textContent = title;
    tableDiv.appendChild(tableTitle);

    const table = document.createElement('table');
    const headerRow = document.createElement('tr');
    const valueRow = document.createElement('tr');

    const keys = Object.keys(data);
    const simpleKeys = keys.filter(key => typeof data[key] !== 'object' || data[key] === null);
    const complexKeys = keys.filter(key => typeof data[key] === 'object' && data[key] !== null);
    const sortedKeys = [...simpleKeys, ...complexKeys];

    sortedKeys.forEach(key => {
        const th = document.createElement('th');
        th.textContent = formatColumnName(key);
        th.title = key;
        headerRow.appendChild(th);

        const td = document.createElement('td');
        const value = data[key];

        if (complexKeys.includes(key)) {
            const button = document.createElement('button');
            button.classList.add('toggle-button');
            button.textContent = "Expand Details";
            button.dataset.key = key;
            button.dataset.value = JSON.stringify(value);
            button.dataset.depth = depth;
            button.tableDiv = tableDiv;
            button.onclick = () => toggleDetailButton(button);
            td.appendChild(button);
        } else {
            td.textContent = value !== undefined ? value : "N/A";
        }
        valueRow.appendChild(td);
    });

    table.appendChild(headerRow);
    table.appendChild(valueRow);
    tableDiv.appendChild(table);

    return tableDiv;
}

