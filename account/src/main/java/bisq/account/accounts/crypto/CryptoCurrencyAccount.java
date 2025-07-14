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

package bisq.account.accounts.crypto;

import bisq.account.accounts.Account;
import bisq.account.payment_method.CryptoPaymentMethod;
import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public abstract class CryptoCurrencyAccount<P extends CryptoCurrencyAccountPayload> extends Account<CryptoPaymentMethod, P> {
    public CryptoCurrencyAccount(String id,
                                 long creationDate,
                                 String accountName,
                                 P accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }


    protected bisq.account.protobuf.CryptoCurrencyAccount toCryptoCurrencyAccountProto(boolean serializeForHash) {
        return resolveBuilder(getCryptoCurrencyAccountBuilder(serializeForHash), serializeForHash).build();
    }

    protected bisq.account.protobuf.CryptoCurrencyAccount.Builder getCryptoCurrencyAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.CryptoCurrencyAccount.newBuilder();
    }

    public static CryptoCurrencyAccount<?> fromProto(bisq.account.protobuf.Account proto) {
        return switch (proto.getCryptoCurrencyAccount().getMessageCase()) {
            case MONEROACCOUNT -> MoneroAccount.fromProto(proto);
            case OTHERCRYPTOCURRENCYACCOUNT -> OtherCryptoCurrencyAccount.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}