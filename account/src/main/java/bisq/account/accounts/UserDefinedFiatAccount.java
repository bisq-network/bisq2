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
import bisq.common.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class UserDefinedFiatAccount extends Account<UserDefinedFiatAccountPayload, FiatPaymentMethod> {
    private static FiatPaymentMethod PAYMENT_METHOD;

    private static FiatPaymentMethod getPaymentMethod(String accountName) {
        if (PAYMENT_METHOD == null) {
            PAYMENT_METHOD = FiatPaymentMethod.fromCustomName(accountName);
        }
        return PAYMENT_METHOD;
    }

    public UserDefinedFiatAccount(String accountName, String accountData) {
        this(accountName, new UserDefinedFiatAccountPayload(StringUtils.createUid(), getPaymentMethod(accountName).getName(), accountData));
    }

    private UserDefinedFiatAccount(String accountName, UserDefinedFiatAccountPayload userDefinedFiatAccountPayload) {
        super(accountName, getPaymentMethod(accountName), userDefinedFiatAccountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setUserDefinedFiatAccount(toUserDefinedFiatAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.UserDefinedFiatAccount toUserDefinedFiatAccountProto(boolean serializeForHash) {
        return resolveBuilder(getUserDefinedFiatAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.UserDefinedFiatAccount.Builder getUserDefinedFiatAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.UserDefinedFiatAccount.newBuilder();
    }

    public static UserDefinedFiatAccount fromProto(bisq.account.protobuf.Account proto) {
        return new UserDefinedFiatAccount(proto.getAccountName(), UserDefinedFiatAccountPayload.fromProto(proto.getAccountPayload()));
    }
}