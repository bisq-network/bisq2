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

package bisq.account.accounts;

import bisq.account.settlement.Settlement;
import bisq.common.locale.Country;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public abstract class CountryBasedAccount<P extends CountryBasedAccountPayload, T extends Settlement<?>> extends Account<P, T> {
    protected final Country country;

    public CountryBasedAccount(String accountName,
                               T settlement,
                               P payload,
                               Country country) {
        super(accountName, settlement, payload);
        this.country = country;
    }

    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder() {
        return bisq.account.protobuf.CountryBasedAccount.newBuilder()
                .setCountry(country.toProto());
    }
}