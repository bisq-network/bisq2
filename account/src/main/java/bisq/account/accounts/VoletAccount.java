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

// Volet is ex-AdvancedCash

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class VoletAccount extends Account<VoletAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.VOLET);

    public VoletAccount(String accountName, VoletAccountPayload accountPayload) {
        super(accountName, PAYMENT_METHOD, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setVoletAccount(toVoletAccountProto(serializeForHash));
    }

    public static VoletAccount fromProto(bisq.account.protobuf.Account proto) {
        return new VoletAccount(
                proto.getAccountName(),
                VoletAccountPayload.fromProto(proto.getAccountPayload())
        );
    }

    private bisq.account.protobuf.VoletAccount toVoletAccountProto(boolean serializeForHash) {
        return resolveBuilder(getVoletAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.VoletAccount.Builder getVoletAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.VoletAccount.newBuilder();
    }
}