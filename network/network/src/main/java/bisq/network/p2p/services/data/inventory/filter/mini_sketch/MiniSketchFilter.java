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

package bisq.network.p2p.services.data.inventory.filter.mini_sketch;


import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Skeleton for planned MiniSketch implementation based on <a href="https://github.com/sipa/minisketch">https://github.com/sipa/minisketch</a>
 */
@Slf4j
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public final class MiniSketchFilter extends InventoryFilter {

    public MiniSketchFilter() {
        this(InventoryFilterType.MINI_SKETCH);
    }

    private MiniSketchFilter(InventoryFilterType inventoryFilterType) {
        super(inventoryFilterType);

        verify();
    }

    @Override
    public void verify() {
        checkArgument(inventoryFilterType == InventoryFilterType.MINI_SKETCH);
    }

    @Override
    public bisq.network.protobuf.InventoryFilter toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.InventoryFilter.Builder getBuilder(boolean serializeForHash) {
        return getInventoryFilterBuilder().setMiniSketchFilter(
                bisq.network.protobuf.MiniSketchFilter.newBuilder());
    }

    public static MiniSketchFilter fromProto(bisq.network.protobuf.InventoryFilter proto) {
        return new MiniSketchFilter(InventoryFilterType.fromProto(proto.getInventoryFilterType()));
    }

    @Override
    public String getDetails() {
        return "MiniSketchFilter";
    }
}