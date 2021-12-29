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

package network.misq.network.p2p.services.peergroup.validateaddress;

import lombok.extern.slf4j.Slf4j;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.*;
import network.misq.network.p2p.services.peergroup.BanList;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static network.misq.network.NetworkService.NETWORK_IO_POOL;

@Slf4j
public class AddressValidationService implements Node.Listener {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    private final Node node;
    private final BanList banList;
    private final Map<String, AddressValidationHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private final Set<String> requesters = new CopyOnWriteArraySet<>(); // connectionIds

    public AddressValidationService(Node node, BanList banList) {
        this.node = node;
        this.banList = banList;
        this.node.addListener(this);
    }

    public CompletableFuture<Boolean> startAddressValidationProtocol(InboundConnection inboundConnection) {
        Address peerAddress = inboundConnection.getPeerAddress();
        String key = inboundConnection.getId();
        if (requestHandlerMap.containsKey(key)) {
            log.warn("Node {} has already an entry in requestHandlerMap for {}. " +
                    "This is expected if the past request has not been completed yet. We return.", node, peerAddress);
            return CompletableFuture.completedFuture(true);
        } else {
            log.debug("Node {} adds a new AddressValidationHandler for {}", node, peerAddress);
        }
        AddressValidationHandler handler = new AddressValidationHandler(node, peerAddress, banList);
        requestHandlerMap.put(key, handler);
        return handler.request()
                .orTimeout(TIMEOUT, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> {
                    requestHandlerMap.remove(key);
                    if (throwable == null) {
                        if (result) {
                            log.info("Node {} completed successfully address validation for inboundConnection from {}", node, peerAddress);
                            inboundConnection.setPeerAddressVerified(true);
                        } else {
                            log.warn("Node {} got a failed address validation for inboundConnection from {}. inboundConnection={}",
                                    node, peerAddress, inboundConnection);
                            node.closeConnection(inboundConnection, CloseReason.ADDRESS_VALIDATION_FAILED);
                        }
                    } else {
                        if (!(throwable instanceof CancellationException)) {
                            log.warn("Node {} got a failed address validation for inboundConnection from {}. exception {}. inboundConnection={}",
                                    node, peerAddress, throwable, inboundConnection);
                            node.closeConnection(inboundConnection, CloseReason.EXCEPTION.exception(throwable));
                        }
                    }
                });
    }

    public boolean isNotInProgress(Connection connection) {
        return !isInProgress(connection);
    }

    public boolean isInProgress(Connection connection) {
        return requestHandlerMap.containsKey(connection.getId()) && !requesters.contains(connection.getId());
    }

    public void shutdown() {
        requestHandlerMap.values().forEach(AddressValidationHandler::dispose);
        requestHandlerMap.clear();
    }

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (message instanceof AddressValidationRequest addressValidationRequest) {
            Address peerAddress = connection.getPeerAddress();
            if (connection instanceof InboundConnection inboundConnection) {
                log.debug("Node {} received AddressValidationRequest with nonce {} from {}", node, addressValidationRequest.nonce(), peerAddress);
                requesters.add(connection.getId());
                NETWORK_IO_POOL.submit(() -> node.send(new AddressValidationResponse(addressValidationRequest.nonce()), inboundConnection));
                log.debug("Node {} sent AddressValidationResponse with nonce {} to {}. Connection={}", node, addressValidationRequest.nonce(), peerAddress, inboundConnection.getId());
            } else {
                log.warn("Node {}  got a AddressValidationRequest at {}. We expect an inbound connection. We close that connection.", node, connection);
                banList.add(peerAddress, BanList.Reason.ADDRESS_VALIDATION_REQUEST_ON_OUTBOUND_CON);
                node.closeConnection(connection, CloseReason.ADDRESS_VALIDATION_FAILED);
            }
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        String key = connection.getId();
        if (requestHandlerMap.containsKey(key)) {
            requestHandlerMap.get(key).dispose();
            requestHandlerMap.remove(key);
        }
        requesters.remove(key);
    }
}