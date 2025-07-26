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

package bisq.oracle_node.bisq1_bridge.grpc.dto;

import bisq.common.proto.NetworkProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public final class BurningmanDto implements NetworkProto {
    private final String receiverAddress;
    private final double cappedBurnAmountShare;

    public BurningmanDto(String receiverAddress,
                         double cappedBurnAmountShare) {
        this.receiverAddress = receiverAddress;
        this.cappedBurnAmountShare = cappedBurnAmountShare;
    }

    @Override
    public void verify() {
        //   NetworkDataValidation.validateDate(timestamp);
    }

    @Override
    public bisq.bridge.protobuf.BurningmanDto.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.BurningmanDto.newBuilder()
                .setReceiverAddress(receiverAddress)
                .setCappedBurnAmountShare(cappedBurnAmountShare);
    }

    @Override
    public bisq.bridge.protobuf.BurningmanDto toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BurningmanDto fromProto(bisq.bridge.protobuf.BurningmanDto proto) {
        return new BurningmanDto(proto.getReceiverAddress(),
                proto.getCappedBurnAmountShare());
    }
}
