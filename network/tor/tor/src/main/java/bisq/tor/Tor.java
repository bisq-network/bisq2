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

package bisq.tor;

import bisq.common.util.FileUtils;
import bisq.tor.context.ReadOnlyTorContext;
import bisq.tor.onionservice.OnionServicePublishService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static java.io.File.separator;

/**
 * Open TODO:
 * - check <a href="https://github.com/ACINQ/Tor_Onion_Proxy_Library">...</a>
 * - support external running tor instance (external tor mode in netlayer)
 * - test use case with overriding existing torrc files
 * - test bridge and pluggable transports use cases
 * - test linux, windows
 * - check open TODOs
 * - test failure cases at start up (e.g. locked files, cookies remaining,...) specially on windows it seems that
 * current bisq has sometimes issues, we delete whole tor dir in those cases, but better to figure out when that
 * can happen.
 * <p>
 * Support for Android is not planned as long we do not target Android.
 */
@Slf4j
public class Tor {
    public static final String VERSION = "0.1.0";

    private final TorController torController;
    private final TorBootstrap torBootstrap;
    private final Path torDirPath;
    @Getter
    private final OnionServicePublishService onionServicePublishService;
    @Getter
    private final TorSocksProxyFactory torSocksProxyFactory;

    public Tor(Path torDirPath, ReadOnlyTorContext torContext) {
        this.torDirPath = torDirPath;
        this.torBootstrap = new TorBootstrap(torDirPath);
        this.torController = new TorController(torBootstrap.getCookieFile());
        this.onionServicePublishService = new OnionServicePublishService(torController, torDirPath);
        this.torSocksProxyFactory = new TorSocksProxyFactory(torContext);
    }

    public boolean startTor() {
        long ts = System.currentTimeMillis();
        try {
            int controlPort = torBootstrap.start();
            torController.start(controlPort);

            int proxyPort = torController.getProxyPort();
            torSocksProxyFactory.initialize(proxyPort);

        } catch (Exception exception) {
            torBootstrap.deleteVersionFile();
            log.error("Starting tor failed.", exception);
            shutdown();
            return false;
        }
        log.info(">> Starting Tor took {} ms", System.currentTimeMillis() - ts);
        return true;
    }

    public void shutdown() {
        log.info("Shutdown tor.");
        long ts = System.currentTimeMillis();

        torBootstrap.shutdown();
        torController.shutdown();

        log.info("Tor shutdown completed. Took {} ms.", System.currentTimeMillis() - ts); // Usually takes 20-40 ms
    }

    public Optional<String> getHostName(String serverId) {
        String fileName = torDirPath + separator + Constants.HS_DIR + separator + serverId + separator + "hostname";
        if (new File(fileName).exists()) {
            try {
                String host = FileUtils.readAsString(fileName);
                return Optional.of(host);
            } catch (IOException e) {
                log.error(e.toString(), e);
            }
        }
        return Optional.empty();
    }

    public boolean isHiddenServiceAvailable(String onionUrl) {
        return torController.isHiddenServiceAvailable(onionUrl);
    }
}
