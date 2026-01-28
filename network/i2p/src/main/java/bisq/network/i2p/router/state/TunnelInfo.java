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

package bisq.network.i2p.router.state;

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class TunnelInfo implements Proto {
    private final int inboundClientTunnelCount;
    private final int outboundTunnelCount;
    private final int outboundClientTunnelCount;

    public TunnelInfo() {
        this(0, 0, 0);
    }

    public TunnelInfo(int inboundClientTunnelCount, int outboundTunnelCount, int outboundClientTunnelCount) {
        this.inboundClientTunnelCount = inboundClientTunnelCount;
        this.outboundTunnelCount = outboundTunnelCount;
        this.outboundClientTunnelCount = outboundClientTunnelCount;
    }

    @Override
    public bisq.bi2p.protobuf.TunnelInfo completeProto() {
        return toProto(false);
    }

    @Override
    public bisq.bi2p.protobuf.TunnelInfo toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.bi2p.protobuf.TunnelInfo.Builder getBuilder(boolean serializeForHash) {
        return bisq.bi2p.protobuf.TunnelInfo.newBuilder()
                .setInboundClientTunnelCount(inboundClientTunnelCount)
                .setOutboundTunnelCount(outboundTunnelCount)
                .setOutboundClientTunnelCount(outboundClientTunnelCount);
    }


    public static TunnelInfo fromProto(bisq.bi2p.protobuf.TunnelInfo proto) {
        return new TunnelInfo(proto.getInboundClientTunnelCount(),
                proto.getOutboundTunnelCount(),
                proto.getOutboundClientTunnelCount());
    }


}
