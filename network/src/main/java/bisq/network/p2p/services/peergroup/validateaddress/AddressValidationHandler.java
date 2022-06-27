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

package bisq.network.p2p.services.peergroup.validateaddress;

import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.*;
import bisq.network.p2p.services.peergroup.BanList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static bisq.network.NetworkService.NETWORK_IO_POOL;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Getter
@Slf4j
class AddressValidationHandler implements Connection.Listener {

    private final Node node;
    private final Address addressOfInboundConnection;
    private final BanList banList;
    private final CompletableFuture<Boolean> future = new CompletableFuture<>();
    private final int nonce;
    @Nullable
    private OutboundConnection outboundConnection;

    AddressValidationHandler(Node node, Address addressOfInboundConnection, BanList banList) {
        this.node = node;
        this.addressOfInboundConnection = addressOfInboundConnection;
        this.banList = banList;
        nonce = new Random().nextInt();
    }

    CompletableFuture<Boolean> request() {
        log.debug("Node {} send ConfirmAddressRequest to {} with nonce {}",
                node, addressOfInboundConnection, nonce);
        supplyAsync(() -> node.getConnection(addressOfInboundConnection, false), NETWORK_IO_POOL)
                .thenApply(connection ->
                        // We get called from the IO thread, so we do not use the async send method
                        node.send(new AddressValidationRequest(nonce), connection)
                )
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
    public void onNetworkMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof AddressValidationResponse addressValidationResponse) {
            Objects.requireNonNull(outboundConnection);
            if (addressValidationResponse.getRequestNonce() == nonce &&
                    outboundConnection.getPeerAddress().equals(addressOfInboundConnection)) {
                log.debug("Node {} received valid AddressValidationResponse from {}", node, addressOfInboundConnection);
                node.closeConnectionGracefullyAsync(outboundConnection, CloseReason.ADDRESS_VALIDATION_COMPLETED);
                removeListeners();
                future.complete(true);
            } else {
                log.warn("Node {} received an invalid AddressValidationResponse from {}." +
                                "Response nonce: {}. Request nonce: {}. " +
                                "connection.getPeerAddress()={}. address={}",
                        node, addressOfInboundConnection,
                        addressValidationResponse.getRequestNonce(), nonce,
                        outboundConnection.getPeerAddress(), addressOfInboundConnection);
                banList.add(addressOfInboundConnection, BanList.Reason.ADDRESS_VALIDATION_FAILED);
                banList.add(outboundConnection.getPeerAddress(), BanList.Reason.ADDRESS_VALIDATION_FAILED);
                node.closeConnection(outboundConnection, CloseReason.ADDRESS_VALIDATION_FAILED);
                removeListeners();
                future.complete(false);
            }
        }
    }

    @Override
    public void onConnectionClosed(CloseReason closeReason) {
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