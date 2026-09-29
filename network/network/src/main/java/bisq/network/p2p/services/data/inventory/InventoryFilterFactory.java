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

import bisq.common.data.ByteUnit;
import bisq.common.timer.Clock;
import bisq.common.timer.SystemClock;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.inventory.filter.FilterService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilterService;
import bisq.network.p2p.services.data.storage.SignedRequest;
import bisq.network.p2p.services.data.storage.StorageService;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
class InventoryFilterFactory {
    private final Map<InventoryFilterType, FilterService<? extends InventoryFilter>> mySupportedFilterServices = new HashMap<>();
    private final InventoryService.Config config;
    // The time of the node. It decides whether the node requires signature V2 (see getPreferredFilterType).
    private final Clock clock;

    InventoryFilterFactory(Set<Feature> myFeatures,
                           DataService dataService,
                           InventoryService.Config config) {
        this(myFeatures, dataService, config, new SystemClock());
    }

    @VisibleForTesting
    InventoryFilterFactory(Set<Feature> myFeatures,
                           DataService dataService,
                           InventoryService.Config config,
                           Clock clock) {
        this.config = config;
        this.clock = clock;
        int maxSize = (int) Math.round(ByteUnit.KB.toBytes(config.getMaxSizeInKb()));
        Inventory.setMaxSize(maxSize);
        StorageService storageService = dataService.getStorageService();

        myFeatures.stream()
                .flatMap(feature -> InventoryFilterType.fromFeature(feature).stream())
                .forEach(inventoryFilterType -> {
                    switch (inventoryFilterType) {
                        case HASH_SET:
                        case HASH_SET_V2:
                            mySupportedFilterServices.put(inventoryFilterType,
                                    new HashSetFilterService(storageService, maxSize, inventoryFilterType));
                            break;
                        case MINI_SKETCH:
                            //  map.put(inventoryFilterType, new MiniSketchFilterService(storageService, maxSize));
                            //  break;
                            throw new IllegalArgumentException("MINI_SKETCH not implemented yet");
                        default:
                            throw new IllegalArgumentException("Undefined filterType " + inventoryFilterType);

                    }
                });
    }

    InventoryFilter createInventoryFilterForRequest(Connection connection) {
        return createInventoryFilterForRequest(connection.getPeersCapability().getFeatures());
    }

    @VisibleForTesting
    InventoryFilter createInventoryFilterForRequest(List<Feature> peersFeatures) {
        // InventoryRequestPolicy only selects peers with a preferred filter type. We do not fall back to another type:
        // the type defines which storage key the filter contains.
        InventoryFilterType inventoryFilterType = getPreferredFilterType(peersFeatures)
                .orElseThrow(() -> new IllegalStateException("Peer supports none of our preferred inventory filter types. " +
                        "peersFeatures=" + peersFeatures));
        FilterService<? extends InventoryFilter> filterService = mySupportedFilterServices.get(inventoryFilterType);
        return filterService.getFilter();
    }

    // The type of the received filter selects the service, not the features of the peer: a peer which supports several
    // types chooses one per request.
    Inventory createInventoryForResponse(InventoryRequest request) {
        InventoryFilter inventoryFilter = request.getInventoryFilter();
        InventoryFilterType inventoryFilterType = inventoryFilter.getInventoryFilterType();
        checkArgument(mySupportedFilterServices.containsKey(inventoryFilterType),
                "We got an inventoryRequest with filterType {} which we do not support." +
                        "This should never happen if our feature entries are correct and if the peers code is executed as expected.", inventoryFilterType);
        FilterService<? extends InventoryFilter> filterService = mySupportedFilterServices.get(inventoryFilterType);
        int requestersVersion = request.getVersion();

        return filterService.createInventory(inventoryFilter);
    }

    // Get first match with peers feature based on order of myPreferredFilterTypes. From the cutoff date of signature V1
    // on, the node does not request with HASH_SET. Only peers of earlier versions need it, and they serve only requests
    // without signature V2, which the node then rejects (see docs/specifications/network/inventory-hash-set-filter.md).
    // A peer which supports no other type is then not used for inventory requests.
    Optional<InventoryFilterType> getPreferredFilterType(List<Feature> peersFeatures) {
        List<InventoryFilterType> peersInventoryFilterTypes = toFilterTypes(peersFeatures);
        boolean isSignatureV2Required = SignedRequest.isSignatureV2Required(clock.now());
        return config.getMyPreferredFilterTypes().stream()
                .filter(peersInventoryFilterTypes::contains)
                .filter(mySupportedFilterServices::containsKey)
                .filter(inventoryFilterType -> !isSignatureV2Required || inventoryFilterType != InventoryFilterType.HASH_SET)
                .findFirst();
    }

    private List<InventoryFilterType> toFilterTypes(List<Feature> features) {
        return features.stream()
                .flatMap(feature -> InventoryFilterType.fromFeature(feature).stream())
                .collect(Collectors.toList());
    }
}
