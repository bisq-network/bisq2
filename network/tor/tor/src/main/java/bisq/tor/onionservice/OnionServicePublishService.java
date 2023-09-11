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

package bisq.tor.onionservice;

import bisq.tor.TorIdentity;
import bisq.tor.controller.NativeTorController;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.TorControlConnection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class OnionServicePublishService {
    private static final String HS_DIR_NANE = "hiddenservice";

    private final NativeTorController nativeTorController;
    private final Path torDirPath;
    private final Map<String, CompletableFuture<OnionAddress>> onionAddressByNodeId = new HashMap<>();

    public OnionServicePublishService(NativeTorController nativeTorController, Path torDirPath) {
        this.nativeTorController = nativeTorController;
        this.torDirPath = torDirPath;
    }

    public synchronized CompletableFuture<OnionAddress> publish(String nodeId, int onionServicePort, int localPort) {
        boolean isNodeIdAlreadyPublished = onionAddressByNodeId.containsKey(nodeId);
        if (isNodeIdAlreadyPublished) {
            return onionAddressByNodeId.get(nodeId);
        }

        CompletableFuture<OnionAddress> completableFuture = new CompletableFuture<>();
        onionAddressByNodeId.put(nodeId, completableFuture);

        try {
            OnionServiceDataDirManager onionDataDirManager = new OnionServiceDataDirManager(
                    torDirPath.resolve(HS_DIR_NANE).resolve(nodeId)
            );
            Optional<String> privateKey = onionDataDirManager.getPrivateKey();
            TorControlConnection.CreateHiddenServiceResult jTorResult =
                    nativeTorController.createHiddenService(onionServicePort, localPort, privateKey);

            CreateHiddenServiceResult result = new CreateHiddenServiceResult(jTorResult);
            onionDataDirManager.persist(result);

            var onionAddress = new OnionAddress(jTorResult.serviceID + ".onion", onionServicePort);
            completableFuture.complete(onionAddress);

        } catch (IOException e) {
            log.error("Couldn't initialize nodeId {}", nodeId);
            completableFuture.completeExceptionally(e);
        }

        return completableFuture;
    }

    public synchronized CompletableFuture<Void> publish(TorIdentity torIdentity, int localPort) {
        try {
            Optional<String> privateKey = Optional.of(torIdentity.getPrivateKey());
            nativeTorController.createHiddenService(torIdentity.getPort(), localPort, privateKey);
            return CompletableFuture.completedFuture(null);

        } catch (IOException e) {
            log.error("Couldn't create onion service {}", torIdentity);
            return CompletableFuture.failedFuture(e);
        }
    }

    public synchronized Optional<OnionAddress> getOnionAddressForNode(String nodeId) {
        try {
            CompletableFuture<OnionAddress> completableFuture = onionAddressByNodeId.get(nodeId);
            if (completableFuture == null) {
                return Optional.empty();
            }

            OnionAddress onionAddress = completableFuture.get();
            return Optional.of(onionAddress);

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
