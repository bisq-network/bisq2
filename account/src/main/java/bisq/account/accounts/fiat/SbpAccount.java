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

import bisq.account.accounts.AccountOrigin;
import bisq.account.protobuf.Account;
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
public final class SbpAccount extends CountryBasedAccount<SbpAccountPayload> {
    public SbpAccount(String id,
                      long creationDate,
                      String accountName,
                      SbpAccountPayload accountPayload,
                      KeyPair keyPair,
                      KeyAlgorithm keyAlgorithm,
                      AccountOrigin accountOrigin) {
        super(id, creationDate, accountName, accountPayload, keyPair, keyAlgorithm, accountOrigin);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setSbpAccount(
                toSbpAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.SbpAccount toSbpAccountProto(boolean serializeForHash) {
        return resolveBuilder(getSbpAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SbpAccount.Builder getSbpAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SbpAccount.newBuilder();
    }

    public static SbpAccount fromProto(Account proto) {
        KeyAlgorithm keyAlgorithm = KeyAlgorithm.fromProto(proto.getKeyAlgorithm());
        AccountOrigin accountOrigin = AccountOrigin.fromProto(proto.getAccountOrigin());
        return new SbpAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                SbpAccountPayload.fromProto(proto.getAccountPayload()),
                KeyPairProtoUtil.fromProto(proto.getKeyPair(), keyAlgorithm.getAlgorithm()),
                keyAlgorithm,
                accountOrigin);
    }
}
