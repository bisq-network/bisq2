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
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SepaAccount extends CountryBasedAccount<SepaAccountPayload> {
    public SepaAccount(String id,
                       long creationDate,
                       String accountName,
                       SepaAccountPayload accountPayload,
                       KeyPair keyPair,
                       String keyAlgorithm) {
        super(id, creationDate, accountName, accountPayload, keyPair, keyAlgorithm);
    }

    public SepaAccount(String id,
                       long creationDate,
                       String accountName,
                       SepaAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setSepaAccount(
                toSepaAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.SepaAccount toSepaAccountProto(boolean serializeForHash) {
        return resolveBuilder(getSepaAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SepaAccount.Builder getSepaAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SepaAccount.newBuilder();
    }

    public static SepaAccount fromProto(bisq.account.protobuf.Account proto) {
        String keyAlgorithm = proto.getKeyAlgorithm();
        return new SepaAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                SepaAccountPayload.fromProto(proto.getAccountPayload()),
                KeyPairProtoUtil.fromProto(proto.getKeyPair(), keyAlgorithm),
                keyAlgorithm);
    }
}