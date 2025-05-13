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
public final class SwishAccount extends Account<SwishAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SWISH);

    public SwishAccount(String accountName, SwishAccountPayload accountPayload) {
        super(accountName, PAYMENT_METHOD, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setSwishAccount(toSwishAccountProto(serializeForHash));
    }

    public static SwishAccount fromProto(bisq.account.protobuf.Account proto) {
        return new SwishAccount(
                proto.getAccountName(),
                SwishAccountPayload.fromProto(proto.getAccountPayload())
        );
    }

    private bisq.account.protobuf.SwishAccount toSwishAccountProto(boolean serializeForHash) {
        return resolveBuilder(getSwishAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SwishAccount.Builder getSwishAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SwishAccount.newBuilder();
    }
}