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

import bisq.common.threading.ThreadName;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.common.network.Address;
import bisq.network.p2p.node.transport.ServerSocketResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Slf4j
public final class Server {
    private final ServerSocket serverSocket;
    @Getter
    private final Address address;
    private volatile boolean isStopped;
    private final Future<?> future;

    Server(ServerSocketResult serverSocketResult,
           Consumer<Socket> socketHandler,
           Consumer<Exception> exceptionHandler) {
        serverSocket = serverSocketResult.getServerSocket();
        address = serverSocketResult.getAddress();
        log.debug("Create server: {}", serverSocketResult);
        future = NetworkService.NETWORK_IO_POOL.submit(() -> {
            ThreadName.set(this, "listen-" + StringUtils.truncate(serverSocketResult.getAddress().toString()));
            try {
                while (isNotStopped()) {
                    Socket socket = serverSocket.accept();
                    log.debug("Accepted new connection on server: {}", serverSocketResult);
                    if (isNotStopped()) {
                        // Call handler on new thread
                        NetworkService.NETWORK_IO_POOL.submit(() -> {
                            ThreadName.set(this, "handle-" + StringUtils.truncate(serverSocketResult.getAddress().toString()));
                            socketHandler.accept(socket);
                        });
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

    void shutdown() {
        log.info("shutdown {}", address);
        if (isStopped) {
            return;
        }
        isStopped = true;
        future.cancel(true);
        try {
            serverSocket.close();
        } catch (IOException ignore) {
        }
    }

    private boolean isNotStopped() {
        return !isStopped && !Thread.currentThread().isInterrupted();
    }
}
