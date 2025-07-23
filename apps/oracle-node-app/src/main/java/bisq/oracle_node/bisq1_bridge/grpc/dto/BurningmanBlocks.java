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
public final class BurningmanBlocks implements NetworkProto {
    private final List<BurningmanBlockDto> blocks;

    public BurningmanBlocks(List<BurningmanBlockDto> blocks) {
        this.blocks = blocks;
    }

    @Override
    public void verify() {
        //   NetworkDtoValidation.validateDate(timestamp);
    }

    @Override
    public bisq.bridge.protobuf.BurningmanBlocks.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.BurningmanBlocks.newBuilder()
                .addAllBurningmanBlocks(blocks.stream().map(e -> e.toProto(serializeForHash)).toList());
    }

    @Override
    public bisq.bridge.protobuf.BurningmanBlocks toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BurningmanBlocks fromProto(bisq.bridge.protobuf.BurningmanBlocks proto) {
        return new BurningmanBlocks(proto.getBurningmanBlocksList().stream().map(BurningmanBlockDto::fromProto).toList());
    }
}
