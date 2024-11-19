// js/controllers/ReportController.js

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
import { DOMUtils } from '../utils/DOMUtils.js';
import { KeyUtility } from '../utils/KeyUtils.js';
import { ReportDiffsController } from './ReportDiffsController.js';

export class ReportController {
    constructor(dataService, reportView) {
        this.dataService = dataService;
        this.reportView = reportView;
        this.diffRenderer = new ReportDiffsController(this);
        this.reports = [];
        this.renderQueue = Promise.resolve();
    }

    init() {
        this.#initToggleAllButton();
        this.updateButtons();
    }

    async loadAllReports(addressList, portList = []) {
        console.log("Loading reports for addresses:", addressList);

        this.#removeObsoleteReports(addressList, portList);
        this.diffRenderer.reset();

        const processedAddresses = new Set();

        for (const address of addressList) {
            if (processedAddresses.has(address)) {
                console.warn(`Skipping duplicate address: ${address}`);
                continue;
            }

            processedAddresses.add(address);

            const addressPort = parseInt(address.split(":")[1]);
            if (!portList.length || portList.includes(addressPort)) {
                this.#createOrUpdatePlaceholderForReport(address);

                (async () => {
                    const success = await this.#loadSingleReport(address);
                    if (success) {
                        this.reports.push({ address, success });
                        this.updateButtons();
                    }
                })();
            } else {
                console.log(`Skipping address as port not in filter list: ${address}`);
            }
        }
    }

    updateButtons() {
        const toggleAllButton = document.getElementById("toggleAllButton");
        const hasSuccessfulReports = this.reports.some(report => report.success);

        if ((hasSuccessfulReports && toggleAllButton.style.display !== "block") ||
            (!hasSuccessfulReports && toggleAllButton.style.display !== "none")) {
            toggleAllButton.style.display = hasSuccessfulReports ? "block" : "none";
        }
    }

    toggleAllDetails(expand) {
        const toggleButtons = document.querySelectorAll('.toggle-button');
        toggleButtons.forEach(button => {
            try {
                if (button.dataset.details) {
                    this.reportView.toggleDetailButton(button, expand);
                }
            } catch (error) {
                console.warn("Skipping button due to invalid JSON in dataset.details:", button);
            }
        });

        const toggleAllButton = document.getElementById("toggleAllButton");
        toggleAllButton.textContent = expand
            ? Constants.BUTTON_COLLAPSE_ALL
            : Constants.BUTTON_EXPAND_ALL;
    }

    /////////////////////////
    // PRIVATE METHODS
    /////////////////////////

    #initToggleAllButton() {
        const toggleAllButton = document.getElementById("toggleAllButton");

        toggleAllButton.addEventListener("click", () => {
            const expand = toggleAllButton.textContent === Constants.BUTTON_EXPAND_ALL;
            this.toggleAllDetails(expand);
        });
    }

    async #loadSingleReport(address) {
        console.log(`Loading new report for address: ${address}`);
        try {
            const data = await this.dataService.fetchReportData(address);

            this.reportView.renderSingleReport(data, address);
            this.diffRenderer.addReport(data, address);

            // Ensure sequential rendering of report diffs to maintain consistency and avoid race conditions.
            this.renderQueue = this.renderQueue
                .then(() => this.diffRenderer.scheduleRender())
                .catch(error => console.error("Render queue error:", error));

            this.updateButtons();
            return true;
        } catch (error) {
            const message = `Error loading report for ${address}: ${error.message}`;
            console.error(message, error);
            this.reportView.renderErrorMessage(message);
            return false;
        }
    }

    #createOrUpdatePlaceholderForReport(address) {
        const existingNodeBlock = DOMUtils.findReportElementByAddress(address);
        if (!existingNodeBlock) {
            this.#createNewPlaceholder(address);
        } else {
            this.#reuseExistingReport(existingNodeBlock);
        }
    }

    #reuseExistingReport(nodeBlock) {
        console.log(`Reusing existing placeholder for ${nodeBlock.id}`);
        this.#markPlaceholderLoading(nodeBlock);
        this.#clearReportData(nodeBlock);
    }

    #createNewPlaceholder(address) {
        const nodeBlock = document.createElement('div');
        nodeBlock.classList.add('node-block');
        nodeBlock.id = KeyUtility.generateReportNodeId(address);

        const header = document.createElement('div');
        header.classList.add('node-header');

        const statusCircle = document.createElement('span');
        statusCircle.classList.add('status-circle', 'status-loading');
        header.appendChild(statusCircle);

        const addressText = document.createElement('span');
        addressText.textContent = address;
        header.appendChild(addressText);

        nodeBlock.appendChild(header);
        document.getElementById("reportContainer").appendChild(nodeBlock);
    }

    #markPlaceholderLoading(nodeBlock) {
        console.log(`Marking placeholder loading for ${nodeBlock.id}`);
        const statusCircle = nodeBlock.querySelector('.status-circle');
        statusCircle.className = 'status-circle status-loading';
    }

    #clearReportData(nodeBlock) {
        console.log(`Clearing report data for ${nodeBlock.id}`);
        Array.from(nodeBlock.children).forEach(child => {
            if (!child.classList.contains('node-header')) {
                nodeBlock.removeChild(child);
            }
        });
    }

    #removeObsoleteReports(addressList, portList) {
        const existingNodes = document.querySelectorAll('.node-block');

        existingNodes.forEach(node => {
            const address = KeyUtility.extractAddressFromReportNodeId(node.id);
            const port = parseInt(address.split(":")[1]);

            if (!addressList.includes(address) || (portList.length > 0 && !portList.includes(port))) {
                console.log(`Removing obsolete report placeholder for address: ${address}`);
                node.remove();
            }
        });
    }
}
