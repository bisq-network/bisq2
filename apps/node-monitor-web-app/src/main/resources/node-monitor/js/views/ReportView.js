// js/views/ReportView.js

/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

import { Constants } from '../Constants.js';
import { KeyUtility } from '../utils/KeyUtils.js';
import { DOMUtils } from '../utils/DOMUtils.js';

export class ReportView {
    constructor() { }

    renderInfoMessage(message, duration) {
        this.#renderMessage(message, "green", duration);
    }

    renderWarnMessage(message, duration) {
        this.#renderMessage(message, "orange", duration);
    }

    renderErrorMessage(message, duration) {
        this.#renderMessage(message, "red", duration);
    }

    clearMessage() {
        const statusMessage = document.getElementById("statusMessage");
        statusMessage.style.display = "none";
    }

    renderSingleReport(data, address) {
        const nodeBlock = document.getElementById(KeyUtility.generateReportNodeId(address));

        const existingTableOrError = nodeBlock.querySelector('.table-container, .error');
        if (existingTableOrError) {
            existingTableOrError.remove();
        }

        const existingRoleType = nodeBlock.querySelector('.role-type');
        const existingUserName = nodeBlock.querySelector('.user-name');

        let header = nodeBlock.querySelector('.node-header');
        if (!header) {
            header = document.createElement('div');
            header.classList.add('node-header');
            nodeBlock.appendChild(header);
        } else {
            header.innerHTML = '';
        }

        const statusCircle = document.createElement('span');
        statusCircle.classList.add('status-circle');
        statusCircle.classList.add(data.success ? 'status-ok' : 'status-error');
        header.appendChild(statusCircle);

        const addressText = document.createElement('span');
        addressText.textContent = address;
        header.appendChild(addressText);

        if (existingRoleType) {
            header.appendChild(existingRoleType);
        }
        if (existingUserName) {
            header.appendChild(existingUserName);
        }

        if (data.success) {
            const mainTable = this.#createTable(data.report, Constants.REPORT_KEY_ROOT_PARENT, Constants.REPORT_KEY_ROOT, 0);
            nodeBlock.appendChild(mainTable);
        } else {
            const errorDiv = document.createElement('div');
            errorDiv.classList.add('error');
            errorDiv.textContent = data.errorMessage || Constants.STATUS_ERROR;
            nodeBlock.appendChild(errorDiv);
        }
    }

    renderAddressDetails(dto) {
        const nodeBlock = document.getElementById(KeyUtility.generateReportNodeId(dto.address));
        if (!nodeBlock) return;

        const header = nodeBlock.querySelector('.node-header');

        let roleText = header.querySelector('.role-type');
        if (!roleText) {
            roleText = document.createElement('span');
            roleText.classList.add('role-type');
            header.appendChild(roleText);
        }
        roleText.textContent = ` ${dto.bondedRoleType} `;

        let userText = header.querySelector('.user-name');
        if (!userText) {
            userText = document.createElement('span');
            userText.classList.add('user-name');
            header.appendChild(userText);
        }
        userText.textContent = `${dto.nickNameOrBondUserName}`;
    }

    toggleDetailButton(button, expand) {
        const fullKey = button.dataset.reftable_fullkey;
        const tableContainer = button.closest('.table-container');
        if (fullKey && tableContainer) {
            const subTable = DOMUtils.findTableContainerInElementByFullKey(tableContainer, fullKey);
            if (subTable) {
                const isVisible = expand !== undefined ? expand : subTable.style.display === 'none';

                subTable.style.display = isVisible ? 'block' : 'none';
                button.textContent = isVisible ? Constants.BUTTON_COLLAPSE_DETAILS : Constants.BUTTON_EXPAND_DETAILS;
            } else {
                console.warn(`Subtable with fullKey "${fullKey}" not found.`);
            }
        } else {
            console.warn('Button or table-container is invalid.');
        }
    }

    //////////////////////
    // PRIVATE METHODS
    //////////////////////

    #renderMessage(message, color = "grey", duration) {
        const statusMessage = document.getElementById("statusMessage");
        statusMessage.textContent = message;
        statusMessage.style.color = color;
        statusMessage.style.display = "block";

        if (duration) {
            setTimeout(() => {
                this.clearMessage();
            }, duration * 1000);
        }
    }

    #formatColumnName(name) {
        return name.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase());
    }

    #createTable(data, parentFullKey, key, depth = 0) {

        const tableDiv = document.createElement('div');

        tableDiv.classList.add('table-container');
        tableDiv.style.marginLeft = `${depth * 20}px`;

        let fullKey;

        if (depth !== 0) {
            const tableTitle = document.createElement('div');
            tableTitle.classList.add('sub-structure-title');
            tableTitle.textContent = this.#formatColumnName(key);
            tableDiv.appendChild(tableTitle);
            fullKey = KeyUtility.createFullKey(parentFullKey, key);
        } else {
            fullKey = Constants.REPORT_KEY_ROOT_PARENT;
        }

        DOMUtils.setDataFullKey(tableDiv, fullKey);

        const table = document.createElement('table');
        const headerRow = document.createElement('tr');
        const valueRow = document.createElement('tr');

        const entries = Object.entries(data);
        const primitiveEntries = entries.filter(([, value]) => typeof value !== 'object' || value === null);
        const complexEntries = entries.filter(([, value]) => typeof value === 'object' && value !== null);
        const sortedEntries = [...primitiveEntries, ...complexEntries];
        const subTableList = [];

        sortedEntries.forEach(([entryKey, entryValue]) => {
            const subFullKey = KeyUtility.createFullKey(fullKey, entryKey);

            const th = document.createElement('th');
            th.textContent = this.#formatColumnName(entryKey);
            th.title = entryKey;
            headerRow.appendChild(th);

            const td = document.createElement('td');
            DOMUtils.setDataFullKey(td, subFullKey);

            if (typeof entryValue === 'object' && entryValue !== null) {
                const button = document.createElement('button');
                button.classList.add('button', 'button--blue', 'toggle-button');
                button.textContent = Constants.BUTTON_EXPAND_DETAILS;
                button.dataset.depth = depth;
                button.dataset.reftable_fullkey = subFullKey;
                button.dataset.details = JSON.stringify(entryValue);
                button.tableDiv = tableDiv;
                button.onclick = () => this.toggleDetailButton(button);
                td.appendChild(button);

                const subTable = this.#createTable(entryValue, fullKey, entryKey, depth + 1);
                subTable.style.display = 'none';
                subTableList.push(subTable);
            } else {
                if (typeof entryValue === 'number') {
                    td.textContent = this.#formatNumber(entryValue);
                } else {
                    td.textContent = entryValue !== undefined ? entryValue : "N/A";
                }
            }
            valueRow.appendChild(td);
        });

        table.appendChild(headerRow);
        table.appendChild(valueRow);
        tableDiv.appendChild(table);
        subTableList.forEach(subTable => tableDiv.appendChild(subTable));

        return tableDiv;
    }

    #formatNumber(value) {
        if (Number.isInteger(value)) {
            return value.toString();
        }
        return parseFloat(value.toFixed(10)).toString();
    }
};
