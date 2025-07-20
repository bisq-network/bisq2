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
public class BlockData implements NetworkProto {
    private final int height;
    private final byte[] hash;
    private final long time;
    private final List<TxData> txDataList;
    private final List<BurningManData> burningManDataList;

    public BlockData(int height,
                     byte[] hash,
                     long time,
                     List<TxData> txDataList,
                     List<BurningManData> burningManDataList
    ) {
        this.height = height;
        this.hash = hash;
        this.time = time;
        this.txDataList = txDataList;
        this.burningManDataList = burningManDataList;
    }

    @Override
    public void verify() {
        //   NetworkDataValidation.validateDate(timestamp);
    }

    @Override
    public bisq.oracle_node.bisq1_bridge.protobuf.BlockData.Builder getBuilder(boolean serializeForHash) {
        return bisq.oracle_node.bisq1_bridge.protobuf.BlockData.newBuilder()
                .setHeight(height)
                .setHash(ByteString.copyFrom(hash))
                .setTime(time)
                .addAllTxData(txDataList.stream().map(e -> e.toProto(serializeForHash)).toList())
                .addAllBurningManData(burningManDataList.stream().map(e -> e.toProto(serializeForHash)).toList());
    }

    @Override
    public bisq.oracle_node.bisq1_bridge.protobuf.BlockData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BlockData fromProto(bisq.oracle_node.bisq1_bridge.protobuf.BlockData proto) {
        return new BlockData(proto.getHeight(),
                proto.getHash().toByteArray(),
                proto.getTime(),
                proto.getTxDataList().stream().map(TxData::fromProto).toList(),
                proto.getBurningManDataList().stream().map(BurningManData::fromProto).toList()
        );
    }
}
