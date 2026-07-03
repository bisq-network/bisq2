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

@Getter
public class CustomCloseTradeResponse implements Proto {
    private final byte[] customPayoutTx;

    public CustomCloseTradeResponse(byte[] customPayoutTx) {
        this.customPayoutTx = customPayoutTx;
    }

    @Override
    public bisq.trade.protobuf.CustomCloseTradeResponse.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.CustomCloseTradeResponse.newBuilder()
                .setCustomPayoutTx(ByteString.copyFrom(customPayoutTx));
    }

    @Override
    public bisq.trade.protobuf.CustomCloseTradeResponse toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static CustomCloseTradeResponse fromProto(bisq.trade.protobuf.CustomCloseTradeResponse proto) {
        return new CustomCloseTradeResponse(proto.getCustomPayoutTx().toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CustomCloseTradeResponse that &&
                Arrays.equals(customPayoutTx, that.customPayoutTx);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(customPayoutTx);
    }
}
