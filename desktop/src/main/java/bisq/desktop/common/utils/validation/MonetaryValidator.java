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

package bisq.desktop.common.utils.validation;

import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

// todo better use another validation framework as in bisq 1
@Slf4j
public class MonetaryValidator extends InputValidator {
    public ValidationResult validate(String value) {
        ValidationResult result = super.validate(value);
        if (!result.isValid) {
            return result;
        }
        try {
            Double.parseDouble(value);
            return new ValidationResult(true);
        } catch (Throwable error) {
            return new ValidationResult(false, Res.common.get("validation.invalid"));
        }
    }
}