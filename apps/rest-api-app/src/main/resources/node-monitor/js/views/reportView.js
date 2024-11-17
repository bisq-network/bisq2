// js/views/reportView.js
App.Views = App.Views || {};

App.Views.ReportView = class {
    constructor() {}

    renderInfoMessage(message, dauer) {
        this.#renderMessage(message, "green", dauer);
    }

    renderWarnMessage(message, dauer) {
        this.#renderMessage(message, "orange", dauer);
    }

    renderErrorMessage(message, dauer) {
        this.#renderMessage(message, "red", dauer);
    }

    clearMessage() {
        const statusMessage = document.getElementById("statusMessage");
        statusMessage.style.display = "none";
    }

    renderSingleReport(data, host) {
        const nodeBlock = document.getElementById(`report-${host.replace(/[:.]/g, '-')}`);
        nodeBlock.innerHTML = '';

        const header = document.createElement('div');
        header.classList.add('node-header');

        const statusCircle = document.createElement('span');
        statusCircle.classList.add('status-circle');

        //todo
        //statusCircle.classList.add(data.successful ? 'status-ok' : 'status-error');
        statusCircle.classList.add('status-ok');
        header.appendChild(statusCircle);

        const hostText = document.createElement('span');
        hostText.textContent = host;
        header.appendChild(hostText);

        nodeBlock.appendChild(header);

        //todo
        //if (data.successful) {
            const mainTable = this.#createTable(data, "Report", 0);
            nodeBlock.appendChild(mainTable);
       /* } else {
            const errorDiv = document.createElement('div');
            errorDiv.classList.add('error');
            errorDiv.textContent = data.errorMessage || App.Constants.STATUS_ERROR;
            nodeBlock.appendChild(errorDiv);
        }*/
    }

    toggleDetailButton(button, expand) {
        const details = JSON.parse(button.dataset.details);
        const key = button.dataset.key;
        const depth = parseInt(button.dataset.depth);

        if (!button.classList.contains('expanded')) {
            const nestedTable = this.#createTable(details, key, depth + 1);
            nestedTable.style.display = 'none';
            button.tableDiv.appendChild(nestedTable);
            button.classList.add('expanded');
            button.nestedTable = nestedTable;
        }
        const isVisible = expand !== undefined ? expand : button.nestedTable.style.display === 'none';
        button.nestedTable.style.display = isVisible ? 'block' : 'none';
        button.textContent = isVisible ? BUTTON_COLLAPSE_DETAILS : BUTTON_EXPAND_DETAILS;
    }

    //////////////////////
    // PRIVATE METHODS
    //////////////////////

    #renderMessage(message, color = "grey", dauer) {
        const statusMessage = document.getElementById("statusMessage");
        statusMessage.textContent = message;
        statusMessage.style.color = color;
        statusMessage.style.display = "block";

        if (dauer) {
            setTimeout(() => {
                this.clearMessage();
            }, dauer * 1000);
        }
    }

    #formatColumnName(name) {
        return name.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase());
    };

    #createTable(data, title, depth = 0) {
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
            th.textContent = this.#formatColumnName(key);
            th.title = key;
            headerRow.appendChild(th);

            const td = document.createElement('td');
            const details = data[key];

            if (complexKeys.includes(key)) {
                const button = document.createElement('button');
                button.classList.add('button', 'button--blue', 'toggle-button');
                button.textContent = App.Constants.BUTTON_EXPAND_DETAILS;
                button.dataset.key = key;
                button.dataset.details = JSON.stringify(details);
                button.dataset.depth = depth;
                button.tableDiv = tableDiv;
                button.onclick = () => this.toggleDetailButton(button);
                td.appendChild(button);
            } else {
                td.textContent = details !== undefined ? details : "N/A";
            }
            valueRow.appendChild(td);
        });

        table.appendChild(headerRow);
        table.appendChild(valueRow);
        tableDiv.appendChild(table);

        return tableDiv;
    }
};

