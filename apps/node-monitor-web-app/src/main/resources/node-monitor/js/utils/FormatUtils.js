// js/utils/FormatUtils.js

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

export class FormatUtils {

    static formatNumber(value, decimalPlaces = 2) {
        if (typeof value !== 'number' || isNaN(value)) {
            throw new Error('Invalid value: The input must be a valid number.');
        }

        return Number.isInteger(value) ? value.toString() : value.toFixed(decimalPlaces);
    }
}
