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

package bisq.oracle_node;

import bisq.application.Executable;
import bisq.common.facades.FacadeProvider;
import bisq.common.file.FileMutatorUtils;
import bisq.common.network.AddressByTransportTypeMap;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class OracleNodeApp extends Executable<OracleNodeApplicationService> {
    public static void main(String[] args) {
        Thread.currentThread().setName("OracleNodeApp.main");
        new OracleNodeApp(args);
    }

    public OracleNodeApp(String[] args) {
        super(args);
    }

    @Override
    protected void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
        AddressByTransportTypeMap addressByTransportTypeMap =
                applicationService.getIdentityService()
                        .getOrCreateDefaultIdentity()
                        .getNetworkId()
                        .getAddressByTransportTypeMap();
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(addressByTransportTypeMap);
        Path path = applicationService.getConfig().getAppDataDirPath().resolve("default_node_address.json");
        try {
            FacadeProvider.getJdkFacade().writeString(json, path);
        } catch (IOException e) {
            log.error("Error at write json", e);
        }
    }

    @Override
    protected OracleNodeApplicationService createApplicationService(String[] args) {
        return new OracleNodeApplicationService(args);
    }
}
