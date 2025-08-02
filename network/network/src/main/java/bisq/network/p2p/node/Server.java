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

package bisq.network.p2p.node;

import bisq.common.network.Address;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.StringUtils;
import bisq.network.NetworkExecutors;
import bisq.network.p2p.node.transport.ServerSocketResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public final class Server {
    private final ServerSocket serverSocket;
    @Getter
    private final Address address;
    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    public final ExecutorService executor;

    Server(ServerSocketResult serverSocketResult,
           int socketTimeout,
           Consumer<Socket> socketHandler,
           Consumer<Exception> exceptionHandler) {
        serverSocket = serverSocketResult.getServerSocket();
        address = serverSocketResult.getAddress();
        log.debug("Create server: {}", serverSocketResult);
        executor = ExecutorFactory.newSingleThreadExecutor("Server.listen-" + StringUtils.truncate(serverSocketResult.getAddress(), 8));
        executor.submit(() -> {
            try {
                while (isNotStopped()) {
                    Socket socket = serverSocket.accept();
                    socket.setSoTimeout(socketTimeout);
                    log.debug("Accepted new connection on server: {}", serverSocketResult);
                    if (isNotStopped()) {
                        // Call handler on new thread
                        NetworkExecutors.getNetworkReadExecutor().submit(() -> socketHandler.accept(socket));
                    }
                }
            } catch (IOException e) {
                if (!isStopped.get()) {
                    exceptionHandler.accept(e);
                    shutdown();
                }
            } finally {
                shutdown();
            }
        });
    }

    void shutdown() {
        log.info("shutdown {}", address);
        if (isStopped.get()) {
            return;
        }
        isStopped.set(true);
        // future.cancel(true); would not do anything here as serverSocket.accept() does not respond to thread interrupts.
        try {
            serverSocket.close();
        } catch (IOException ignore) {
        }
        ExecutorFactory.shutdownAndAwaitTermination(executor);
    }

    private boolean isNotStopped() {
        return !isStopped.get() && !Thread.currentThread().isInterrupted();
    }
}
