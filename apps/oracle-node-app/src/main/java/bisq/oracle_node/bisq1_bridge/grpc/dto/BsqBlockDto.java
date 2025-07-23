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

import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
public final class BsqBlockDto implements NetworkProto {
    private final int height;
    private final long time;
    private final List<TxDto> txDtoList;

    public BsqBlockDto(int height,
                       long time,
                       List<TxDto> txDtoList) {
        this.height = height;
        this.time = time;
        this.txDtoList = txDtoList;
    }

    @Override
    public void verify() {
        //   NetworkDtoValidation.validateDate(timestamp);
    }

    @Override
    public bisq.bridge.protobuf.BsqBlockDto.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.BsqBlockDto.newBuilder()
                .setHeight(height)
                .setTime(time)
                .addAllTxDto(txDtoList.stream().map(e -> e.toProto(serializeForHash)).toList());
    }

    @Override
    public bisq.bridge.protobuf.BsqBlockDto toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BsqBlockDto fromProto(bisq.bridge.protobuf.BsqBlockDto proto) {
        return new BsqBlockDto(proto.getHeight(),
                proto.getTime(),
                proto.getTxDtoList().stream().map(TxDto::fromProto).toList()
        );
    }
}
