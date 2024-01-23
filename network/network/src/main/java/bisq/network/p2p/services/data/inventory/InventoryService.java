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

import bisq.common.util.ByteUnit;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.inventory.filter.FilterService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilterService;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.peergroup.PeerGroupManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Manages Inventory data requests and response and apply it to the data service.
 * We have InventoryServices for each supported transport. The data service though is a single instance getting services
 * by all transport specific services.
 * <p>
 */
@Slf4j
public class InventoryService {

    @Getter
    public static final class Config {
        // Default config value is 2000 (about 2MB)
        private final int maxSizeInKb;
        private final List<InventoryFilterType> myPreferredFilterTypes; // Lower list index means higher preference

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getInt("maxSizeInKb"),
                    new ArrayList<>(config.getEnumList(InventoryFilterType.class, "myPreferredFilterTypes")));
        }

        public Config(int maxSizeInKb, List<InventoryFilterType> myPreferredFilterTypes) {
            this.maxSizeInKb = maxSizeInKb;
            this.myPreferredFilterTypes = myPreferredFilterTypes;
        }
    }

    private final InventoryResponseService inventoryResponseService;
    private final InventoryRequestService inventoryRequestService;

    public InventoryService(Config config,
                            Node node,
                            PeerGroupManager peerGroupManager,
                            DataService dataService,
                            Set<Feature> features) {
        int maxSize = (int) Math.round(ByteUnit.KB.toBytes(config.getMaxSizeInKb()));
        Inventory.setMaxSize(maxSize);
        Map<InventoryFilterType, FilterService<? extends InventoryFilter>> supportedFilterServices = new HashMap<>();
        StorageService storageService = dataService.getStorageService();

        features.stream()
                .flatMap(feature -> InventoryFilter.fromFeature(feature).stream())
                .forEach(supportedFilterType -> {
                    switch (supportedFilterType) {
                        case HASH_SET:
                            supportedFilterServices.put(supportedFilterType, new HashSetFilterService(storageService, maxSize));
                            break;
                        case MINI_SKETCH:
                            //  supportedFilterServices.put(supportedFilterType, new MiniSketchFilterService(storageService, maxSize));
                            //  break;
                            throw new IllegalArgumentException("MINI_SKETCH not implemented yet");
                        default:
                            throw new IllegalArgumentException("Undefined filterType " + supportedFilterType);

                    }
                });
        inventoryResponseService = new InventoryResponseService(node, supportedFilterServices);
        inventoryRequestService = new InventoryRequestService(node,
                peerGroupManager,
                dataService,
                supportedFilterServices,
                config.getMyPreferredFilterTypes(),
                maxSize);
    }


    public void shutdown() {
        inventoryResponseService.shutdown();
        inventoryRequestService.shutdown();
    }
}