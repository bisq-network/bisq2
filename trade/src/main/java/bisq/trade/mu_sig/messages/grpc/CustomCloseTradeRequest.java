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
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

@Getter
public class CustomCloseTradeRequest implements Proto {
    private final String tradeId;
    private final byte[] peersCustomPayoutPsbt;

    public CustomCloseTradeRequest(String tradeId, byte[] peersCustomPayoutPsbt) {
        this.tradeId = tradeId;
        this.peersCustomPayoutPsbt = peersCustomPayoutPsbt;
    }

    @Override
    public bisq.trade.protobuf.CustomCloseTradeRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.CustomCloseTradeRequest.newBuilder()
                .setTradeId(tradeId)
                .setPeersCustomPayoutPsbt(ByteString.copyFrom(peersCustomPayoutPsbt));
    }

    @Override
    public bisq.trade.protobuf.CustomCloseTradeRequest toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static CustomCloseTradeRequest fromProto(bisq.trade.protobuf.CustomCloseTradeRequest proto) {
        return new CustomCloseTradeRequest(proto.getTradeId(),
                proto.getPeersCustomPayoutPsbt().toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CustomCloseTradeRequest that &&
                Objects.equals(tradeId, that.tradeId) &&
                Arrays.equals(peersCustomPayoutPsbt, that.peersCustomPayoutPsbt);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(tradeId);
        result = 31 * result + Arrays.hashCode(peersCustomPayoutPsbt);
        return result;
    }
}
