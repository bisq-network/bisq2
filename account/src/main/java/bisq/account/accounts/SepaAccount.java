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

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
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
public final class SepaAccount extends CountryBasedAccount<SepaAccountPayload, FiatPaymentMethod> {
    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);

    public SepaAccount(String accountName,
                       String holderName,
                       String iban,
                       String bic,
                       Country country) {
        this(accountName,
                new SepaAccountPayload(StringUtils.createUid(), PAYMENT_METHOD.getName(), holderName, iban, bic, country.getCode()),
                country);
    }

    private SepaAccount(String accountName, SepaAccountPayload sepaAccountPayload, Country country) {
        super(accountName, PAYMENT_METHOD, sepaAccountPayload, country);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setSepaAccount(
                toSepaAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.SepaAccount toSepaAccountProto(boolean serializeForHash) {
        return resolveBuilder(getSepaAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SepaAccount.Builder getSepaAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SepaAccount.newBuilder();
    }

    public static SepaAccount fromProto(bisq.account.protobuf.Account proto) {
        return new SepaAccount(proto.getAccountName(),
                SepaAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}