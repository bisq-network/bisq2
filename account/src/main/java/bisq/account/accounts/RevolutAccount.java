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
import bisq.common.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class RevolutAccount extends Account<RevolutAccountPayload, FiatPaymentMethod> {
    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.REVOLUT);

    public RevolutAccount(String accountName, String email) {
        this(accountName, new RevolutAccountPayload(StringUtils.createUid(), PAYMENT_METHOD.getName(), email));
    }

    private RevolutAccount(String accountName, RevolutAccountPayload revolutAccountPayload) {
        super(accountName, PAYMENT_METHOD, revolutAccountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setRevolutAccount(toRevolutAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.RevolutAccount toRevolutAccountProto(boolean serializeForHash) {
        return resolveBuilder(getRevolutAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.RevolutAccount.Builder getRevolutAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.RevolutAccount.newBuilder();
    }

    public static RevolutAccount fromProto(bisq.account.protobuf.Account proto) {
        return new RevolutAccount(proto.getAccountName(), RevolutAccountPayload.fromProto(proto.getAccountPayload()));
    }
}