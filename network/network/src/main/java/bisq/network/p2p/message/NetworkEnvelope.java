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
import bisq.common.proto.UnresolvableProtobufException;
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

    // Size limit of a serialized envelope, without its length prefix. It is a protocol constant: a node closes the
    // connection when a peer announces a larger message, and it never sends one. The largest regular messages are
    // inventory messages: a request with a full hash set filter has at most about 3.0 MB, and a response of the default
    // size with a HASH_CASH_V2 token about 2 MB (see
    // docs/specifications/network/message-framing.md).
    public static final int MAX_SERIALIZED_SIZE = 4 * 1024 * 1024;
    // Size limit of the envelopes of the handshake. The node parses the first message of a peer before it checks
    // anything of the peer, and the parsed objects of a message can need about 20 times its size. The largest valid
    // handshake message is below 1.8 KB (see docs/specifications/network/message-framing.md).
    public static final int MAX_HANDSHAKE_SERIALIZED_SIZE = 64 * 1024;

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
        return unsafeToProto(serializeForHash);
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

    public static NetworkEnvelope fromProto(bisq.network.protobuf.NetworkEnvelope proto)
            throws UnresolvableProtobufException {
        try {
            return new NetworkEnvelope(proto.getVersion(),
                    AuthorizationToken.fromProto(proto.getAuthorizationToken()),
                    EnvelopePayloadMessage.fromProto(proto.getNetworkMessage()));
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
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