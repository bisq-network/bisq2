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
import bisq.network.p2p.services.data.filter.DataFilter;

public record InventoryRequest(DataFilter dataFilter, int nonce) implements BroadcastMessage {
    public bisq.network.protobuf.NetworkMessage toNetworkMessageProto() {
        return getNetworkMessageBuilder().setInventoryRequest(
                        bisq.network.protobuf.InventoryRequest.newBuilder()
                                .setDataFilter(dataFilter.toProto())
                                .setNonce(nonce))
                .build();
    }

    public static InventoryRequest fromProto(bisq.network.protobuf.InventoryRequest proto) {
        return new InventoryRequest(DataFilter.fromProto(proto.getDataFilter()), proto.getNonce());
    }
}
