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
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
public class BlockDto implements NetworkProto {
    private final int height;
    private final byte[] hash;
    private final long time;
    private final List<TxDto> txDtoList;
    private final List<BurningManDto> burningManDtoList;

    public BlockDto(int height,
                    byte[] hash,
                    long time,
                    List<TxDto> txDtoList,
                    List<BurningManDto> burningManDtoList
    ) {
        this.height = height;
        this.hash = hash;
        this.time = time;
        this.txDtoList = txDtoList;
        this.burningManDtoList = burningManDtoList;
    }

    @Override
    public void verify() {
        //   NetworkDtoValidation.validateDate(timestamp);
    }

    @Override
    public bisq.oracle_node.bisq1_bridge.protobuf.BlockDto.Builder getBuilder(boolean serializeForHash) {
        return bisq.oracle_node.bisq1_bridge.protobuf.BlockDto.newBuilder()
                .setHeight(height)
                .setHash(ByteString.copyFrom(hash))
                .setTime(time)
                .addAllTxDto(txDtoList.stream().map(e -> e.toProto(serializeForHash)).toList())
                .addAllBurningManDto(burningManDtoList.stream().map(e -> e.toProto(serializeForHash)).toList());
    }

    @Override
    public bisq.oracle_node.bisq1_bridge.protobuf.BlockDto toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BlockDto fromProto(bisq.oracle_node.bisq1_bridge.protobuf.BlockDto proto) {
        return new BlockDto(proto.getHeight(),
                proto.getHash().toByteArray(),
                proto.getTime(),
                proto.getTxDtoList().stream().map(TxDto::fromProto).toList(),
                proto.getBurningManDtoList().stream().map(BurningManDto::fromProto).toList()
        );
    }
}
