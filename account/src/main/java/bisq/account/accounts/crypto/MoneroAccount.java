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

import bisq.account.accounts.AccountOrigin;
import bisq.account.timestamp.KeyAlgorithm;
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
public final class MoneroAccount extends CryptoAssetAccount<MoneroAccountPayload> {
    public MoneroAccount(String id,
                         long creationDate,
                         String accountName,
                         MoneroAccountPayload accountPayload,
                         KeyPair keyPair,
                         KeyAlgorithm keyAlgorithm,
                         AccountOrigin accountOrigin) {
        super(id, creationDate, accountName, accountPayload, keyPair, keyAlgorithm, accountOrigin);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        bisq.account.protobuf.CryptoAssetAccount.Builder builder = getCryptoAssetAccountBuilder(serializeForHash)
                .setMoneroAccount(getMoneroAccountBuilder(serializeForHash));
        bisq.account.protobuf.CryptoAssetAccount cryptoAssetAccount = resolveBuilder(builder, serializeForHash).build();
        return getAccountBuilder(serializeForHash)
                .setCryptoAssetAccount(cryptoAssetAccount);
    }

    private bisq.account.protobuf.MoneroAccount.Builder getMoneroAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.MoneroAccount.newBuilder();
    }

    public static MoneroAccount fromProto(bisq.account.protobuf.Account proto) {
        var cryptoAssetAccount = proto.getCryptoAssetAccount();
        var monero = cryptoAssetAccount.getMoneroAccount();
        KeyAlgorithm keyAlgorithm = KeyAlgorithm.fromProto(proto.getKeyAlgorithm());
        AccountOrigin accountOrigin = AccountOrigin.fromProto(proto.getAccountOrigin());
        KeyPair keyPair = KeyPairProtoUtil.fromProto(proto.getKeyPair(), keyAlgorithm.getAlgorithm());
        return new MoneroAccount(
                proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                MoneroAccountPayload.fromProto(proto.getAccountPayload()),
                keyPair,
                keyAlgorithm,
                accountOrigin
        );
    }
}
