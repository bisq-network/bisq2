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
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.inventory.filter.FilterService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilterService;
import bisq.network.p2p.services.data.storage.StorageService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class InventoryFilterFactory {
    private final Map<InventoryFilterType, FilterService<? extends InventoryFilter>> supportedFilterServices = new HashMap<>();
    private final InventoryService.Config config;

    public InventoryFilterFactory(Set<Feature> features,
                                  DataService dataService,
                                  InventoryService.Config config) {
        this.config = config;
        int maxSize = (int) Math.round(ByteUnit.KB.toBytes(config.getMaxSizeInKb()));
        Inventory.setMaxSize(maxSize);
        StorageService storageService = dataService.getStorageService();

        features.stream()
                .flatMap(feature -> InventoryFilterType.fromFeature(feature).stream())
                .forEach(supportedFilterType -> {
                    switch (supportedFilterType) {
                        case HASH_SET:
                            supportedFilterServices.put(supportedFilterType, new HashSetFilterService(storageService, maxSize));
                            break;
                        case MINI_SKETCH:
                            //  map.put(supportedFilterType, new MiniSketchFilterService(storageService, maxSize));
                            //  break;
                            throw new IllegalArgumentException("MINI_SKETCH not implemented yet");
                        default:
                            throw new IllegalArgumentException("Undefined filterType " + supportedFilterType);

                    }
                });
    }

    public InventoryFilter createInventoryFilterForRequest(Connection connection) {
        List<Feature> peersFeatures = connection.getPeersCapability().getFeatures();
        InventoryFilterType inventoryFilterType = getPreferredFilterType(peersFeatures).orElseThrow(); // we filtered before for presence
        FilterService<? extends InventoryFilter> filterService = supportedFilterServices.get(inventoryFilterType);
        return filterService.getFilter();
    }

    public Inventory createInventoryForResponse(InventoryRequest request) {
        InventoryFilter inventoryFilter = request.getInventoryFilter();
        InventoryFilterType inventoryFilterType = inventoryFilter.getInventoryFilterType();
        checkArgument(supportedFilterServices.containsKey(inventoryFilterType),
                "We got an inventoryRequest with filterType {} which we do not support." +
                        "This should never happen if our feature entries are correct and if the peers code is executed as expected.", inventoryFilterType);
        FilterService<? extends InventoryFilter> filterService = supportedFilterServices.get(inventoryFilterType);
        int requestersVersion = request.getVersion();

        // We filter out version 1 objects in Add/Remove DataRequest objects which would break the hash when requested from old nodes (pre v.2.1.0)
        // This code can be removed once there are no old nodes expected in the network anymore.
        Predicate<Integer> predicate = distributedDataVersion -> requestersVersion > 0 || distributedDataVersion == 0;

        return filterService.createInventory(inventoryFilter, predicate);
    }

    // Get first match with peers feature based on order of myPreferredFilterTypes
    private Optional<InventoryFilterType> getPreferredFilterType(List<Feature> peersFeatures) {
        List<InventoryFilterType> peersInventoryFilterTypes = toFilterTypes(peersFeatures);
        return config.getMyPreferredFilterTypes().stream()
                .filter(peersInventoryFilterTypes::contains)
                .findFirst();
    }

    private List<InventoryFilterType> toFilterTypes(List<Feature> features) {
        return features.stream()
                .flatMap(feature -> InventoryFilterType.fromFeature(feature).stream())
                .collect(Collectors.toList());
    }
}
