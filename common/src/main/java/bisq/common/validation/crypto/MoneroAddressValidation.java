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

package bisq.common.validation.crypto;

import bisq.common.validation.Validation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MoneroAddressValidation implements Validation {
    @Getter
    public final String i18nKey = "validation.address.invalid";

    private final Set<Long> validPrefixes = Set.of(18L, 19L, 42L);
    private static MoneroAddressValidation instance;

    public static MoneroAddressValidation getInstance() {
        if (instance == null) {
            instance = new MoneroAddressValidation();
        }
        return instance;
    }

    public boolean isValid(String address) {
        try {
            long prefix = MoneroAddressUtil.MoneroBase58.decodeAddress(address);
            return validPrefixes.contains(prefix);
        } catch (Exception e) {
            log.debug("Monero address is invalid", e);
            return false;
        }
    }

}
