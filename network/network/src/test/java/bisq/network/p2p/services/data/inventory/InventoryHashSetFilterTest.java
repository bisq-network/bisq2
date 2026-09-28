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

import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryHashSetFilterTest {
    // The type defines which storage key the entries contain, so it must never be guessed
    @Test
    void unknownOrMismatchedFilterTypeIsRejected() {
        bisq.network.protobuf.InventoryFilter proto = new HashSetFilter(InventoryFilterType.HASH_SET, new ArrayList<>())
                .toProto(false);

        bisq.network.protobuf.InventoryFilter unspecified = proto.toBuilder()
                .setInventoryFilterType(bisq.network.protobuf.InventoryFilterType.INVENTORYFILTERTYPE_UNSPECIFIED)
                .build();
        assertThrows(UnresolvableProtobufMessageException.class, () -> InventoryFilter.fromProto(unspecified));

        bisq.network.protobuf.InventoryFilter unknown = proto.toBuilder().setInventoryFilterTypeValue(99).build();
        assertThrows(UnresolvableProtobufMessageException.class, () -> InventoryFilter.fromProto(unknown));

        bisq.network.protobuf.InventoryFilter miniSketchType = proto.toBuilder()
                .setInventoryFilterType(bisq.network.protobuf.InventoryFilterType.INVENTORYFILTERTYPE_MINI_SKETCH)
                .build();
        assertThrows(IllegalArgumentException.class, () -> InventoryFilter.fromProto(miniSketchType));
    }
}
