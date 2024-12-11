// js/utils/DOMUtils.js
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

/**
 * DOMUtils is a helper class for DOM manipulation and searching.
 */

import { KeyUtils } from './KeyUtils.js'

export class DOMUtils {

    static SELECTOR_DATA_FULL_KEY = 'data-fullkey';

    static findReportElementByAddress(address) {
        const nodeBlock = document.getElementById(KeyUtils.generateReportNodeId(address));
        if (!nodeBlock) {
            console.warn(`Report node for address "${address}" not found.`);
            return null;
        }
        return nodeBlock;
    }

    static findTableContainerInElementByFullKey(element, fullKey) {
        const selector = `div.table-container[data-fullkey="${fullKey}"]`;
        return DOMUtils.#findElementBySelectorAndOptionals(selector, element);
    }

    static findTableValueByAddressAndFullKey(address, fullKey) {
        const selector = `td[${DOMUtils.SELECTOR_DATA_FULL_KEY}="${fullKey}"]`;
        return DOMUtils.#findElementBySelectorAndOptionals(selector, undefined, address);
    }

    static #findElementBySelectorAndOptionals(selector, element = undefined, address = undefined) {
        let nodeBlock = element || (address && DOMUtils.findReportElementByAddress(address));
        if (!nodeBlock) {
            console.warn(`No valid node found for selector "${selector}", address "${address}".`);
            return null;
        }

        const subElement = nodeBlock.querySelector(selector);
        if (!subElement) {
            console.warn(`Element not found for selector "${subElement}" in node (address: "${address}").`);
        }
        return subElement;
    }

    static setDataFullKey(element, fullKey) {
        element.setAttribute(DOMUtils.SELECTOR_DATA_FULL_KEY, fullKey);
    }

    static getDataFullKey(element) {
        return element.getAttribute(DOMUtils.SELECTOR_DATA_FULL_KEY);
    }

    static findElementByKey(element, key) {
        return this.#findElement(element, DOMUtils.SELECTOR_DATA_KEY, key);
    }

    static findElementByFullKey(element, fullKey) {
        return this.#findElement(element, DOMUtils.SELECTOR_DATA_FULL_KEY, fullKey);
    }

    //////////////////////
    // PRIVATE METHODS
    //////////////////////

    static #findElement(element, attribute, value) {
        if (!element || typeof value !== 'string' || !value.trim()) {
            console.warn('Invalid input provided to #findElement');
            return null;
        }

        const selector = `[${attribute}="${value}"]`;
        const selectedElement = element.querySelector(selector);

        if (selectedElement) {
            console.log(`Found element with selector "${selector}" in parent node.`);
        } else {
            console.warn(`No element found with selector "${selector}" in parent node.`);
        }

        return selectedElement;
    }
}

