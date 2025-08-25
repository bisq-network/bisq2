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

package bisq.desktop.i2p_router;

import bisq.common.application.Service;
import bisq.common.util.NetworkUtils;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class I2pRouterAppService implements Service {
    private final I2pRouterProcessLauncher i2pRouterProcessLauncher;
    private final Config i2pConfig;

    public I2pRouterAppService(Config i2pConfig, Path baseDir) {
        this.i2pConfig = i2pConfig;
        i2pRouterProcessLauncher = new I2pRouterProcessLauncher(i2pConfig, baseDir);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        String i2cpHost = i2pConfig.getString("i2cpHost");
        int i2cpPort = i2pConfig.getInt("i2cpPort");
        if (NetworkUtils.isPortInUse(i2cpHost, i2cpPort)) {
            log.info("I2CP is already running, so we do not launch our router process");
            return CompletableFuture.completedFuture(true);
        }

        log.info("I2CP is not running. We start the i2p router application as new process");
        return i2pRouterProcessLauncher.start()
                .whenComplete((process, throwable) -> {
                    if (throwable != null) {
                        log.error("i2pRouterProcessLauncher.start failed", throwable);
                        shutdown();
                    } else {
                        log.info("We started the i2p router application as new Java process");
                    }
                })
                .thenApply(Objects::nonNull)
                .thenApply(e->{
                    try {
                        Thread.sleep(100000);
                    } catch (InterruptedException ea) {
                    }
                    return  e;
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return i2pRouterProcessLauncher.shutdown();
    }
}
