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

import bisq.account.payment.FiatPayment;
import bisq.common.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class UserDefinedFiatAccount extends Account<UserDefinedFiatAccountPayload, FiatPayment> {
    private static final FiatPayment PAYMENT = new FiatPayment(FiatPayment.Method.USER_DEFINED);

    public UserDefinedFiatAccount(String accountName, String accountData) {
        this(accountName, new UserDefinedFiatAccountPayload(StringUtils.createUid(), PAYMENT.getPaymentMethodName(), accountData));
    }

    private UserDefinedFiatAccount(String accountName, UserDefinedFiatAccountPayload userDefinedFiatAccountPayload) {
        super(accountName, PAYMENT, userDefinedFiatAccountPayload);
    }

    @Override
    public bisq.account.protobuf.Account toProto() {
        return getAccountBuilder().setUserDefinedFiatAccount(bisq.account.protobuf.UserDefinedFiatAccount.newBuilder()).build();
    }

    public static UserDefinedFiatAccount fromProto(bisq.account.protobuf.Account proto) {
        return new UserDefinedFiatAccount(proto.getAccountName(), UserDefinedFiatAccountPayload.fromProto(proto.getAccountPayload()));
    }
}