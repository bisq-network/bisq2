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

package network.misq.network.p2p.services.data;

import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.OsUtils;
import network.misq.network.NetworkServiceConfigFactory;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.monitor.MultiNodesSetup;

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
        String baseDir = OsUtils.getUserDataDir() + File.separator + "misq_MultiNodes";
        NetworkServiceConfigFactory networkServiceConfigFactory = new NetworkServiceConfigFactory(baseDir);
        multiNodesSetup = new MultiNodesSetup(networkServiceConfigFactory.get(), transports, false);

        Stream<Address> seeds = transports.stream().flatMap(transport -> multiNodesSetup.getSeedAddresses(transport, numSeeds).stream());
        Stream<Address> nodes = transports.stream().flatMap(transport -> multiNodesSetup.getNodeAddresses(transport, numNodes).stream());
        Optional<List<Address>> addressesToBootstrap = Optional.of(Stream.concat(seeds, nodes).collect(Collectors.toList()));

        return multiNodesSetup.bootstrap(addressesToBootstrap, 0);
    }

    public CompletableFuture<List<Void>> shutdown() {
        return multiNodesSetup.shutdown();
    }
}
