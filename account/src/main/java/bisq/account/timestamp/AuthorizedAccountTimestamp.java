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

package bisq.account.timestamp;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.common.annotation.ExcludeForHash;
import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.PublishDateAware;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

import static bisq.network.p2p.services.data.storage.MetaData.DEFAULT_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_20_DAYS;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedAccountTimestamp implements AuthorizedDistributedData, PublishDateAware {
    private static final int VERSION = 1;

    public static final MetaData META_DATA = new MetaData(TTL_20_DAYS, DEFAULT_PRIORITY, AuthorizedAccountTimestamp.class.getSimpleName());
    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    private final AccountTimestamp accountTimestamp;

    @ExcludeForHash
    @EqualsAndHashCode.Exclude
    private final boolean staticPublicKeysProvided;

    @EqualsAndHashCode.Exclude
    @Setter
    private transient long publishDate;

    public AuthorizedAccountTimestamp(AccountTimestamp accountTimestamp,
                                      boolean staticPublicKeysProvided) {
        this(VERSION,
                accountTimestamp,
                staticPublicKeysProvided);
    }

    private AuthorizedAccountTimestamp(int version,
                                       AccountTimestamp accountTimestamp,
                                       boolean staticPublicKeysProvided) {
        this.version = version;
        this.accountTimestamp = accountTimestamp;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.account.protobuf.AuthorizedAccountTimestamp.Builder getBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AuthorizedAccountTimestamp.newBuilder()
                .setAccountTimestamp(accountTimestamp.toProto(serializeForHash))
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .setVersion(version);
    }

    @Override
    public bisq.account.protobuf.AuthorizedAccountTimestamp toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static AuthorizedAccountTimestamp fromProto(bisq.account.protobuf.AuthorizedAccountTimestamp proto)
            throws UnresolvableProtobufException {
        return new AuthorizedAccountTimestamp(
                proto.getVersion(),
                AccountTimestamp.fromProto(proto.getAccountTimestamp()),
                proto.getStaticPublicKeysProvided()
        );
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.account.protobuf.AuthorizedAccountTimestamp.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public MetaData getMetaData() {
        return META_DATA;
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }

    // Oracle nodes of earlier versions published it with the key of their network ID
    @Override
    public boolean isLegacyOwnerAccepted() {
        return true;
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
        return "AuthorizedAccountTimestamp{" +
                ",\r\n                    accountTimestamp=" + accountTimestamp +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided() +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}