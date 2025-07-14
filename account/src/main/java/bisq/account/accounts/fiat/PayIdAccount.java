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

package bisq.account.accounts.fiat;

import bisq.account.accounts.Account;
import bisq.account.payment_method.FiatPaymentMethod;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class PayIdAccount extends Account<FiatPaymentMethod, PayIdAccountPayload> {
    public PayIdAccount(String id, long creationDate, String accountName, PayIdAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setPayIdAccount(toPayIdAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.PayIdAccount toPayIdAccountProto(boolean serializeForHash) {
        return resolveBuilder(getPayIdAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PayIdAccount.Builder getPayIdAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PayIdAccount.newBuilder();
    }

    public static PayIdAccount fromProto(bisq.account.protobuf.Account proto) {
        return new PayIdAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                PayIdAccountPayload.fromProto(proto.getAccountPayload())
        );
    }
}