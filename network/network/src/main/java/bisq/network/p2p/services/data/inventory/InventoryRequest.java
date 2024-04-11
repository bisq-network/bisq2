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

import bisq.network.p2p.message.Request;
import bisq.network.p2p.services.data.broadcast.BroadcastMessage;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class InventoryRequest implements BroadcastMessage, Request {
    private final InventoryFilter inventoryFilter;
    private final int nonce;

    public InventoryRequest(InventoryFilter inventoryFilter, int nonce) {
        this.inventoryFilter = inventoryFilter;
        this.nonce = nonce;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage toProto() {
        return getNetworkMessageBuilder().setInventoryRequest(
                        bisq.network.protobuf.InventoryRequest.newBuilder()
                                .setInventoryFilter(inventoryFilter.toProto())
                                .setNonce(nonce))
                .build();
    }

    public static InventoryRequest fromProto(bisq.network.protobuf.InventoryRequest proto) {
        return new InventoryRequest(InventoryFilter.fromProto(proto.getInventoryFilter()),
                proto.getNonce());
    }

    @Override
    public double getCostFactor() {
        return 0.25;
    }

    @Override
    public String getRequestId() {
        return String.valueOf(nonce);
    }
}
