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

package bisq.network.p2p.services.peergroup.keepalive;

import bisq.network.p2p.message.EnvelopePayloadMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class Ping implements EnvelopePayloadMessage {
    private final int nonce;

    public Ping(int nonce) {
        this.nonce = nonce;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage toProto() {
        return getNetworkMessageBuilder().setPing(bisq.network.protobuf.Ping.newBuilder().setNonce(nonce)).build();
    }

    public static Ping fromProto(bisq.network.protobuf.Ping proto) {
        return new Ping(proto.getNonce());
    }

    @Override
    public double getCostFactor() {
        return 0.05;
    }
}