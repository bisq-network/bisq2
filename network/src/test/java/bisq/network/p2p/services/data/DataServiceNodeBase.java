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

package bisq.network.p2p.services.data;

import bisq.common.util.ConfigUtil;
import bisq.common.util.OsUtils;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfigFactory;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class DataServiceNodeBase {
    protected MultiNodesSetup multiNodesSetup;

    protected Map<Transport.Type, List<Address>> bootstrapMultiNodesSetup(Set<Transport.Type> transports, int numSeeds, int numNodes) {
        String appDir = OsUtils.getUserDataDir() + File.separator + "misq_MultiNodes";

        Config typesafeConfig = ConfigUtil.load("test", "misq.networkServiceConfig");
        NetworkService.Config networkServiceConfigFactory = NetworkServiceConfigFactory.getConfig(appDir, typesafeConfig);

        multiNodesSetup = new MultiNodesSetup(networkServiceConfigFactory, transports, false);
        multiNodesSetup.setNumSeeds(numSeeds);
        multiNodesSetup.setNumNodes(numNodes);

        Stream<Address> seeds = transports.stream().flatMap(transport -> multiNodesSetup.getSeedAddresses(transport, numSeeds).stream());
        Stream<Address> nodes = transports.stream().flatMap(transport -> multiNodesSetup.getNodeAddresses(transport, numNodes).stream());
        Optional<List<Address>> addressesToBootstrap = Optional.of(Stream.concat(seeds, nodes).collect(Collectors.toList()));

        return multiNodesSetup.bootstrap(addressesToBootstrap);
    }

    public CompletableFuture<List<Void>> shutdown() {
        return multiNodesSetup.shutdown();
    }
}
