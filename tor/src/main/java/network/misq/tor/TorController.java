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

package network.misq.tor;

import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.TorControlConnection;
import network.misq.common.util.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static network.misq.tor.Constants.*;

@Slf4j
public class TorController {

    private final File cookieFile;
    @Nullable
    private TorControlConnection torControlConnection;
    @Nullable
    private Socket controlSocket;
    private final TorEventHandler torEventHandler = new TorEventHandler();
    private boolean isStarted;
    private volatile boolean isStopped;
    private volatile boolean isTorEventHandlerSet;
    private final Object isTorEventHandlerSetLock = new Object();

    TorController(File cookieFile) {
        this.cookieFile = cookieFile;
    }

    void start(int controlPort) throws IOException {
        isStopped = false;
        controlSocket = new Socket("127.0.0.1", controlPort);
        torControlConnection = new TorControlConnection(controlSocket);
        torControlConnection.authenticate(FileUtils.asBytes(cookieFile));
        torControlConnection.setEvents(CONTROL_EVENTS);
        torControlConnection.takeOwnership();
        torControlConnection.resetConf(CONTROL_RESET_CONF);
        torControlConnection.setConf(CONTROL_DISABLE_NETWORK, "0");

        Set<String> loggedStatusMessages = new HashSet<>();
        while (!isStopped && !Thread.interrupted()) {
            String status = torControlConnection.getInfo(CONTROL_STATUS_BOOTSTRAP_PHASE);
            log.debug("Listen on bootstrap progress: >> {}", status);
            if (status != null && !loggedStatusMessages.contains(status)) {
                log.info("Listen on bootstrap progress: >> {}", status);
                loggedStatusMessages.add(status);
            }

            if (status != null && status.contains("PROGRESS=100")) {
                break;
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {
                }
            }
        }
        isStarted = true;
    }

    void shutdown() {
        isStopped = true;

        try {
            if (torControlConnection != null) {
                torControlConnection.setConf(CONTROL_DISABLE_NETWORK, "1");
                torControlConnection.shutdownTor("TERM");
            }
        } catch (IOException e) {
            log.error(e.toString(), e);
        } finally {
            try {
                if (controlSocket != null) {
                    try {
                        controlSocket.close();
                    } catch (IOException ignore) {
                    }
                }
            } finally {
                controlSocket = null;
                torControlConnection = null;
                isTorEventHandlerSet = false;
            }
        }
    }

    int getProxyPort() throws IOException {
        assertState();
        String socksInfo = torControlConnection().getInfo(CONTROL_NET_LISTENERS_SOCKS);
        socksInfo = socksInfo.replace("\"", "");
        String[] tokens = socksInfo.split(":");
        String port = tokens[tokens.length - 1];
        return Integer.parseInt(port);
    }

    TorControlConnection.CreateHiddenServiceResult createHiddenService(int hiddenServicePort,
                                                                       int localPort) throws IOException {
        assertState();
        return torControlConnection().createHiddenService(hiddenServicePort, localPort);
    }

    TorControlConnection.CreateHiddenServiceResult createHiddenService(int hiddenServicePort,
                                                                       int localPort,
                                                                       String privateKey) throws IOException {
        assertState();
        return torControlConnection().createHiddenService(hiddenServicePort, localPort, privateKey);
    }

    void destroyHiddenService(String serviceId) throws IOException {
        if (!isStopped) {
            torControlConnection().destroyHiddenService(serviceId);
        }
    }

    private void assertState() {
        checkArgument(isStarted, "Startup not completed");
        checkArgument(!isStopped, "Shutdown called already");
    }

    private TorControlConnection torControlConnection() {
        return checkNotNull(torControlConnection);
    }

    public void addHiddenServiceReadyListener(String serviceId, Runnable listener) {
        // We set it on demand once needed, but ensure its not overwritten in case we use multiple servers for the
        // same tor instance.
        synchronized (isTorEventHandlerSetLock) {
            if (torControlConnection != null && !isTorEventHandlerSet) {
                isTorEventHandlerSet = true;
                torControlConnection.setEventHandler(torEventHandler);
            }
        }

        torEventHandler.addHiddenServiceReadyListener(serviceId, listener);
    }

    public void removeHiddenServiceReadyListener(String serviceId) {
        torEventHandler.removeHiddenServiceReadyListener(serviceId);
    }
}
