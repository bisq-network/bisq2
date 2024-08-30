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

import bisq.common.annotation.ExcludeForHash;
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
    private static final int VERSION = 1;

    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    private final InventoryFilter inventoryFilter;
    private final int nonce;


    public InventoryRequest(InventoryFilter inventoryFilter, int nonce) {
        this(VERSION, inventoryFilter, nonce);
    }

    private InventoryRequest(int version, InventoryFilter inventoryFilter, int nonce) {
        this.version = version;
        this.inventoryFilter = inventoryFilter;
        this.nonce = nonce;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
        return newEnvelopePayloadMessageBuilder().setInventoryRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.InventoryRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.InventoryRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.InventoryRequest.newBuilder()
                .setVersion(version)
                .setInventoryFilter(inventoryFilter.toProto(serializeForHash))
                .setNonce(nonce);
    }

    public static InventoryRequest fromProto(bisq.network.protobuf.InventoryRequest proto) {
        return new InventoryRequest(proto.getVersion(),
                InventoryFilter.fromProto(proto.getInventoryFilter()),
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
