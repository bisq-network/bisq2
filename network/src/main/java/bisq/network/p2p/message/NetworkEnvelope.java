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

package bisq.network.p2p.message;

import bisq.common.proto.Proto;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Outside data structure to be sent over the wire.
 */
@ToString
@EqualsAndHashCode
@Getter
@Slf4j
public final class NetworkEnvelope implements Proto {
    public static final int VERSION = 1;

    private final int version;
    private final AuthorizationToken authorizationToken;
    private final NetworkMessage networkMessage;

    public NetworkEnvelope(int version, AuthorizationToken authorizationToken, NetworkMessage networkMessage) {
        this.version = version;
        this.authorizationToken = authorizationToken;
        this.networkMessage = networkMessage;
    }

    public bisq.network.protobuf.NetworkEnvelope toProto() {
        return bisq.network.protobuf.NetworkEnvelope.newBuilder()
                .setVersion(version)
                .setAuthorizationToken(authorizationToken.toProto())
                .setNetworkMessage(networkMessage.toProto())
                .build();
    }

    public static NetworkEnvelope fromProto(bisq.network.protobuf.NetworkEnvelope proto) {
        return new NetworkEnvelope(proto.getVersion(),
                AuthorizationToken.fromProto(proto.getAuthorizationToken()),
                NetworkMessage.fromProto(proto.getNetworkMessage()));
    }
}