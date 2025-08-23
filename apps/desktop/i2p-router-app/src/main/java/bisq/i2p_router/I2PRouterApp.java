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

package bisq.i2p_router;

import bisq.common.platform.PlatformUtils;
import bisq.network.i2p.router.I2pRouter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class I2PRouterApp {
    public static void main(String[] args) {
        String i2pDirPath = PlatformUtils.getUserDataDir().resolve("bisq2_i2p_embedded_router").toString();
        I2pRouter router = new I2pRouter(i2pDirPath);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("I2pRouter.shutdownHook");
            router.shutdown();
        }));

        router.start()
                .orTimeout(3, TimeUnit.MINUTES)
                .whenComplete((result, throwable) -> {
                    if (throwable != null || !result) {
                        router.shutdown();
                        log.error("I2P router failed to start, exiting.");
                        System.exit(1);
                    }
                });

        keepRunning();
    }

    private static void keepRunning() {
        try {
            // Keep running
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.warn("Thread got interrupted at keepRunning method", e);
            Thread.currentThread().interrupt(); // Restore interrupted state
        }
    }
}
