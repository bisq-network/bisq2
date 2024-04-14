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

package bisq.network.p2p.services.peer_group.keep_alive;

import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Response;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class Pong implements EnvelopePayloadMessage, Response {
    private final int requestNonce;

    public Pong(int requestNonce) {
        this.requestNonce = requestNonce;
    }

    @Override
    public void verify() {
    }
    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
        return newEnvelopePayloadMessageBuilder().setPong(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.Pong toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.Pong.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.Pong.newBuilder().setRequestNonce(requestNonce);
    }

    public static Pong fromProto(bisq.network.protobuf.Pong proto) {
        return new Pong(proto.getRequestNonce());
    }

    @Override
    public double getCostFactor() {
        return 0.05;
    }

    @Override
    public String getRequestId() {
        return String.valueOf(requestNonce);
    }
}