// js/services/DataService.js

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
import { Config } from '../Config.js';
import { NodeMonitroError, ServerError, DataValidationError, NetworkError } from '../errors.js';

export class DataService {
    async fetchReportData(address) {
        const url = `${Config.API_URL_GET_REPORT}/${address}`;
        try {
            const response = await fetch(url);
            if (!response.ok) {
                const statusText = response.statusText || `Status code: ${response.status}`;
                throw new ServerError(`Error fetching report: ${statusText}`, response.status);
            }

            const contentType = response.headers.get("Content-Type") || "";
            if (!contentType.startsWith("application/json")) {
                const errorMessage = await response.text();
                throw new DataValidationError(`Unexpected content type: ${errorMessage}`);
            }

            const report = await response.json();
            const filteredReport = this.#filterReportData(report);
            return { success: true, report: filteredReport, errorMessage: undefined };
        } catch (error) {
            if (error instanceof NodeMonitroError) {
                console.error(`${error.message} Address: ${address}.`, error);
                return { success: false, report: undefined, errorMessage: error.message };
            } else {
                console.error(`Network error: Unable to fetch report for ${address}`, error);
                return { success: false, report: undefined, errorMessage: error.message };
            }
        }
    }

    async fetchAddressList() {
        try {
            const response = await fetch(Config.API_URL_GET_ADDRESSES);
            if (!response.ok) {
                const statusText = response.statusText || `Status code: ${response.status}`;
                throw new ServerError(`Error fetching address list: ${statusText}`, response.status);
            }

            const contentType = response.headers.get("Content-Type") || "";
            if (!contentType.startsWith("application/json")) {
                const errorMessage = await response.text();
                throw new DataValidationError(`Unexpected content type for address list: ${errorMessage}`);
            }

            const data = await response.json();
            if (!data || !Array.isArray(data)) {
                throw new DataValidationError("Invalid data format: expected an array.");
            }

            return data;
        } catch (error) {
            if (error instanceof NodeMonitroError) {
                console.error(error);
                throw error;
            } else {
                console.error("Network error: Unable to fetch address list:", error);
                throw new NetworkError(`Network error while fetching address list: ${error.message}`);
            }
        }
    }

    async fetchAddressDetails(addressList) {
        try {
            const url = Config.API_URL_POST_ADDRESSES_DETAILS;
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(addressList),
            });

            if (!response.ok) {
                const statusText = response.statusText || `Status code: ${response.status}`;
                throw new ServerError(`Error fetching address details: ${statusText}`, response.status);
            }

            const contentType = response.headers.get("Content-Type") || "";
            if (!contentType.startsWith("application/json")) {
                const errorMessage = await response.text();
                throw new DataValidationError(`Unexpected content type for address details: ${errorMessage}`);
            }

            return await response.json();
        } catch (error) {
            if (error instanceof NodeMonitroError) {
                console.error(error);
                throw error;
            } else {
                console.error("Network error: Unable to fetch address details:", error);
                throw new NetworkError(`Network error while fetching address details: ${error.message}`);
            }
        }
    }

    //////////////////////
    // PRIVATE METHODS
    //////////////////////

    #filterReportData(report) {
        if (!report || typeof report !== 'object') {
            return report;
        }
        const { version = null, serializedSize = null, excludedFields = null, ...filteredReport } = report;
        return filteredReport;
    }
}
