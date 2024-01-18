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

package bisq.network.i2p;

import lombok.extern.slf4j.Slf4j;
import net.i2p.I2PException;
import net.i2p.client.streaming.I2PServerSocket;
import net.i2p.client.streaming.I2PSocket;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class I2pServer {
    private final I2PServerSocket serverSocket;
    private final Consumer<I2PSocket> socketConsumer;
    private final AtomicBoolean isRunning = new AtomicBoolean();
    private Optional<Thread> serverThread = Optional.empty();

    public I2pServer(I2PServerSocket serverSocket, Consumer<I2PSocket> socketConsumer) {
        this.serverSocket = serverSocket;
        this.socketConsumer = socketConsumer;
    }

    public void start() {
        isRunning.set(true);

        Thread serverThread = new Thread(() -> {
            while (isRunning.get()) {
                try {
                    @SuppressWarnings("resource")
                    I2PSocket socket = serverSocket.accept();
                    if (socket != null) {
                        new Thread(() -> socketConsumer.accept(socket)).start();
                    }

                } catch (I2PException | IOException e) {
                    log.warn("Couldn't accept client connection.", e);
                }
            }
        });
        this.serverThread = Optional.of(serverThread);

        serverThread.start();
    }

    public void shutdown() {
        isRunning.set(false);
        try {
            serverSocket.close();
            waitForServerThreadShutdown();
        } catch (I2PException e) {
            log.warn("Couldn't close server socket.", e);
        }
    }

    private void waitForServerThreadShutdown() {
        serverThread.ifPresent(thread -> {
            long timeout = TimeUnit.SECONDS.toMillis(30);
            try {
                thread.join(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
