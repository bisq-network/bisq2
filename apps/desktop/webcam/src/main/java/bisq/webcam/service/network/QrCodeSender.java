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

import bisq.common.timer.Scheduler;
import bisq.common.webcam.ControlSignals;
import bisq.webcam.service.ErrorCode;
import bisq.webcam.service.WebcamException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static bisq.common.encoding.NonPrintingCharacters.UNIT_SEPARATOR;

@Slf4j
public class QrCodeSender {
    private static final long SEND_HEART_BEAT_INTERVAL = TimeUnit.SECONDS.toMillis(1);
    private static final long SEND_MSG_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    private final InetSocketAddress serverAddress;
    private Optional<Scheduler> heartBeatScheduler = Optional.empty();

    public QrCodeSender(int port) {
        serverAddress = new InetSocketAddress("127.0.0.1", port);
    }

    public void startSendingHeartBeat() {
        // Send a heart beat every second to avoid triggering server socket timeout
        heartBeatScheduler = Optional.of(Scheduler.run(() -> send(ControlSignals.HEART_BEAT))
                .periodically(SEND_HEART_BEAT_INTERVAL));
    }

    public void shutdown() {
        heartBeatScheduler.ifPresent(Scheduler::stop);
    }

    public CompletableFuture<Void> send(ControlSignals controlSignal) {
        return doSend(UNIT_SEPARATOR.getNonPrintingChar() + controlSignal.name());
    }

    public CompletableFuture<Void> send(ControlSignals controlSignal, String message) {
        return doSend(UNIT_SEPARATOR.getNonPrintingChar() + controlSignal.name() + UNIT_SEPARATOR.getNonPrintingChar() + message);
    }

    private CompletableFuture<Void> doSend(String message) {
        log.info("send {} to {}", message, serverAddress);
        return CompletableFuture.runAsync(() -> {
            try (Socket socket = new Socket();) {
                socket.connect(serverAddress);
                try (PrintWriter printWriter = new PrintWriter(socket.getOutputStream())) {
                    printWriter.println(message);
                }
            } catch (IOException e) {
                log.error("Error at sending qrCode {} to {}", message, serverAddress, e);
                throw new WebcamException(ErrorCode.IO_EXCEPTION, e);
            }
        }).orTimeout(SEND_MSG_TIMEOUT, TimeUnit.MILLISECONDS);
    }
}
