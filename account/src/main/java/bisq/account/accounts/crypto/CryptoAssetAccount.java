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
import bisq.account.accounts.AccountOrigin;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.timestamp.KeyAlgorithm;
import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public abstract class CryptoAssetAccount<P extends CryptoAssetAccountPayload> extends Account<CryptoPaymentMethod, P> {
    public CryptoAssetAccount(String id,
                              long creationDate,
                              String accountName,
                              P accountPayload,
                              KeyPair keyPair,
                              KeyAlgorithm keyAlgorithm,
                              AccountOrigin accountOrigin) {
        super(id, creationDate, accountName, accountPayload, keyPair, keyAlgorithm, accountOrigin);
    }


    protected bisq.account.protobuf.CryptoAssetAccount toCryptoAssetAccountProto(boolean serializeForHash) {
        return resolveBuilder(getCryptoAssetAccountBuilder(serializeForHash), serializeForHash).build();
    }

    protected bisq.account.protobuf.CryptoAssetAccount.Builder getCryptoAssetAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.CryptoAssetAccount.newBuilder();
    }

    public static CryptoAssetAccount<?> fromProto(bisq.account.protobuf.Account proto) {
        return switch (proto.getCryptoAssetAccount().getMessageCase()) {
            case MONEROACCOUNT -> MoneroAccount.fromProto(proto);
            case OTHERCRYPTOASSETACCOUNT -> OtherCryptoAssetAccount.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}
