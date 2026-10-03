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

package bisq.network.p2p.node.authorization;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.security.pow.ProofOfWork;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The legacy HASH_CASH token of the handshake request of a node of a version up to 2.1.13. Those versions do not know
 * the features of the peer when they create their handshake request, and their default is the legacy method. The
 * versions 2.1.3 to 2.1.13 use HASH_CASH_V2 for all later messages.
 * <p>
 * A node of this version accepts the token only for a received handshake request and never creates it (see
 * docs/specifications/network/authorization-tokens.md). The class is therefore not an AuthorizationToken: no
 * NetworkEnvelope can hold it, and AuthorizationToken.fromProto rejects the legacy token body for every message.
 * <p>
 * The class has no toString method. The payload of the proof of work is the serialized handshake request.
 */
@Getter
@EqualsAndHashCode
public final class LegacyHandshakeToken {
    // The handshake request is the first message of a connection
    private static final int MESSAGE_COUNTER = 0;

    private final ProofOfWork proofOfWork;
    private final int messageCounter;

    private LegacyHandshakeToken(ProofOfWork proofOfWork, int messageCounter) {
        checkArgument(messageCounter == MESSAGE_COUNTER,
                "The message counter of a legacy handshake token must be %s", MESSAGE_COUNTER);
        this.proofOfWork = proofOfWork;
        this.messageCounter = messageCounter;
    }

    /**
     * @return true if the token has the body of the legacy HASH_CASH method. The declared type is not checked here:
     * a legacy body with another declared type is rejected by {@link #fromProto}.
     */
    public static boolean hasLegacyBody(bisq.network.protobuf.AuthorizationToken proto) {
        return proto.getMessageCase() == bisq.network.protobuf.AuthorizationToken.MessageCase.HASHCASHTOKEN;
    }

    @SuppressWarnings("deprecation")
    public static LegacyHandshakeToken fromProto(bisq.network.protobuf.AuthorizationToken proto)
            throws UnresolvableProtobufException {
        try {
            checkArgument(hasLegacyBody(proto), "The token has no legacy body");
            // As for the other methods, the declared type must match the token body
            checkArgument(proto.getAuthorizationTokenType() ==
                            bisq.network.protobuf.AuthorizationTokenType.AUTHORIZATIONTOKENTYPE_HASH_CASH,
                    "The declared type of a legacy token body must be the legacy type");
            bisq.network.protobuf.HashCashToken hashCashTokenProto = proto.getHashCashToken();
            return new LegacyHandshakeToken(ProofOfWork.fromProto(hashCashTokenProto.getProofOfWork()),
                    hashCashTokenProto.getMessageCounter());
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }
}
