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

package bisq.tor.controller;

import bisq.tor.ClientTorrcGenerator;
import bisq.tor.controller.events.ControllerEventHandler;
import bisq.tor.process.NativeTorProcess;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.PasswordDigest;
import net.freehaven.tor.control.TorControlConnection;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
public class NativeTorController implements BootstrapEventListener, HsDescUploadedEventListener {

    private final CountDownLatch isBootstrappedCountdownLatch = new CountDownLatch(1);
    private final CountDownLatch isHsDescUploadedCountdownLatch = new CountDownLatch(1);
    private final ControllerEventHandler controllerEventHandler = new ControllerEventHandler();
    private final CompletableFuture<String> hiddenServiceAddress = new CompletableFuture<>();
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

    public TorControlConnection.CreateHiddenServiceResult createHiddenService(
            int hiddenServicePort, int localPort, Optional<String> privateKey) throws IOException {
        TorControlConnection controlConnection = torControlConnection.orElseThrow();

        controllerEventHandler.addHsDescUploadedListener(this);
        try {
            controlConnection.setEvents(List.of("HS_DESC"));
        } catch (IOException e) {
            throw new IllegalStateException("Can't set tor events.");
        }

        TorControlConnection.CreateHiddenServiceResult result;
        if (privateKey.isEmpty()) {
            result = controlConnection.createHiddenService(hiddenServicePort, localPort);
        } else {
            result = controlConnection.createHiddenService(hiddenServicePort, localPort, privateKey.get());
        }
        hiddenServiceAddress.complete(result.serviceID);

        try {
            boolean isSuccess = isHsDescUploadedCountdownLatch.await(2, TimeUnit.MINUTES);
            if (isSuccess) {
                removeHsDescUploadedEventListener();
            } else {
                throw new HsDescUploadFailedException("HS_DESC upload timeout (2 minutes) triggered.");
            }
        } catch (InterruptedException e) {
            throw new HsDescUploadFailedException(e);
        }

        return result;
    }

    public boolean isHiddenServiceAvailable(String onionUrl) {
        try {
            TorControlConnection controlConnection = torControlConnection.orElseThrow();
            return controlConnection.isHSAvailable(onionUrl);
        } catch (IOException e) {
            return false;
        }
    }

    public void waitUntilBootstrapped() {
        try {
            boolean isSuccess = isBootstrappedCountdownLatch.await(2, TimeUnit.MINUTES);
            if (isSuccess) {
                removeBootstrapEventListener();
            } else {
                throw new TorBootstrapFailedException("Tor bootstrap timout (2 minutes) triggered.");
            }
        } catch (InterruptedException e) {
            throw new TorBootstrapFailedException(e);
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
        }
    }

    @Override
    public void onHsDescUploaded(HsDescUploadedEvent uploadedEvent) {
        log.info("Tor HS_DESC event: {}", uploadedEvent);
        try {
            String hsAddress = hiddenServiceAddress.get(2, TimeUnit.MINUTES);
            if (hsAddress.equals(uploadedEvent.getHsAddress())) {
                isHsDescUploadedCountdownLatch.countDown();
            }
        } catch (TimeoutException e) {
            throw new HsDescUploadFailedException("Unknown hidden service descriptor uploaded");
        } catch (ExecutionException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void addBootstrapEventListener(TorControlConnection controlConnection) throws IOException {
        controllerEventHandler.addBootstrapListener(this);
        controlConnection.setEventHandler(controllerEventHandler);
        controlConnection.setEvents(List.of("STATUS_CLIENT"));
    }

    private void removeBootstrapEventListener() {
        TorControlConnection controlConnection = torControlConnection.orElseThrow();
        controllerEventHandler.removeBootstrapListener(this);

        try {
            controlConnection.setEvents(Collections.emptyList());
        } catch (IOException e) {
            throw new IllegalStateException("Can't set tor events.");
        }
    }

    private void removeHsDescUploadedEventListener() {
        TorControlConnection controlConnection = torControlConnection.orElseThrow();
        controllerEventHandler.removeHsDescUploadedListener(this);

        try {
            controlConnection.setEvents(Collections.emptyList());
        } catch (IOException e) {
            throw new IllegalStateException("Can't set tor events.");
        }
    }
}
