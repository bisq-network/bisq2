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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class TransferwiseAccount extends CountryBasedAccount<TransferwiseAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);

    public TransferwiseAccount(String accountName, TransferwiseAccountPayload payload, Country country) {
        super(accountName, PAYMENT_METHOD, payload, country);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash)
                .setTransferwiseAccount(toTransferwiseAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.TransferwiseAccount toTransferwiseAccountProto(boolean serializeForHash) {
        return resolveBuilder(getTransferwiseAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.TransferwiseAccount.Builder getTransferwiseAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.TransferwiseAccount.newBuilder();
    }

    public static TransferwiseAccount fromProto(bisq.account.protobuf.Account proto) {
        return new TransferwiseAccount(
                proto.getAccountName(),
                TransferwiseAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry())
        );
    }
}