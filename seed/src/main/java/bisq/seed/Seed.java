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

package bisq.seed;

import bisq.application.NetworkApplicationService;
import bisq.common.util.FileUtils;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.Transport;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
public class Seed {

    public Seed(String[] args) {
        NetworkApplicationService applicationService = new NetworkApplicationService(args);
        applicationService.readAllPersisted()
                .thenCompose(result -> applicationService.initialize())
                .whenComplete((r, t) -> {
                    Map<Transport.Type, Address> addressByNetworkType = applicationService.getNetworkService().getAddressByNetworkType(Node.DEFAULT);
                    String json = new Gson().toJson(addressByNetworkType);
                    Path path = Path.of(applicationService.getConfig().getBaseDir(), "default_node_address.json");
                    try {
                        FileUtils.writeToFile(json, path.toFile());
                    } catch (IOException e) {
                        log.error("Error at write json", e);
                    }
                });
    }
}
