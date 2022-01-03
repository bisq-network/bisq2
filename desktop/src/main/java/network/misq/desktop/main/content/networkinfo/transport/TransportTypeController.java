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

package network.misq.desktop.main.content.networkinfo.transport;

import lombok.Getter;
import network.misq.application.DefaultServiceProvider;
import network.misq.common.encoding.Hex;
import network.misq.desktop.common.view.Controller;
import network.misq.network.NetworkService;
import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.message.TextMessage;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.confidential.ConfidentialMessageService;
import network.misq.network.p2p.services.data.AuthenticatedTextPayload;
import network.misq.security.KeyGeneration;
import network.misq.security.KeyPairService;
import network.misq.security.PubKey;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class TransportTypeController implements Controller {
    private final Transport.Type transportType;
    private final TransportTypeModel model;
    @Getter
    private final TransportTypeView view;
    private final NetworkService networkService;
    private final KeyPairService keyPairService;

    public TransportTypeController(DefaultServiceProvider serviceProvider, Transport.Type transportType) {
        networkService = serviceProvider.getNetworkService();

        keyPairService = serviceProvider.getKeyPairService();
        this.transportType = transportType;

        model = new TransportTypeModel(serviceProvider, transportType);
        view = new TransportTypeView(model, this);
    }

    CompletableFuture<String> sendMessage(String receiver, String pubKeyAsHex, String message) {
        Address address = new Address(receiver);
        Transport.Type transportType = Transport.Type.from(address);
        Map<Transport.Type, Address> addressByNetworkType = Map.of(transportType, address);
        PublicKey publicKey;
        try {
            publicKey = KeyGeneration.generatePublic(Hex.decode(pubKeyAsHex));
        } catch (GeneralSecurityException e) {
            return CompletableFuture.failedFuture(e);
        }
        PubKey pubKey = new PubKey(publicKey, KeyPairService.DEFAULT);
        NetworkId receiverNetworkId = new NetworkId(addressByNetworkType, pubKey, Node.DEFAULT_NODE_ID);
        KeyPair senderKeyPair = keyPairService.getOrCreateKeyPair(KeyPairService.DEFAULT);
        CompletableFuture<String> future = new CompletableFuture<>();
        networkService.confidentialSendAsync(new TextMessage(message), receiverNetworkId, senderKeyPair, Node.DEFAULT_NODE_ID)
                .whenComplete((resultMap, throwable) -> {
                    if (throwable == null) {
                        if (resultMap.containsKey(transportType)) {
                            ConfidentialMessageService.Result result = resultMap.get(transportType);
                            result.getMailboxFuture()
                                    .ifPresentOrElse(broadcastFuture -> broadcastFuture
                                                    .whenComplete((broadcastResult, error) ->
                                                            future.complete(result.getState() + "; " + broadcastResult.toString())),
                                            () -> {
                                                String value = result.getState().toString();
                                                if (result.getState() == ConfidentialMessageService.State.FAILED) {
                                                    value += " with Error: " + result.getErrorMsg();
                                                }
                                                future.complete(value);
                                            });
                        }
                    }
                });
        return future;
    }

    public CompletionStage<String> addData(String dataText, String id) {
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(id);
        Map<Transport.Type, Address> addressByNetworkType = Map.of();
        PubKey pubKey = new PubKey(keyPair.getPublic(), id);
        NetworkId networkId = new NetworkId(addressByNetworkType, pubKey, id);
        AuthenticatedTextPayload payload = new AuthenticatedTextPayload(dataText, networkId);
        return networkService.addNetworkPayload(payload, keyPair)
                .thenApply(list -> {
                    return list.toString();
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // View events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

}
