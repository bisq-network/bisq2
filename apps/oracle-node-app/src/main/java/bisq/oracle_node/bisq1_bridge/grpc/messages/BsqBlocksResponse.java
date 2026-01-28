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

package bisq.oracle_node.bisq1_bridge.grpc.messages;


import bisq.common.proto.NetworkProto;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BsqBlockDto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@EqualsAndHashCode
@ToString
public final class BsqBlocksResponse implements NetworkProto {
    private final List<BsqBlockDto> blocks;

    public BsqBlocksResponse(List<BsqBlockDto> blocks) {
        this.blocks = blocks;
    }

    @Override
    public void verify() {
        checkArgument(blocks.size() < 10000);
    }

    @Override
    public bisq.bridge.protobuf.BsqBlocksResponse.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.BsqBlocksResponse.newBuilder()
                .addAllBsqBlocks(blocks.stream().map(e -> e.toProto(serializeForHash)).collect(Collectors.toList()));
    }

    @Override
    public bisq.bridge.protobuf.BsqBlocksResponse toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static BsqBlocksResponse fromProto(bisq.bridge.protobuf.BsqBlocksResponse proto) {
        return new BsqBlocksResponse(proto.getBsqBlocksList().stream().map(BsqBlockDto::fromProto).collect(Collectors.toList()));
    }
}
