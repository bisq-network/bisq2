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

import bisq.account.settlement.FiatSettlement;
import bisq.common.locale.Country;
import bisq.common.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SepaAccount extends CountryBasedAccount<SepaAccountPayload, FiatSettlement> {
    private static final FiatSettlement SETTLEMENT = new FiatSettlement(FiatSettlement.Method.SEPA);

    public SepaAccount(String accountName,
                       String holderName,
                       String iban,
                       String bic,
                       Country country) {
        this(accountName,
                new SepaAccountPayload(StringUtils.createUid(), SETTLEMENT.getSettlementMethodName(), holderName, iban, bic, country.getCode()),
                country);
    }

    private SepaAccount(String accountName, SepaAccountPayload sepaAccountPayload, Country country) {
        super(accountName, SETTLEMENT, sepaAccountPayload, country);
    }

    @Override
    public bisq.account.protobuf.Account toProto() {
        return getAccountBuilder().setCountryBasedAccount(getCountryBasedAccountBuilder()
                        .setSepaAccount(bisq.account.protobuf.SepaAccount.newBuilder()))
                .build();
    }

    public static SepaAccount fromProto(bisq.account.protobuf.Account proto) {
        return new SepaAccount(proto.getAccountName(),
                SepaAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}