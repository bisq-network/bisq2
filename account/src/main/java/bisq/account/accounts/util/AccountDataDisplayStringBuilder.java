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

package bisq.account.accounts.util;

import bisq.common.util.StringUtils;

public class AccountDataDisplayStringBuilder {
    StringBuilder stringBuilder = new StringBuilder();

    public AccountDataDisplayStringBuilder(String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Expected key-value pairs, but received an odd number of arguments: " + keyValuePairs.length);
        }
        for (int i = 0; i < keyValuePairs.length; i++) {
            String key = keyValuePairs[i];
            i++;
            String value = keyValuePairs[i];
            if (StringUtils.isNotEmpty(value)) {
                stringBuilder.append(key)
                        .append(": ")
                        .append(value);

                if (i < keyValuePairs.length - 1) {
                    stringBuilder.append("\n");
                }
            }
        }
    }

    public String toString() {
        // Remove potential trailing linebreak
        return stringBuilder.toString().replaceFirst("\\R?$", "");
    }

    public void add(String description, String value) {
        if (StringUtils.isNotEmpty(value)) {
            stringBuilder.append(description).append(": ")
                    .append(value).append("\n");
        }
    }
}
