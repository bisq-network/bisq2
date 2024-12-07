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

export class Constants {
    static MODE_DEV = 'dev';
    static MODE_PROD = 'prod';

    static DEVIATION_THRESHOLDS_LOW = 5;
    static DEVIATION_THRESHOLDS_MEDIUM = 20;
    static DEVIATION_THRESHOLDS_HIGH = 50;

    static BUTTON_EXPAND_ALL = "Expand All Details";
    static BUTTON_COLLAPSE_ALL = "Collapse All Details";
    static BUTTON_EXPAND_DETAILS = "Expand Details";
    static BUTTON_COLLAPSE_DETAILS = "Collapse Details";

    static STATUS_ERROR = "Failed to fetch data";
    static STATUS_ENTER_ADDRESSES = "Please enter an address list in the settings to start fetching data.";

    static PLACEHOLDER_ADDRESS_LIST = "Host:port list in host:port format, separated by commas or new lines.\n# Comments and empty lines are allowed.";
    static PLACEHOLDER_PORT_LIST = "Port list, for filtering addresses. Separated by commas or new lines.\n# Comments and empty lines are allowed.";

    static CONFIG_KEY = Object.freeze({
        ADDRESSES: 'addresses',
        PORTS: 'ports',
        DEVIATION_THRESHOLDS: 'deviation_thresholds'
    });

    static REPORT_KEY_ROOT_PARENT = '';
    static REPORT_KEY_ROOT = "Report";
    static HIERARCHY_DELIMITER = '.';

    static SELECTOR_DATA_KEY = 'data-key';
    static SELECTOR_DATA_FULL_KEY = 'data-fullkey';
}
