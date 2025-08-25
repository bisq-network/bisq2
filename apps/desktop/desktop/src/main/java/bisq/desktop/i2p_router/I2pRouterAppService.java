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

import bisq.application.ApplicationService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class I2pRouterAppService {
    private final I2pRouterProcessLauncher i2pRouterProcessLauncher;

    public I2pRouterAppService(ApplicationService.Config config) {
        String baseDir = config.getBaseDir().toAbsolutePath().toString();
        i2pRouterProcessLauncher = new I2pRouterProcessLauncher(baseDir);
    }

    public void start() {
        i2pRouterProcessLauncher.start()
                .whenComplete((process, throwable) -> {
                    if (throwable != null) {
                        log.error("i2pRouterProcessLauncher.start failed", throwable);
                        shutdown();
                    }
                });
        log.info("We start the i2p router application as new Java process");
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return i2pRouterProcessLauncher.shutdown();
    }
}
