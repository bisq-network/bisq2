// js/controllers/AppController.js

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
import { ReportController } from './ReportController.js';
import { ReportView } from '../views/ReportView.js';
import { SettingsView } from '../views/SettingsView.js';

export class AppController {
    constructor(dataService, storageService) {
        this.dataService = dataService;
        this.storageService = storageService;
        this.reportView = new ReportView();
        this.settingsView = new SettingsView(
            this.reportView,
            this.storageService,
            this.dataService,
            this.onSettingsChanged.bind(this)
        );

        this.reportController = new ReportController(this.dataService, this.reportView);
    }

    async initApp() {
        try {
            await this.#updateAddressesAndLoadAllReports();
            this.#setInitialUIState();
            await this.#loadAddressDetails();
            this.reportController.init();
        } catch (error) {
            console.error("Error on init app", error);
            this.reportView.renderErrorMessage(error.message);
        }
    }

    async onSettingsChanged() {
        try {
            await this.#updateAddressesAndLoadAllReports();
            await this.#loadAddressDetails();
        } catch (error) {
            this.reportView.renderErrorMessage(error.message);
        }
    }

    //////////////////////
    // PRIVATE METHODS
    //////////////////////

    #setInitialUIState() {
        const hasAddresses = this.addressList && this.addressList.length > 0;
        this.reportController.updateButtons();
        this.#toggleReloadButton(hasAddresses);
    }

    #toggleReloadButton(show) {
        const button = document.getElementById("reloadButton");
        button.style.display = show ? "block" : "none";

        button.addEventListener("click", () => {
            if (this.addressList.length > 0) {
                this.reportController.loadAllReports(this.addressList, this.portList);
            } else {
                this.reportView.renderErrorMessage(Constants.STATUS_ENTER_ADDRESSES);
            }
        });
    }

    async #getAddresses() {
        let addresses = this.storageService.getAddressesFromCookie();
        if (addresses.length === 0) {
            addresses = await this.dataService.fetchAddressList();
            if (addresses && addresses.length > 0) {
                this.storageService.saveAddresses(addresses.join('\n'));
                this.reportView.clearMessage();
            } else {
                throw new Error("No addresses available from server.");
            }
        }
        return addresses;
    }

    async #updateAddressesAndLoadAllReports() {
        try {
            this.addressList = this.storageService.getAddressesFromCookie();
            this.portList = this.storageService.getPortsFromCookie();

            if (this.addressList.length === 0) {
                this.addressList = await this.#getAddresses();
                if (this.addressList.length === 0) {
                    this.reportView.renderErrorMessage(Constants.STATUS_ENTER_ADDRESSES);
                    this.reportController.updateButtons();
                    return;
                }
            }

            this.reportController.loadAllReports(this.addressList, this.portList);
        } catch (error) {
            this.reportView.renderErrorMessage(error.message);
            this.reportController.updateButtons();
        }
    }

    async #loadAddressDetails() {
        try {
            const addressDetails = await this.dataService.fetchAddressDetails(this.addressList);
            addressDetails.forEach(dto => {
                this.reportView.renderAddressDetails(dto);
            });
        } catch (error) {
            this.reportView.renderWarnMessage(error.message, 5);
        }
    }
}
