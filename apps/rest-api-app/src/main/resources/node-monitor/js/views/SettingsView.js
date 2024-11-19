// js/views/SettingsView.js

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

export class SettingsView {
    constructor(reportView, storageService, dataService, onSettingsChangedCallback) {
        this.reportView = reportView;
        this.storageService = storageService;
        this.dataService = dataService;
        this.onSettingsChanged = onSettingsChangedCallback;
        this.#initialize();
    }

    toggleSettingsMenu() {
        const settingsPanel = document.getElementById("settingsPanel");
        const isCurrentlyHidden = settingsPanel.style.display === "none";
        settingsPanel.style.display = isCurrentlyHidden ? "block" : "none";
        if (isCurrentlyHidden) {
            this.#initializeAddressesTextAndPortsText();
        }
    }

    //////////////////////
    // PRIVATE METHODS
    //////////////////////

    #initialize() {
        this.#initializeHamburgerButton();
        this.#initializeFetchRemoteListButton();
        this.#initializeSaveConfigButton();
        this.#initializeSettingsView();
    }

    #initializeHamburgerButton() {
        document.getElementById("hamburgerButton").addEventListener("click", () => {
            this.toggleSettingsMenu();
        });
    }

    #initializeFetchRemoteListButton() {
        const fetchRemoteListButton = document.getElementById("fetchRemoteListButton");
        fetchRemoteListButton.classList.add("button", "button--orange");
        fetchRemoteListButton.addEventListener("click", async () => {
            await this.#fetchAndDisplayRemoteAddressList();
        });
    }

    #initializeSaveConfigButton() {
        const saveConfigButton = document.getElementById("saveConfigButton");
        saveConfigButton.classList.add("button", "button--green");
        saveConfigButton.addEventListener("click", () => {
            this.#saveCurrentConfiguration();
        });
    }

    #initializeAddressesTextAndPortsText() {
        const addressesText = this.storageService.getAddressesCookie();
        const portsText = this.storageService.getPortsCookie();
        document.getElementById("addressListInput").value = addressesText;
        document.getElementById("portListInput").value = portsText;
    }

    #initializeSettingsView() {

        document.getElementById("addressListInput").placeholder = Constants.PLACEHOLDER_ADDRESS_LIST;
        document.getElementById("portListInput").placeholder = Constants.PLACEHOLDER_PORT_LIST;

        const addressesText = this.storageService.getAddressesCookie() || '';
        const portsText = this.storageService.getPortsCookie() || '';
        document.getElementById("addressListInput").value = addressesText;
        document.getElementById("portListInput").value = portsText;

        const thresholds = this.storageService.getDeviationThresholds() || {
            LOW: 5,
            MEDIUM: 20,
            HIGH: 50
        };
        document.getElementById("lowThreshold").value = thresholds.LOW;
        document.getElementById("mediumThreshold").value = thresholds.MEDIUM;
        document.getElementById("highThreshold").value = thresholds.HIGH;
    }

    async #fetchAndDisplayRemoteAddressList() {
        this.reportView.clearMessage();
        try {
            const remoteAddresses = await this.dataService.fetchAddressList();
            if (remoteAddresses && Array.isArray(remoteAddresses)) {
                const newValue = remoteAddresses.join('\n');
                if (newValue.length > 0) {
                    document.getElementById("addressListInput").value = newValue;
                } else {
                    this.reportView.renderErrorMessage("Received address list is empty.");
                }
            } else {
                this.reportView.renderErrorMessage("Failed to fetch remote address list.");
            }
        } catch (error) {
            this.reportView.renderErrorMessage(error.message);
        }
    }

    #saveCurrentConfiguration() {
        const addressesInput = document.getElementById("addressListInput").value;
        const portsInput = document.getElementById("portListInput").value;

        const lowThreshold = parseInt(document.getElementById("lowThreshold").value, 10);
        const mediumThreshold = parseInt(document.getElementById("mediumThreshold").value, 10);
        const highThreshold = parseInt(document.getElementById("highThreshold").value, 10);

        try {
            this.storageService.saveAddressesAndPorts(addressesInput, portsInput);

            const newThresholds = {
                LOW: lowThreshold,
                MEDIUM: mediumThreshold,
                HIGH: highThreshold
            };
            this.storageService.saveDeviationThresholds(newThresholds);

            Constants.initialize({ DEVIATION_THRESHOLDS: newThresholds });

            this.reportView.renderInfoMessage("Configuration saved successfully.", 1);
            this.toggleSettingsMenu();
            Promise.resolve().then(() => this.onSettingsChanged());
        } catch (error) {
            this.reportView.renderErrorMessage(error.message);
        }
    }
};
