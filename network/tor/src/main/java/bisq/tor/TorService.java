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

import bisq.common.application.Service;
import bisq.tor.context.TorContext;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class TorService implements Service {
    private static final int RANDOM_PORT = 0;

    private final ExecutorService executorService;
    private final Tor tor;
    private final TorContext context = new TorContext();
    private final OnionServicePublishService onionPublishService;
    private final TorSocksProxyFactory torSocksProxyFactory;

    public TorService(ExecutorService executorService, Path torDirPath) {
        this.executorService = executorService;
        this.tor = new Tor(torDirPath, context);
        this.onionPublishService = tor.getOnionServicePublishService();
        this.torSocksProxyFactory = tor.getTorSocksProxyFactory();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("Tor.shutdownHook");
            shutdown();
        }));
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(
                () -> {
                    var retryPolicy = RetryPolicy.<Boolean>builder()
                            .handle(IllegalStateException.class)
                            .handleResultIf(result -> context.getState() == TorContext.State.STARTING)
                            .withBackoff(Duration.ofSeconds(3), Duration.ofSeconds(30))
                            .withJitter(0.25)
                            .withMaxDuration(Duration.ofMinutes(5)).withMaxRetries(30)
                            .onRetry(e -> log.info("Retry. AttemptCount={}.", e.getAttemptCount()))
                            .onRetriesExceeded(e -> {
                                log.warn("Failed. Max retries exceeded. We shutdown.");
                                shutdown();
                            })
                            .onSuccess(e -> log.debug("Succeeded."))
                            .build();

                    return Failsafe.with(retryPolicy).get(this::start);
                },
                executorService
        );
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        TorContext.State previousState = context.getAndUpdateStateAtomically(
                state -> state.isStartingOrRunning() ? TorContext.State.STOPPING : state
        );

        if (!previousState.isStartingOrRunning()) {
            return CompletableFuture.completedFuture(true);
        }

        onionPublishService.shutdown();
        tor.shutdown();
        context.setState(TorContext.State.TERMINATED);
        return CompletableFuture.completedFuture(true);
    }

    private boolean start() {
        TorContext.State previousState = context.compareAndExchangeState(TorContext.State.NEW, TorContext.State.STARTING);
        switch (previousState) {
            case NEW: {
                boolean isSuccess = tor.startTor();
                context.setState(TorContext.State.RUNNING);
                return isSuccess;
            }
            case STARTING: {
                throw new IllegalStateException("Already starting.");
            }
            case RUNNING: {
                log.debug("Got called while already running. We ignore that call.");
                return true;
            }
            case STOPPING:
            case TERMINATED:
                return false;
            default: {
                throw new IllegalStateException("Unhandled state " + previousState);
            }
        }
    }

    public CompletableFuture<CreateOnionServiceResponse> createOnionService(int port, String nodeId) {
        log.info("Start hidden service with port {} and nodeId {}", port, nodeId);
        long ts = System.currentTimeMillis();
        try {
            @SuppressWarnings("resource") ServerSocket localServerSocket = new ServerSocket(RANDOM_PORT);
            int localPort = localServerSocket.getLocalPort();

            return onionPublishService.initialize(nodeId, port, localPort)
                    .thenApply(onionAddress -> {
                                log.info("Tor hidden service Ready. Took {} ms. Onion address={}; nodeId={}",
                                        System.currentTimeMillis() - ts, onionAddress, nodeId);
                                return new CreateOnionServiceResponse(nodeId, localServerSocket, onionAddress);
                            }
                    );

        } catch (IOException e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

    public boolean isOnionServiceOnline(String onionUrl) {
        return tor.isHiddenServiceAvailable(onionUrl);
    }

    public Optional<String> getHostName(String serverId) {
        return tor.getHostName(serverId);
    }

    public Socket getSocket(String streamId) throws IOException {
        return torSocksProxyFactory.getSocket(streamId);
    }

    public Socks5Proxy getSocks5Proxy(String streamId) throws IOException {
        return torSocksProxyFactory.getSocks5Proxy(streamId);
    }
}
