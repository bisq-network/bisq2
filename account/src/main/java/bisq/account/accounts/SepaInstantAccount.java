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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SepaInstantAccount extends CountryBasedAccount<SepaInstantAccountPayload> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA_INSTANT);

    public SepaInstantAccount(String accountName,
                              SepaInstantAccountPayload accountPayload) {
        super(accountName, accountPayload);
    }

    public static SepaInstantAccount fromProto(bisq.account.protobuf.Account proto) {
        return new SepaInstantAccount(
                proto.getAccountName(),
                SepaInstantAccountPayload.fromProto(proto.getAccountPayload()));
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash)
                .setSepaInstantAccount(toSepaInstantAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.SepaInstantAccount toSepaInstantAccountProto(boolean serializeForHash) {
        return resolveBuilder(getSepaInstantAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SepaInstantAccount.Builder getSepaInstantAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SepaInstantAccount.newBuilder();
    }
}