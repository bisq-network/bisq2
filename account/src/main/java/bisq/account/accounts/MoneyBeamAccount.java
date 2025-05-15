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
public final class MoneyBeamAccount extends Account<MoneyBeamAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.MONEY_BEAM);

    public MoneyBeamAccount(String accountName, MoneyBeamAccountPayload accountPayload) {
        super(accountName, PAYMENT_METHOD, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setMoneyBeamAccount(toMoneyBeamAccountProto(serializeForHash));
    }

    public static MoneyBeamAccount fromProto(bisq.account.protobuf.Account proto) {
        return new MoneyBeamAccount(
                proto.getAccountName(),
                MoneyBeamAccountPayload.fromProto(proto.getAccountPayload())
        );
    }

    private bisq.account.protobuf.MoneyBeamAccount toMoneyBeamAccountProto(boolean serializeForHash) {
        return resolveBuilder(getMoneyBeamAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.MoneyBeamAccount.Builder getMoneyBeamAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.MoneyBeamAccount.newBuilder();
    }
}