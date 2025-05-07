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

package bisq.trade.mu_sig.messages.grpc;

import bisq.common.proto.Proto;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class CloseTradeResponse implements Proto {
    private final byte[] peerOutputPrvKeyShare;

    public CloseTradeResponse(byte[] peerOutputPrvKeyShare) {
        this.peerOutputPrvKeyShare = peerOutputPrvKeyShare;
    }

    @Override
    public bisq.trade.protobuf.CloseTradeResponse.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.CloseTradeResponse.newBuilder()
                .setPeerOutputPrvKeyShare(ByteString.copyFrom(peerOutputPrvKeyShare));
    }

    @Override
    public bisq.trade.protobuf.CloseTradeResponse toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static CloseTradeResponse fromProto(bisq.trade.protobuf.CloseTradeResponse proto) {
        return new CloseTradeResponse(proto.getPeerOutputPrvKeyShare().toByteArray());
    }
}
