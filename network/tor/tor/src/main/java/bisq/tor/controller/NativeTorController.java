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

import bisq.tor.TorrcClientConfigFactory;
import bisq.tor.controller.events.ControllerEventHandler;
import bisq.tor.controller.events.events.BootstrapEvent;
import bisq.tor.controller.events.events.HsDescUploadedEvent;
import bisq.tor.controller.events.listener.BootstrapEventListener;
import bisq.tor.controller.events.listener.HsDescUploadedEventListener;
import bisq.tor.controller.exceptions.HsDescUploadFailedException;
import bisq.tor.controller.exceptions.TorBootstrapFailedException;
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

    public synchronized void connect(int controlPort, PasswordDigest controlConnectionSecret) {
        try {
            var controlSocket = new Socket("127.0.0.1", controlPort);
            var controlConnection = new TorControlConnection(controlSocket);
            controlConnection.launchThread(true);
            controlConnection.authenticate(controlConnectionSecret.getSecret());
            torControlConnection = Optional.of(controlConnection);
        } catch (IOException e) {
            throw new ControlCommandFailedException("Couldn't connect to control port.", e);
        }
    }

    public void bindTorToConnection() {
        try {
            TorControlConnection controlConnection = torControlConnection.orElseThrow();
            controlConnection.takeOwnership();
            controlConnection.resetConf(NativeTorProcess.ARG_OWNER_PID);
        } catch (IOException e) {
            throw new ControlCommandFailedException("Couldn't bind Tor to control connection.", e);
        }
    }

    public void enableTorNetworking() {
        try {
            TorControlConnection controlConnection = torControlConnection.orElseThrow();
            addBootstrapEventListener(controlConnection);
            controlConnection.setConf(TorrcClientConfigFactory.DISABLE_NETWORK_CONFIG_KEY, "0");
        } catch (IOException e) {
            throw new ControlCommandFailedException("Couldn't enable Tor networking.", e);
        }
    }

    public TorControlConnection.CreateHiddenServiceResult createHiddenService(
            int hiddenServicePort, int localPort, Optional<String> privateKey) throws IOException {
        TorControlConnection controlConnection = torControlConnection.orElseThrow();

        controllerEventHandler.addHsDescUploadedListener(this);
        setEventSubscriptionsOnConnection(controlConnection, List.of("HS_DESC"));

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

    public synchronized void shutdown() {
        try {
            TorControlConnection controlConnection = torControlConnection.orElseThrow();
            controlConnection.shutdownTor("SHUTDOWN");
        } catch (IOException e) {
            throw new ControlCommandFailedException("Couldn't send shutdown command to Tor.", e);
        }
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

    private void setEventSubscriptionsOnConnection(TorControlConnection controlConnection, List<String> events) {
        try {
            controlConnection.setEvents(events);
        } catch (IOException e) {
            throw new IllegalStateException("Can't set tor events.");
        }
    }

    private void clearAllEventSubscriptionsOnConnection(TorControlConnection controlConnection) {
        setEventSubscriptionsOnConnection(controlConnection, Collections.emptyList());
    }

    private void addBootstrapEventListener(TorControlConnection controlConnection) {
        controllerEventHandler.addBootstrapListener(this);
        controlConnection.setEventHandler(controllerEventHandler);
        setEventSubscriptionsOnConnection(controlConnection, List.of("STATUS_CLIENT"));
    }

    private void removeBootstrapEventListener() {
        TorControlConnection controlConnection = torControlConnection.orElseThrow();
        controllerEventHandler.removeBootstrapListener(this);
        clearAllEventSubscriptionsOnConnection(controlConnection);
    }

    private void removeHsDescUploadedEventListener() {
        TorControlConnection controlConnection = torControlConnection.orElseThrow();
        controllerEventHandler.removeHsDescUploadedListener(this);
        clearAllEventSubscriptionsOnConnection(controlConnection);
    }
}
