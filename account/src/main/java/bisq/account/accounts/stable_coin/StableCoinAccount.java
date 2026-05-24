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

package bisq.account.accounts.stable_coin;

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountOrigin;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethod;
import bisq.account.timestamp.KeyType;
import bisq.security.keys.KeyPairProtoUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class StableCoinAccount extends Account<StableCoinPaymentMethod, StableCoinAccountPayload> {

    public StableCoinAccount(String id,
                             long creationDate,
                             String accountName,
                             StableCoinAccountPayload accountPayload,
                             KeyPair keyPair,
                             KeyType keyType,
                             AccountOrigin accountOrigin) {
        super(id, creationDate, accountName, accountPayload, keyPair, keyType, accountOrigin);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setStableCoinAccount(
                        bisq.account.protobuf.StableCoinAccount.newBuilder());
    }

    public static StableCoinAccount fromProto(bisq.account.protobuf.Account proto) {
        KeyType keyType = KeyType.fromProto(proto.getKeyType());
        AccountOrigin accountOrigin = AccountOrigin.fromProto(proto.getAccountOrigin());
        KeyPair keyPair = KeyPairProtoUtil.fromProto(proto.getKeyPair(), keyType.getKeyAlgorithm());
        return new StableCoinAccount(
                proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                StableCoinAccountPayload.fromProto(proto.getAccountPayload()),
                keyPair,
                keyType,
                accountOrigin);
    }
}
