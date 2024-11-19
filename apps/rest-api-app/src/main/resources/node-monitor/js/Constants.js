// js/Constants.js

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

export const Constants = {
    MODE_DEV: 'dev',
    MODE_PROD: 'prod',
    MODE: null,

    API_URL_GET_REPORT: 'http://localhost:8082/api/v1/report',
    API_URL_GET_ADDRESSES: 'http://localhost:8082/api/v1/report/addresses',
    API_URL_GET_ADDRESSES_DETAILS: 'http://localhost:8082/api/v1/report/addresses/details',
    QUERY_PARAM_ADDRESSES: 'addresses',

    STATUS_ERROR: "Failed to fetch data",

    STATUS_ENTER_ADDRESSES: "Please enter an address list in the settings to start fetching data.",
    PLACEHOLDER_ADDRESS_LIST: "Host:port list in host:port format, separated by commas or new lines.\n# Comments and empty lines are allowed.",
    PLACEHOLDER_PORT_LIST: "Port list, for filtering addresses. Separated by commas or new lines.\n# Comments and empty lines are allowed.",

    BUTTON_EXPAND_ALL: "Expand All Details",
    BUTTON_COLLAPSE_ALL: "Collapse All Details",
    BUTTON_EXPAND_DETAILS: "Expand Details",
    BUTTON_COLLAPSE_DETAILS: "Collapse Details",

    CONFIG_KEY: {
        ADDRESSES_COOKIE: 'addresses',
        PORTS_COOKIE: 'ports',
        DEVIATION_THRESHOLDS: 'deviation_thresholds'
    },

    SELECTOR_DATA_KEY: 'data-key',
    SELECTOR_DATA_FULL_KEY: 'data-fullkey',

    REPORT_KEY_ROOT_PARENT: '',
    REPORT_KEY_ROOT: "Report",
    HIERARCHY_DELIMITER: '.',

    DEFAULT_DEVIATION_THRESHOLDS: {
        LOW: { value: 5 },
        MEDIUM: { value: 20 },
        HIGH: { value: 50 }
    },

    DEVIATION_THRESHOLDS: {},

    // Defaults
    initialize(savedConfig = {}) {
        this.MODE = savedConfig.MODE || this.MODE_PROD;

        this.DEVIATION_THRESHOLDS = {
            LOW: { value: savedConfig.DEVIATION_THRESHOLDS?.LOW || this.DEFAULT_DEVIATION_THRESHOLDS.LOW.value },
            MEDIUM: { value: savedConfig.DEVIATION_THRESHOLDS?.MEDIUM || this.DEFAULT_DEVIATION_THRESHOLDS.MEDIUM.value },
            HIGH: { value: savedConfig.DEVIATION_THRESHOLDS?.HIGH || this.DEFAULT_DEVIATION_THRESHOLDS.HIGH.value }
        };

        console.log('Constants initialized with:', {
            MODE: this.MODE,
            DEVIATION_THRESHOLDS: this.DEVIATION_THRESHOLDS
        });
    }
};

const savedConfig = {

    MODE: Constants.MODE_PROD,

    DEVIATION_THRESHOLDS: {
        LOW: 5,
        MEDIUM: 20
    }
};

Constants.initialize(savedConfig);
