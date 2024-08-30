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

import bisq.common.proto.NetworkProto;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static bisq.network.p2p.node.ConnectionException.Reason.INVALID_NETWORK_VERSION;

/**
 * Outside data structure to be sent over the wire.
 */
@ToString
@EqualsAndHashCode
@Getter
@Slf4j
public final class NetworkEnvelope implements NetworkProto {
    // For live network we use networkVersion=1
    // For dev testing networkVersion=0
    @Setter
    public static int networkVersion;

    private final int version;
    private final AuthorizationToken authorizationToken;
    private final EnvelopePayloadMessage envelopePayloadMessage;

    public NetworkEnvelope(AuthorizationToken authorizationToken, EnvelopePayloadMessage envelopePayloadMessage) {
        this(networkVersion, authorizationToken, envelopePayloadMessage);
    }

    public NetworkEnvelope(int version, AuthorizationToken authorizationToken, EnvelopePayloadMessage envelopePayloadMessage) {
        this.version = version;
        this.authorizationToken = authorizationToken;
        this.envelopePayloadMessage = envelopePayloadMessage;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.NetworkEnvelope toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.NetworkEnvelope completeProto() {
        return toProto(false);
    }

    @Override
    public bisq.network.protobuf.NetworkEnvelope.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.NetworkEnvelope.newBuilder()
                .setVersion(version)
                .setAuthorizationToken(authorizationToken.toProto(serializeForHash))
                .setNetworkMessage(envelopePayloadMessage.toProto(serializeForHash));
    }

    public static NetworkEnvelope fromProto(bisq.network.protobuf.NetworkEnvelope proto) {
        return new NetworkEnvelope(proto.getVersion(),
                AuthorizationToken.fromProto(proto.getAuthorizationToken()),
                EnvelopePayloadMessage.fromProto(proto.getNetworkMessage()));
    }

    public void verifyVersion() throws ConnectionException {
        if (version != networkVersion) {
            throw new ConnectionException(INVALID_NETWORK_VERSION, "Invalid networkEnvelopeVersion. " +
                    "version=" + version +
                    "; networkVersion=" + networkVersion +
                    "; networkMessage=" + envelopePayloadMessage.getClass().getSimpleName());
        }
    }

}