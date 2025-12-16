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

package bisq.account.age_witness;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.common.annotation.ExcludeForHash;
import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

import static bisq.network.p2p.services.data.storage.MetaData.DEFAULT_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_30_DAYS;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedAccountAgeWitness implements AuthorizedDistributedData {
    private static final int VERSION = 1;

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_30_DAYS, DEFAULT_PRIORITY, getClass().getSimpleName());
    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    private final AccountAgeWitness accountAgeWitness;

    @ExcludeForHash
    @EqualsAndHashCode.Exclude
    private final boolean staticPublicKeysProvided;

    public AuthorizedAccountAgeWitness(AccountAgeWitness accountAgeWitness,
                                       boolean staticPublicKeysProvided) {
        this(VERSION,
                accountAgeWitness,
                staticPublicKeysProvided);
    }

    private AuthorizedAccountAgeWitness(int version,
                                        AccountAgeWitness accountAgeWitness,
                                        boolean staticPublicKeysProvided) {
        this.version = version;
        this.accountAgeWitness = accountAgeWitness;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.account.protobuf.AuthorizedAccountAgeWitness.Builder getBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AuthorizedAccountAgeWitness.newBuilder()
                .setAccountAgeWitness(accountAgeWitness.toProto(serializeForHash))
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .setVersion(version);
    }

    @Override
    public bisq.account.protobuf.AuthorizedAccountAgeWitness toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static AuthorizedAccountAgeWitness fromProto(bisq.account.protobuf.AuthorizedAccountAgeWitness proto) {
        return new AuthorizedAccountAgeWitness(
                proto.getVersion(),
                AccountAgeWitness.fromProto(proto.getAccountAgeWitness()),
                proto.getStaticPublicKeysProvided()
        );
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.account.protobuf.AuthorizedAccountAgeWitness.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return 0.2;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return false;
    }

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        if (DevMode.isDevMode()) {
            return AuthorizedPubKeys.DEV_PUB_KEYS;
        } else {
            return AuthorizedPubKeys.ORACLE_NODE_PUB_KEYS;
        }
    }

    @Override
    public boolean staticPublicKeysProvided() {
        return staticPublicKeysProvided;
    }

    @Override
    public String toString() {
        return "AuthorizedAccountAgeWitness{" +
                ",\r\n                    accountAgeWitness=" + accountAgeWitness +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided() +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}