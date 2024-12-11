// js/controllers/ReportDiffsController.js

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
import { DOMUtils } from '../utils/DOMUtils.js';
import { KeyUtils } from '../utils/KeyUtils.js';
import { FormatUtils } from '../utils/FormatUtils.js';

const VALUE_GT_REFERENCE_VALUE = '>';
const VALUE_LT_REFERENCE_VALUE = '<';
const THRESHOLD_CONFIG = {
    'numConnections': { direction: VALUE_LT_REFERENCE_VALUE, referenceValue: 10 },
    'memoryUsed': { direction: VALUE_GT_REFERENCE_VALUE, referenceValue: 500 },
    'numThreads': { direction: VALUE_GT_REFERENCE_VALUE, referenceValue: 70 },
    'nodeLoad': { direction: VALUE_GT_REFERENCE_VALUE, referenceValue: 0.4 }
};

export class ReportDiffsController {

    constructor() {
        this.reports = [];
        this.averageReport = {};
        this.relevantKeys = new Set();
        this.isRendering = false;
        this.pendingRender = false;
    }

    reset() {
        this.reports = [];
        this.averageReport = {};
        this.relevantKeys.clear();
        this.isRendering = false;
        this.pendingRender = false;
    }

    addReport(data, address) {
        if (!data.success) {
            console.warn(`Report for ${address} was not successful and will be ignored.`);
            return;
        }

        this.reports.push({ address, data: data.report });
        this.#updateAverageAndRelevantKeys(data.report, Constants.REPORT_KEY_ROOT_PARENT);
    }

    scheduleRender() {
        if (this.isRendering) {
            this.pendingRender = true;
            return;
        }

        this.isRendering = true;
        setTimeout(() => {
            this.#renderFieldDiffs();
            this.isRendering = false;

            if (this.pendingRender) {
                this.pendingRender = false;
                this.scheduleRender();
            }
        }, 0);
    }

    #updateAverageAndRelevantKeys(report, parentFullKey = Config.REPORT_KEY_ROOT_PARENT) {
        Object.entries(report).forEach(([key, value]) => {
            const fullKey = KeyUtils.createFullKey(parentFullKey, key);

            if (typeof value === 'number') {
                this.#updateAverageForField(value, fullKey);
            } else if (typeof value === 'object' && value !== null) {
                this.#updateAverageAndRelevantKeys(value, fullKey);
            }
        });
    }

    #updateAverageForField(value, fullKey) {
        let currentAverageData = KeyUtils.getNestedValue(this.averageReport, fullKey);

        if (!currentAverageData) {
            currentAverageData = { average: 0, count: 0 };
            KeyUtils.setNestedValue(this.averageReport, fullKey, currentAverageData);
        }

        currentAverageData.count += 1;
        currentAverageData.average += (value - currentAverageData.average) / currentAverageData.count;

        const newAverage = currentAverageData.average;
        const deviation = Math.abs((value - newAverage) / newAverage) * 100;
        if (deviation >= Config.DEVIATION_THRESHOLDS.LOW) {
            this.relevantKeys.add(fullKey);
        }
        return deviation;
    }

    #renderFieldDiffs() {
        this.reports.forEach(({ address, data }) => {
            const maxDeviationPerParent = new Map();

            this.relevantKeys.forEach(fullKey => {
                const value = KeyUtils.getNestedValue(data, fullKey);
                let deviation = 0;
                let hover = ``;

                if (THRESHOLD_CONFIG[fullKey]) {
                    const { direction, referenceValue } = THRESHOLD_CONFIG[fullKey];
                    deviation = this.#calculateCustomDeviation(value, referenceValue, direction);
                    if (deviation == 0) {
                        return;
                    }
                    hover = `Deviation: ${FormatUtils.formatNumber(deviation)}% from reference value (${FormatUtils.formatNumber(referenceValue)})`;
                    this.#renderField(address, fullKey, deviation, hover);
                } else {
                    const averageData = KeyUtils.getNestedValue(this.averageReport, fullKey);
                    if (!averageData || averageData.average == undefined) {
                        console.error(`Average Value not found or null: Key: ${fullKey}`);
                    } else if (value != undefined) {
                        deviation = Math.abs((value - averageData.average) / averageData.average * 100);
                        hover = `Deviation: ${FormatUtils.formatNumber(deviation)}% from average (${FormatUtils.formatNumber(averageData.average)})`;
                        this.#renderField(address, fullKey, deviation, hover);

                        const parentKey = KeyUtils.getParentKey(fullKey);
                        if (parentKey === Constants.REPORT_KEY_ROOT_PARENT) {
                            return;
                        }
                        if (!maxDeviationPerParent.has(parentKey) || deviation > maxDeviationPerParent.get(parentKey)) {
                            maxDeviationPerParent.set(parentKey, deviation);
                        }
                    }
                }
            });

            maxDeviationPerParent.forEach((maxDeviation, parentKey) => {
                this.#renderField(address, parentKey, maxDeviation, `Max Deviation: ${maxDeviation.toFixed(2)}%`);
            });
        });
    }

    #renderField(address, fullKey, deviation, title) {
        const element = DOMUtils.findTableValueByAddressAndFullKey(address, fullKey);
        if (!element) {
            console.error(`Element not found: Key: ${fullKey}, Address: ${address}`);
            return;
        }

        element.title = title;

        element.classList.remove('low', 'medium', 'high', 'diff');
        if (deviation >= Config.DEVIATION_THRESHOLDS.HIGH) {
            element.classList.add('diff', 'high');
        } else if (deviation >= Config.DEVIATION_THRESHOLDS.MEDIUM) {
            element.classList.add('diff', 'medium');
        } else if (deviation >= Config.DEVIATION_THRESHOLDS.LOW) {
            element.classList.add('diff', 'low');
        }
    }

    #calculateCustomDeviation(value, referenceValue, direction) {
        if (direction === VALUE_LT_REFERENCE_VALUE) {
            return value < referenceValue ? ((referenceValue - value) / referenceValue) * 100 : 0;
        } else if (direction === VALUE_GT_REFERENCE_VALUE) {
            return value > referenceValue ? ((value - referenceValue) / referenceValue) * 100 : 0;
        }
        return 0;
    }
}