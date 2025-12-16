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

import bisq.security.keys.KeyPairProtoUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Getter
@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ZelleAccount extends CountryBasedAccount<ZelleAccountPayload> {
    public ZelleAccount(String id,
                        long creationDate,
                        String accountName,
                        ZelleAccountPayload accountPayload,
                        KeyPair keyPair,
                        String keyAlgorithm) {
        super(id, creationDate, accountName, accountPayload, keyPair, keyAlgorithm);
    }

    public ZelleAccount(String id, long creationDate, String accountName, ZelleAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setZelleAccount(toZelleAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.ZelleAccount toZelleAccountProto(boolean serializeForHash) {
        return resolveBuilder(getZelleAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.ZelleAccount.Builder getZelleAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.ZelleAccount.newBuilder();
    }

    public static ZelleAccount fromProto(bisq.account.protobuf.Account proto) {
        String keyAlgorithm = proto.getKeyAlgorithm();
        return new ZelleAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                ZelleAccountPayload.fromProto(proto.getAccountPayload()),
                KeyPairProtoUtil.fromProto(proto.getKeyPair(), keyAlgorithm),
                keyAlgorithm);
    }
}
