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

package bisq.protocol.prototype;

import bisq.common.util.ConfigUtil;
import bisq.common.util.OsUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfigFactory;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import com.typesafe.config.Config;

import java.io.File;

public class BaseProtocolTest {
    protected NetworkService networkService;
    protected IdentityService identityService;

    public void setup() {
        String appDir = OsUtils.getUserDataDir() + File.separator + getClass().getSimpleName();
        Config typesafeConfig = ConfigUtil.load("test", "bisq.networkServiceConfig");
        NetworkService.Config networkServiceConfig = NetworkServiceConfigFactory.getConfig(appDir, typesafeConfig);
        PersistenceService persistenceService = new PersistenceService(appDir);
        KeyPairService keyPairService = new KeyPairService(persistenceService);
        networkService = new NetworkService(networkServiceConfig, persistenceService, keyPairService);
        IdentityService.Config identityServiceConfig = IdentityService.Config.from(typesafeConfig.getConfig("identityServiceConfig"));
        identityService = new IdentityService(persistenceService, keyPairService, networkService, identityServiceConfig);
    }
}