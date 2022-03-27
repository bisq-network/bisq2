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

import bisq.account.settlement.FiatSettlementMethod;
import bisq.common.locale.Country;
import com.google.protobuf.Message;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SepaAccount extends CountryBasedAccount<FiatSettlementMethod> {
    private static final FiatSettlementMethod METHOD = FiatSettlementMethod.SEPA;

    public SepaAccount(String accountName,
                       String holderName,
                       String iban,
                       String bic,
                       Country country) {
        super(accountName,
                METHOD,
                new SepaAccountPayload(METHOD.name(), holderName, iban, bic, country.code()),
                FiatSettlementMethod.getTradeCurrencies(METHOD),
                country);
    }

    @Override
    public Message toProto() {
        return null;
    }
}