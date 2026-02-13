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

package bisq.common.validation.fiat;

import bisq.common.util.StringUtils;
import bisq.common.validation.RegexValidation;
import lombok.Getter;

import java.util.regex.Pattern;

public class IFSCValidation implements RegexValidation {
    @Getter
    public final Pattern pattern = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    @Getter
    public final String i18nKey = "paymentAccounts.ifsc.invalid";
    private static IFSCValidation instance;

    public IFSCValidation() {
    }

    public static IFSCValidation getInstance() {
        if (instance == null) {
            instance = new IFSCValidation();
        }
        return instance;
    }

    @Override
    public boolean isValid(Pattern pattern, String input) {
        return StringUtils.isNotEmpty(input) && pattern.matcher(input).matches();
    }
}
