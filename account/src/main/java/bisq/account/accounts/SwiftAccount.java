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
public final class SwiftAccount extends Account<SwiftAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SWIFT);

    public SwiftAccount(long creationDate, String accountName, SwiftAccountPayload accountPayload) {
        super(creationDate, accountName, PAYMENT_METHOD, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setSwiftAccount(toSwiftAccountProto(serializeForHash));
    }

    public static SwiftAccount fromProto(bisq.account.protobuf.Account proto) {
        return new SwiftAccount(
                proto.getCreationDate(),
                proto.getAccountName(),
                SwiftAccountPayload.fromProto(proto.getAccountPayload())
        );
    }

    private bisq.account.protobuf.SwiftAccount toSwiftAccountProto(boolean serializeForHash) {
        return resolveBuilder(getSwiftAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SwiftAccount.Builder getSwiftAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SwiftAccount.newBuilder();
    }
}