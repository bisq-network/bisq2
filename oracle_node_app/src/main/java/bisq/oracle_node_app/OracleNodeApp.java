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

package bisq.oracle_node_app;

import bisq.common.util.FileUtils;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.Type;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
public class OracleNodeApp {
    public static void main(String[] args) {
        OracleNodeApplicationService applicationService = new OracleNodeApplicationService(args);
        applicationService.readAllPersisted()
                .thenCompose(result -> applicationService.initialize())
                .whenComplete((result, throwable) -> {
                    Map<Type, Address> addressByNetworkType = applicationService.getNetworkService().getAddressByNetworkType(Node.DEFAULT);
                    String json = new GsonBuilder().setPrettyPrinting().create().toJson(addressByNetworkType);
                    Path path = applicationService.getConfig().getBaseDir().resolve("default_node_address.json");
                    try {
                        FileUtils.writeToFile(json, path.toFile());
                    } catch (IOException e) {
                        log.error("Error at write json", e);
                    }
                });

        keepRunning();
    }

    private static void keepRunning() {
        try {
            // block and wait shut down signal, like CTRL+C
            Thread.currentThread().join();
        } catch (InterruptedException ignore) {
        }
    }
}
