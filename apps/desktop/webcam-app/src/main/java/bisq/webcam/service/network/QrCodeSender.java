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

package bisq.webcam.service.network;

import bisq.common.threading.ExecutorFactory;
import bisq.common.timer.Scheduler;
import bisq.common.webcam.WebcamControlSignals;
import bisq.common.webcam.WebcamIpcMessageSerializer;
import bisq.common.webcam.WebcamIpcWireMessage;
import bisq.webcam.service.ErrorCode;
import bisq.webcam.service.WebcamException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class QrCodeSender {
    private static final long SEND_HEART_BEAT_INTERVAL = TimeUnit.SECONDS.toMillis(1);
    private static final long SEND_MSG_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final int SOCKET_CONNECT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(2);

    private final InetSocketAddress serverAddress;
    private final String sessionSecret;
    private Optional<Scheduler> heartBeatScheduler = Optional.empty();
    private final ExecutorService executorService = ExecutorFactory.newCachedThreadPool("QrCodeSender");

    public QrCodeSender(int port, String sessionSecret) {
        checkArgument(sessionSecret != null && !sessionSecret.isBlank(), "Missing webcam IPC session secret");
        serverAddress = new InetSocketAddress("127.0.0.1", port);
        this.sessionSecret = sessionSecret;
    }

    public void startSendingHeartBeat() {
        // Send a heart beat every second to avoid triggering server socket timeout
        heartBeatScheduler = Optional.of(Scheduler.run(() -> send(WebcamControlSignals.HEART_BEAT))
                .host(this)
                .runnableName("sendHeartBeat")
                .periodically(SEND_HEART_BEAT_INTERVAL));
    }

    public void shutdown() {
        heartBeatScheduler.ifPresent(Scheduler::stop);
        heartBeatScheduler = Optional.empty();
        executorService.shutdownNow();
    }

    public CompletableFuture<Void> send(WebcamControlSignals controlSignal) {
        return doSend(controlSignal, Optional.empty());
    }

    public CompletableFuture<Void> send(WebcamControlSignals controlSignal, String payload) {
        if (payload == null) {
            return CompletableFuture.failedFuture(new NullPointerException("payload must not be null"));
        }
        return doSend(controlSignal, Optional.of(payload));
    }

    private CompletableFuture<Void> doSend(WebcamControlSignals controlSignal, Optional<String> payload) {
        int payloadByteLength = payload.map(value -> value.getBytes(StandardCharsets.UTF_8).length).orElse(0);
        WebcamIpcWireMessage wireMessage;
        try {
            wireMessage = WebcamIpcWireMessage.create(sessionSecret, controlSignal, payload);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }

        log.info("send {} message with payloadByteLength={}", controlSignal, payloadByteLength);
        return CompletableFuture.runAsync(() -> {
                    try (Socket socket = new Socket()) {
                        socket.connect(serverAddress, SOCKET_CONNECT_TIMEOUT);
                        WebcamIpcMessageSerializer.writeFrame(socket.getOutputStream(), wireMessage);
                    } catch (IOException e) {
                        log.error("Error at sending {} message with payloadByteLength={}",
                                controlSignal, payloadByteLength, e);
                        throw new WebcamException(ErrorCode.IO_EXCEPTION, e);
                    }
                }, executorService)
                .orTimeout(SEND_MSG_TIMEOUT, TimeUnit.MILLISECONDS);
    }
}
