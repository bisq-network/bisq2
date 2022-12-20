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

package bisq.network.p2p.services.data.inventory;

import bisq.network.p2p.services.data.broadcast.BroadcastMessage;
import bisq.network.protobuf.NetworkMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class InventoryResponse implements BroadcastMessage {
    private final Inventory inventory;
    private final int requestNonce;

    public InventoryResponse(Inventory inventory, int requestNonce) {
        this.inventory = inventory;
        this.requestNonce = requestNonce;
    }

    @Override
    public NetworkMessage toProto() {
        return getNetworkMessageBuilder().setInventoryResponse(
                        bisq.network.protobuf.InventoryResponse.newBuilder()
                                .setInventory(inventory.toProto())
                                .setRequestNonce(requestNonce))
                .build();
    }

    public static InventoryResponse fromProto(bisq.network.protobuf.InventoryResponse proto) {
        return new InventoryResponse(Inventory.fromProto(proto.getInventory()), proto.getRequestNonce());
    }
}
