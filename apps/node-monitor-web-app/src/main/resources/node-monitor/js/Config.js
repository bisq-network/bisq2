// js/Config.js

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

import { Constants } from './Constants.js';

export class Config {
    static MODE = Constants.MODE_PROD;

    static API_URL_GET_REPORT = null;
    static API_URL_GET_ADDRESSES = null;
    static API_URL_POST_ADDRESSES_DETAILS = null;

    static DEVIATION_THRESHOLDS = {
        LOW: Constants.DEVIATION_THRESHOLDS_LOW,
        MEDIUM: Constants.DEVIATION_THRESHOLDS_MEDIUM,
        HIGH: Constants.DEVIATION_THRESHOLDS_HIGH
    };

    static initialize(savedConfig = {}) {
        const validateThreshold = (value, defaultValue) => {
            return typeof value === 'number' && value > 0 ? value : defaultValue;
        };

        Config.MODE = savedConfig.MODE || Config.MODE;

        Config.DEVIATION_THRESHOLDS = Object.freeze({
            LOW: validateThreshold(savedConfig.DEVIATION_THRESHOLDS?.LOW, Constants.DEVIATION_THRESHOLDS_LOW),
            MEDIUM: validateThreshold(savedConfig.DEVIATION_THRESHOLDS?.MEDIUM, Constants.DEVIATION_THRESHOLDS_MEDIUM),
            HIGH: validateThreshold(savedConfig.DEVIATION_THRESHOLDS?.HIGH, Constants.DEVIATION_THRESHOLDS_HIGH)
        });

        const { protocol, hostname, port } = window.location;
        const baseURL = `${protocol}//${hostname}${port ? `:${port}` : ''}`;

        Config.API_URL_GET_REPORT = `${baseURL}/api/v1/report`;
        Config.API_URL_GET_ADDRESSES = `${baseURL}/api/v1/report/addresses`;
        Config.API_URL_POST_ADDRESSES_DETAILS = `${baseURL}/api/v1/report/addresses/details`;
    }
}
