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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public final class BurningmanBlocksRequest implements NetworkProto {
    private final int startBlockHeight;

    public BurningmanBlocksRequest(int startBlockHeight) {
        this.startBlockHeight = startBlockHeight;
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.bridge.protobuf.BurningmanBlocksRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.BurningmanBlocksRequest.newBuilder()
                .setStartBlockHeight(startBlockHeight);
    }

    @Override
    public bisq.bridge.protobuf.BurningmanBlocksRequest toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.bridge.protobuf.BurningmanBlocksRequest completeProto() {
        return toProto(false);
    }

    public static BurningmanBlocksRequest fromProto(bisq.bridge.protobuf.BurningmanBlocksRequest proto) {
        return new BurningmanBlocksRequest(proto.getStartBlockHeight());
    }
}
