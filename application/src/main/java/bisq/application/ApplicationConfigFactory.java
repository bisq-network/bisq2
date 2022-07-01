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

package bisq.application;


import bisq.common.util.OsUtils;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class ApplicationConfigFactory {
    public static ApplicationConfig getConfig(Config typesafeConfig, String[] args) {
        String appName = "Bisq2";
        if (typesafeConfig.hasPath("appName")) {
            appName = typesafeConfig.getString("appName");
        }

        boolean devMode = typesafeConfig.getBoolean("devMode");

        // todo @alvasw can we use the typesafeConfig instead?
        String dataDir = null;
        boolean isBitcoindRegtest = false;
        boolean isElementsdRegtest = false;
        for (String arg : args) {
            if (arg.startsWith("--appName")) {
                appName = arg.split("=")[1];
            }

            if (arg.startsWith("--data-dir")) {
                dataDir = arg.split("=")[1];
            }

            if (arg.startsWith("--regtest-bitcoind")) {
                isBitcoindRegtest = true;
            }

            if (arg.startsWith("--regtest-elementsd")) {
                isElementsdRegtest = true;
            }
        }

        String appDir = dataDir == null ? OsUtils.getUserDataDir() + File.separator + appName : dataDir;
        log.info("Use application directory {}", appDir);

        String version = typesafeConfig.getString("version");
        
        return new ApplicationConfig(appDir, appName, version, devMode, isBitcoindRegtest, isElementsdRegtest);
    }
}