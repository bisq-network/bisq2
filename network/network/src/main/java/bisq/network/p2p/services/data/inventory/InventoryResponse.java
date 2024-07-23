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
import bisq.network.p2p.message.Response;
import bisq.network.p2p.services.data.broadcast.BroadcastMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class InventoryResponse implements BroadcastMessage, Response {
    private static final int VERSION = 1;

    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    // After v 2.1.0 version 0 should not be used anymore. Then we can remove the excludeOnlyInVersions param to
    // not need to maintain future versions. We add though hypothetical versions 2 and 3 for safety
    @EqualsAndHashCode.Exclude
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    private final Inventory inventory;
    private final int requestNonce;

    public InventoryResponse(Inventory inventory, int requestNonce) {
        this(VERSION, inventory, requestNonce);
    }

    public InventoryResponse(int version, Inventory inventory, int requestNonce) {
        this.version = version;
        this.inventory = inventory;
        this.requestNonce = requestNonce;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
        return newEnvelopePayloadMessageBuilder().setInventoryResponse(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.InventoryResponse toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.InventoryResponse.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.InventoryResponse.newBuilder()
                .setVersion(version)
                .setInventory(inventory.toProto(serializeForHash))
                .setRequestNonce(requestNonce);
    }

    public static InventoryResponse fromProto(bisq.network.protobuf.InventoryResponse proto) {
        return new InventoryResponse(proto.getVersion(), Inventory.fromProto(proto.getInventory()), proto.getRequestNonce());
    }

    @Override
    public double getCostFactor() {
        return 0.1;
    }

    @Override
    public String getRequestId() {
        return String.valueOf(requestNonce);
    }
}
