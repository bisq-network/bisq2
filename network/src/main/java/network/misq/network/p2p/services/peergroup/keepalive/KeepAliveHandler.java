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

package network.misq.network.p2p.services.peergroup.keepalive;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
class KeepAliveHandler implements Connection.Listener {
    private static final long TIMEOUT = 90;

    private final Node node;
    private final Connection connection;
    private final CompletableFuture<Void> future = new CompletableFuture<>();
    private final int nonce;

    KeepAliveHandler(Node node, Connection connection) {
        this.node = node;
        this.connection = connection;

        nonce = new Random().nextInt();
        connection.addListener(this);
    }

    CompletableFuture<Void> request() {
        future.orTimeout(TIMEOUT, TimeUnit.SECONDS);
        log.info("Node {} send Ping to {} with nonce {}. Connection={}",
                node, connection.getPeerAddress(), nonce, connection.getId());
        node.send(new Ping(nonce), connection)
                .whenComplete((c, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                        dispose();
                    }
                });
        return future;
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof Pong pong) {
            if (pong.requestNonce() == nonce) {
                log.info("Node {} received Pong from {} with nonce {}. Connection={}",
                        node, connection.getPeerAddress(), pong.requestNonce(), connection.getId());
                removeListeners();
                future.complete(null);
            } else {
                log.warn("Node {} received Pong from {} with invalid nonce {}. Request nonce was {}. Connection={}",
                        node, connection.getPeerAddress(), pong.requestNonce(), nonce, connection.getId());
            }
        }
    }

    @Override
    public void onConnectionClosed() {
        dispose();
    }

    void dispose() {
        removeListeners();
        future.cancel(true);
    }

    private void removeListeners() {
        connection.removeListener(this);
    }
}