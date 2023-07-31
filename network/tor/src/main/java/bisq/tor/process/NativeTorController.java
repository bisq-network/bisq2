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

package bisq.tor.process;

import bisq.tor.ClientTorrcGenerator;
import bisq.tor.bootstrap.BootstrapEvent;
import bisq.tor.bootstrap.BootstrapEventHandler;
import bisq.tor.bootstrap.BootstrapEventListener;
import bisq.tor.bootstrap.TorBootstrapFailed;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.PasswordDigest;
import net.freehaven.tor.control.TorControlConnection;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NativeTorController implements BootstrapEventListener {

    private final CountDownLatch isBootstrappedCountdownLatch = new CountDownLatch(1);
    private final BootstrapEventHandler bootstrapEventHandler = new BootstrapEventHandler();
    private Optional<TorControlConnection> torControlConnection = Optional.empty();

    public void connect(int controlPort, PasswordDigest controlConnectionSecret) throws IOException {
        var controlSocket = new Socket("127.0.0.1", controlPort);
        var controlConnection = new TorControlConnection(controlSocket);
        controlConnection.launchThread(true);
        controlConnection.authenticate(controlConnectionSecret.getSecret());
        torControlConnection = Optional.of(controlConnection);
    }

    public void bindTorToConnection() throws IOException {
        TorControlConnection controlConnection = torControlConnection.orElseThrow();
        controlConnection.takeOwnership();
        controlConnection.resetConf(NativeTorProcess.ARG_OWNER_PID);
    }

    public void enableTorNetworking() throws IOException {
        TorControlConnection controlConnection = torControlConnection.orElseThrow();
        addBootstrapEventListener(controlConnection);
        controlConnection.setConf(ClientTorrcGenerator.DISABLE_NETWORK_CONFIG_KEY, "0");
    }

    public void waitUntilBootstrapped() {
        try {
            boolean isSuccess = isBootstrappedCountdownLatch.await(2, TimeUnit.MINUTES);
            if (!isSuccess) {
                throw new TorBootstrapFailed("Tor bootstrap timout (2 minutes) triggered.");
            }
        } catch (InterruptedException e) {
            throw new TorBootstrapFailed(e);
        }
    }

    public void shutdown() throws IOException {
        TorControlConnection controlConnection = torControlConnection.orElseThrow();
        controlConnection.shutdownTor("SHUTDOWN");
    }

    @Override
    public void onBootstrapStatusEvent(BootstrapEvent bootstrapEvent) {
        log.info("Tor bootstrap event: {}", bootstrapEvent);
        if (bootstrapEvent.isDoneEvent()) {
            isBootstrappedCountdownLatch.countDown();
            removeBootstrapEventListener();
        }
    }

    private void addBootstrapEventListener(TorControlConnection controlConnection) throws IOException {
        bootstrapEventHandler.addListener(this);
        controlConnection.setEventHandler(bootstrapEventHandler);
        controlConnection.setEvents(List.of("STATUS_CLIENT"));
    }

    private void removeBootstrapEventListener() {
        TorControlConnection controlConnection = torControlConnection.orElseThrow();
        bootstrapEventHandler.removeListener(this);

        controlConnection.setEventHandler(null);
        try {
            controlConnection.setEvents(Collections.emptyList());
        } catch (IOException e) {
            throw new IllegalStateException("Can't set tor events.");
        }
    }
}
