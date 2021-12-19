/*
 * This file is part of Misq.
 *
 * Misq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Misq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Misq. If not, see <http://www.gnu.org/licenses/>.
 */

package network.misq.network.p2p.node;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.threading.ExecutorFactory;
import network.misq.common.util.StringUtils;
import network.misq.network.p2p.node.transport.Transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
public final class Server {
    private final ServerSocket serverSocket;
    @Getter
    private final Address address;
    private volatile boolean isStopped;
    private static BlockingQueue<Runnable> q = new ArrayBlockingQueue<>(20);
    private static ThreadPoolExecutor ex = new ThreadPoolExecutor(4, 10, 20, TimeUnit.SECONDS, q);

    Server(Transport.ServerSocketResult serverSocketResult, Consumer<Socket> socketHandler, Consumer<Exception> exceptionHandler) {
        serverSocket = serverSocketResult.serverSocket();
        address = serverSocketResult.address();
        log.debug("Create server: {}", serverSocketResult);
        ExecutorFactory.IO_POOL.execute(() -> {
            Thread.currentThread().setName("Server-" +
                    StringUtils.truncate(serverSocketResult.nodeId()) + "-" +
                    StringUtils.truncate(serverSocketResult.address().toString()));
            try {
                while (isNotStopped()) {
                    Socket socket = serverSocket.accept();
                    log.debug("Accepted new connection on server: {}", serverSocketResult);
                    if (isNotStopped()) {
                        socketHandler.accept(socket);
                    }
                }
            } catch (IOException e) {
                if (!isStopped) {
                    exceptionHandler.accept(e);
                    shutdown();
                }
            }
        });
    }

    CompletableFuture<Void> shutdown() {
        log.info("shutdown {}", address);
        if (isStopped) {
            return CompletableFuture.completedFuture(null);
        }
        isStopped = true;
        return CompletableFuture.runAsync(() -> {
            try {
                serverSocket.close();
            } catch (IOException ignore) {
            }
        });
    }

    private boolean isNotStopped() {
        return !isStopped && !Thread.currentThread().isInterrupted();
    }
}
