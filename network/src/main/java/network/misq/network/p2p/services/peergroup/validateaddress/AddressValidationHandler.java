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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.*;
import network.misq.network.p2p.services.peergroup.Quarantine;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
class AddressValidationHandler implements Connection.Listener {
    private static final long TIMEOUT = 90;

    private final Node node;
    private final Address addressOfInboundConnection;
    private final Quarantine quarantine;
    private final CompletableFuture<Boolean> future = new CompletableFuture<>();
    private final int nonce;
    @Nullable
    private OutboundConnection outboundConnection;

    AddressValidationHandler(Node node, Address addressOfInboundConnection, Quarantine quarantine) {
        this.node = node;
        this.addressOfInboundConnection = addressOfInboundConnection;
        this.quarantine = quarantine;
        nonce = new Random().nextInt();
    }

    CompletableFuture<Boolean> request() {
        future.orTimeout(TIMEOUT, TimeUnit.SECONDS);
        log.debug("Node {} send ConfirmAddressRequest to {} with nonce {}",
                node, addressOfInboundConnection, nonce);
        node.send(new AddressValidationRequest(nonce), addressOfInboundConnection)
                .whenComplete((connection, throwable) -> {
                    if (throwable == null) {
                        if (connection instanceof OutboundConnection outboundConnection) {
                            this.outboundConnection = outboundConnection;
                            outboundConnection.addListener(this);
                        } else {
                            future.completeExceptionally(new Exception("Connection must be OutboundConnection"));
                            dispose();
                        }
                    } else {
                        future.completeExceptionally(throwable);
                        dispose();
                    }
                });
        return future;
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof AddressValidationResponse addressValidationResponse) {
            Objects.requireNonNull(outboundConnection);
            if (addressValidationResponse.requestNonce() == nonce &&
                    outboundConnection.getPeerAddress().equals(addressOfInboundConnection)) {
                log.debug("Node {} received valid AddressValidationResponse from {}",
                        node, addressOfInboundConnection);
                node.send(new CloseConnectionMessage(CloseConnectionMessage.Reason.ADDRESS_VALIDATION_COMPLETED), outboundConnection);
                removeListeners();
                future.complete(true);
            } else {
                log.warn("Node {} received an invalid AddressValidationResponse from {}." +
                                "Response nonce: {}. Request nonce: {}. " +
                                "connection.getPeerAddress()={}. address={}",
                        node, addressOfInboundConnection,
                        addressValidationResponse.requestNonce(), nonce,
                        outboundConnection.getPeerAddress(), addressOfInboundConnection);
                quarantine.add(addressOfInboundConnection, Quarantine.Reason.ADDRESS_VALIDATION_FAILED);
                quarantine.add(outboundConnection.getPeerAddress(), Quarantine.Reason.ADDRESS_VALIDATION_FAILED);
                node.closeConnection(outboundConnection);
                removeListeners();
                future.complete(false);
            }
        }
    }

    @Override
    public void onConnectionClosed() {
        log.debug("Node {} got called onDisconnect. outboundConnection={}", node, outboundConnection);
        dispose();
    }

    void dispose() {
        log.debug("Node {} got called dispose. future.isDone={}", node, future.isDone());
        removeListeners();
        future.cancel(true);
    }

    private void removeListeners() {
        if (outboundConnection != null) {
            outboundConnection.removeListener(this);
        }
    }
}