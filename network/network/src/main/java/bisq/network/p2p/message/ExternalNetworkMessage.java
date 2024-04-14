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

import com.google.protobuf.Any;

// Wrapper for NetworkMessages which are not part of the network module (e.g. PrivateChatMessage).
// We wrap them into an Any binary blob.
public interface ExternalNetworkMessage extends EnvelopePayloadMessage {

    default bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
        return newEnvelopePayloadMessageBuilder().setExternalNetworkMessage(toExternalNetworkMessageProto(serializeForHash));
    }

    default bisq.network.protobuf.ExternalNetworkMessage toExternalNetworkMessageProto(boolean serializeForHash) {
        return resolveBuilder(getExternalPayloadMessageBuilder(serializeForHash), serializeForHash).build();
    }

    default bisq.network.protobuf.ExternalNetworkMessage.Builder getExternalPayloadMessageBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.ExternalNetworkMessage.newBuilder()
                .setPayload(Any.pack(toValueProto(serializeForHash)));
    }

    static EnvelopePayloadMessage fromProto(bisq.network.protobuf.ExternalNetworkMessage externalNetworkMessage) {
        return NetworkMessageResolver.fromAny(externalNetworkMessage.getPayload());
    }
}