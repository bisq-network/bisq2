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

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufEnumException;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.node.Feature;

import java.util.Optional;

public enum InventoryFilterType implements ProtoEnum {
    // Hash set filter with storage key V1. Only for responses to nodes of earlier versions. Replaced by HASH_SET_V2.
    @Deprecated(since = "2.1.14")
    HASH_SET,
    MINI_SKETCH,
    // Hash set filter with storage key V2
    HASH_SET_V2;

    public static Optional<InventoryFilterType> fromFeature(Feature feature) {
        return switch (feature) {
            case INVENTORY_HASH_SET -> Optional.of(HASH_SET);
            case INVENTORY_MINI_SKETCH -> Optional.of(MINI_SKETCH);
            case INVENTORY_HASH_SET_V2 -> Optional.of(HASH_SET_V2);
            default -> Optional.empty();
        };
    }

    @Override
    public bisq.network.protobuf.InventoryFilterType toProtoEnum() {
        return bisq.network.protobuf.InventoryFilterType.valueOf(getProtobufEnumPrefix() + name());
    }

    // The type defines which storage key the filter entries contain. An unknown type must not be read as another
    // type, so we reject it instead of using a fallback.
    public static InventoryFilterType fromProto(bisq.network.protobuf.InventoryFilterType proto)
            throws UnresolvableProtobufException {
        try {
            return ProtobufUtils.enumFromProto(InventoryFilterType.class, proto.name());
        } catch (UnresolvableProtobufEnumException e) {
            throw new UnresolvableProtobufMessageException(e);
        }
    }

    public boolean isHashSet() {
        return this == HASH_SET || this == HASH_SET_V2;
    }
}
