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

package bisq.oracle.daobridge.model;

import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.NetworkId;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@EqualsAndHashCode
@Getter
@ToString
public final class AuthorizedDaoBridgeServiceProvider implements AuthorizedDistributedData {
    // The pubKeys which are authorized for publishing that data.
    // todo Production key not set yet - we use devMode key only yet
    private static final Set<String> authorizedPublicKeys = Set.of();

    private final MetaData metaData = new MetaData(TimeUnit.DAYS.toMillis(10),
            100000,
            AuthorizedDaoBridgeServiceProvider.class.getSimpleName());

    private final NetworkId networkId;

    public AuthorizedDaoBridgeServiceProvider(NetworkId networkId) {
        this.networkId = networkId;
    }

    @Override
    public bisq.oracle.protobuf.AuthorizedDaoBridgeServiceProvider toProto() {
        return bisq.oracle.protobuf.AuthorizedDaoBridgeServiceProvider.newBuilder()
                .setNetworkId(networkId.toProto())
                .build();
    }

    public static AuthorizedDaoBridgeServiceProvider fromProto(bisq.oracle.protobuf.AuthorizedDaoBridgeServiceProvider proto) {
        return new AuthorizedDaoBridgeServiceProvider(NetworkId.fromProto(proto.getNetworkId()));
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.oracle.protobuf.AuthorizedDaoBridgeServiceProvider.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public Set<String> getAuthorizedPublicKeys() {
        if (DevMode.isDevMode()) {
            return Set.of(
                    "3056301006072a8648ce3d020106052b8104000a03420004170a828efbaa0316b7a59ec5a1e8033ca4c215b5e58b17b16f3e3cbfa5ec085f4bdb660c7b766ec5ba92b432265ba3ed3689c5d87118fbebe19e92b9228aca63"
            );
        } else {
            return authorizedPublicKeys;
        }
    }
}