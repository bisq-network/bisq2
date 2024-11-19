// js/services/StorageService.js

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

export class StorageService {
    constructor() {
        this.addressesKey = Constants.CONFIG_KEY.ADDRESSES_COOKIE;
        this.portsKey = Constants.CONFIG_KEY.PORTS_COOKIE;
        this.thresholdsKey = Constants.CONFIG_KEY.DEVIATION_THRESHOLDS;
    }

    saveAddressesAndPorts(addressesInput, portsInput) {
        try {
            const addressList = this.#parseAddressListInput(addressesInput);
            const portList = this.#parsePortListInput(portsInput);

            this.#setAddressesCookie(addressesInput);
            this.#setPortsCookie(portsInput);
        } catch (error) {
            throw new Error("Address and filter-port lists not stored: " + error.message);
        }
    }

    saveAddresses(addressesInput) {
        try {
            const addressList = this.#parseAddressListInput(addressesInput);

            this.#setAddressesCookie(addressesInput);
        } catch (error) {
            throw new Error("Address list not stored: " + error.message);
        }
    }

    getAddressesFromCookie() {
        const addressesText = this.getAddressesCookie();
        const uniqueAddresses = new Set();

        addressesText.split(/\r?\n/).forEach(line => {
            line = line.trim();
            if (line && !line.startsWith('#')) {
                line.split(',').forEach(address => {
                    address = address.trim();
                    if (address) uniqueAddresses.add(address);
                });
            }
        });

        return Array.from(uniqueAddresses);
    }

    getPortsFromCookie() {
        const portsText = this.getPortsCookie();
        const uniquePorts = new Set();

        portsText.split(/\r?\n/).forEach(line => {
            line = line.trim();
            if (line && !line.startsWith('#')) {
                line.split(',').forEach(port => {
                    port = port.trim();
                    if (port && !isNaN(port)) uniquePorts.add(Number(port));
                });
            }
        });

        return Array.from(uniquePorts);
    }

    getAddressesCookie() {
        return this.#getCookie(this.addressesKey) || "";
    }

    getPortsCookie() {
        return this.#getCookie(this.portsKey) || "";
    }

    saveDeviationThresholds(thresholds) {
        this.#setCookie(this.thresholdsKey, JSON.stringify(thresholds));
    }

    getDeviationThresholds() {
        const thresholdsText = this.#getCookie(this.thresholdsKey);
        return thresholdsText ? JSON.parse(thresholdsText) : null;
    }

    //////////////////////
    // PRIVATE METHODS
    //////////////////////

    #getCookie(name) {
        const decodedCookie = decodeURIComponent(document.cookie);
        const cookiesArray = decodedCookie.split(';');
        for (let cookie of cookiesArray) {
            cookie = cookie.trim();
            if (cookie.startsWith(name + "=")) {
                return cookie.substring(name.length + 1);
            }
        }
        return null;
    }

    #parseAddressListInput(input) {
        return input
            .split(/\r?\n|,/)
            .map(line => line.trim())
            .filter(line => line && !line.startsWith('#'))
            .map(item => {
                if (!this.#isValidAddressFormat(item)) {
                    throw new Error(`Invalid address entry detected: "${item}"`);
                }
                return item;
            });
    };

    #isValidAddressFormat = (address) => {
        const addressPattern = /^[a-zA-Z0-9.-]+:\d+$/;
        return addressPattern.test(address);
    };

    #parsePortListInput(input) {
        return input
            .split(/\r?\n|,/)
            .map(line => line.trim())
            .filter(line => line && !line.startsWith('#'))
            .map(item => {
                const port = Number(item);
                if (isNaN(port) || port <= 0 || port > 65535) {
                    throw new Error(`Invalid or out-of-range port detected: "${item}"`);
                }
                return port;
            });
    };

    #setCookie(key, text) {
        const date = new Date();
        date.setTime(date.getTime() + (365 * 24 * 60 * 60 * 1000));
        const expires = "expires=" + date.toUTCString();
        document.cookie = `${key}=${encodeURIComponent(text)}; ${expires}; path=/; SameSite=None; Secure`;
    }

    #setAddressesCookie(addressesText) {
        this.#setCookie(this.addressesKey, addressesText);
    }

    #setPortsCookie(portsText) {
        this.#setCookie(this.portsKey, portsText);
    }
};

