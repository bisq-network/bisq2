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

package network.misq.protocol;


import network.misq.network.NetworkService;
import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MockServiceTransport extends NetworkService {
    private static final Logger log = LoggerFactory.getLogger(MockServiceTransport.class);
    private final Set<Node.Listener> listeners = ConcurrentHashMap.newKeySet();

    public MockServiceTransport() {
        super(null, null);
    }

    @Override
    public CompletableFuture<Boolean> bootstrap() {
        return CompletableFuture.completedFuture(true);
    }


    public CompletableFuture<Connection> confidentialSend(Message message, NetworkId peerNetworkId, KeyPair myKeyPair) {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
            future.complete(null);

            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
            listeners.forEach(e -> e.onMessage(message, null, "default"));
        }).start();

        return future;
    }


    @Override
    public void addMessageListener(Node.Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeMessageListener(Node.Listener listener) {
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }
}
