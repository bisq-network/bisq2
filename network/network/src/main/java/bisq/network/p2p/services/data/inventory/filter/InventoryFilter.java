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

package bisq.network.p2p.services.data.inventory.filter;

import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilter;
import bisq.network.p2p.services.data.inventory.filter.mini_sketch.MiniSketchFilter;
import lombok.Getter;

public abstract class InventoryFilter implements NetworkProto {
    @Getter
    protected final InventoryFilterType inventoryFilterType;

    public InventoryFilter(InventoryFilterType inventoryFilterType) {
        this.inventoryFilterType = inventoryFilterType;
    }

    public bisq.network.protobuf.InventoryFilter.Builder getInventoryFilterBuilder() {
        return bisq.network.protobuf.InventoryFilter.newBuilder()
                .setInventoryFilterType(inventoryFilterType.toProtoEnum());
    }

    public static InventoryFilter fromProto(bisq.network.protobuf.InventoryFilter proto) {
        return switch (proto.getMessageCase()) {
            case HASHSETFILTER -> HashSetFilter.fromProto(proto);
            case MINISKETCHFILTER -> MiniSketchFilter.fromProto(proto);
            default -> throw new UnresolvableProtobufMessageException(proto);
        };
    }

    @Override
    abstract public bisq.network.protobuf.InventoryFilter toProto(boolean serializeForHash);

    abstract public String getDetails();
}
