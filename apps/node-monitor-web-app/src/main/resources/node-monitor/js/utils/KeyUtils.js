// js/utils/KeyUtils.js
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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

import { Constants } from '../Constants.js';

/**
 * KeyUtils is a helper class that provides methods for working with nested keys and object data.
 */
export class KeyUtils {

    static generateReportNodeId(address) {
        return `report-${address.replace(/:/g, '-').replace(/\./g, '_')}`;
    }

    static extractAddressFromReportNodeId(reportNodeId) {
        if (!reportNodeId.startsWith('report-')) {
            throw new Error(`Invalid report node ID: ${reportNodeId}`);
        }

        return reportNodeId
            .replace('report-', '')
            .replace(/-/g, ':')
            .replace(/_/g, '.');
    }

    static createFullKey(parentFullKey, currentKey) {
        const fullKey = parentFullKey ? `${parentFullKey}.${currentKey}` : currentKey;
        return fullKey;
    }

    static getParentKey(fullKey) {
        return fullKey.substring(0, fullKey.lastIndexOf(Constants.HIERARCHY_DELIMITER));
    }

    static getNestedValue(obj, fullKey, defaultValue = undefined) {
        const keys = fullKey.split(Constants.HIERARCHY_DELIMITER);
        let current = obj;

        for (const key of keys) {
            if (current && key in current) {
                current = current[key];
            } else {
                return defaultValue;
            }
        }

        return current;
    }

    static setNestedValue(obj, fullKey, value) {
        const keys = fullKey.split(Constants.HIERARCHY_DELIMITER);
        keys.reduce((acc, key, index) => {
            if (acc === null || acc === undefined) {
                throw new TypeError(`Cannot set value for "${fullKey}". Encountered null or undefined at key "${key}".`);
            }

            if (index === keys.length - 1) {
                acc[key] = value;
            } else {
                if (!(key in acc)) {
                    acc[key] = {};
                }
                return acc[key];
            }
        }, obj);
    }
}

