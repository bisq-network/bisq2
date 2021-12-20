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

@Slf4j
public class DataIntegrationTest extends DataNodeBase {
   /* @Test
    public void testAddData() throws InterruptedException {
        String baseDirPath = OsUtils.getUserDataDir() + "/DataIntegrationTest";
        NetworkServiceConfig networkServiceConfigsSeed = new NetworkServiceConfig(baseDirPath,
                new NodeId("default", 1000, Set.of(NetworkType.CLEAR)),
                NetworkType.CLEAR);
        NetworkServiceConfig networkServiceConfigs1 = new NetworkServiceConfig(baseDirPath,
                new NodeId("default", 1111, Set.of(NetworkType.CLEAR)),
                NetworkType.CLEAR);
        NetworkServiceConfig networkServiceConfigs2 = new NetworkServiceConfig(baseDirPath,
                new NodeId("default", 1112, Set.of(NetworkType.CLEAR)),
                NetworkType.CLEAR);

        bootstrap(Set.of(networkServiceConfigsSeed), Set.of(networkServiceConfigs1), Set.of(networkServiceConfigs2));
        addData();
    }*/

    private void addData() {
      /*  MockMessage data1 = new MockMessage("data1");
        AddDataRequest addDataRequest = new AddDataRequest(data1);
        p2pService1.requestAddData(addDataRequest, gossipResult -> {
            log.info(gossipResult.toString());
        });*/
    }

}
