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

package bisq.network.p2p.message;

import bisq.common.proto.Proto;
import bisq.network.p2p.node.CloseConnectionMessage;
import bisq.network.p2p.node.ConnectionHandshake;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.data.inventory.InventoryRequest;
import bisq.network.p2p.services.data.inventory.InventoryResponse;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.network.p2p.services.peergroup.keepalive.Ping;
import bisq.network.p2p.services.peergroup.keepalive.Pong;
import bisq.network.p2p.services.peergroup.validateaddress.AddressValidationRequest;
import bisq.network.p2p.services.peergroup.validateaddress.AddressValidationResponse;
import com.google.protobuf.Any;

/**
 * Interface for any message sent as payload in NetworkEnvelope
 */
public interface NetworkMessage extends Proto {
    default bisq.network.protobuf.NetworkMessage.Builder getNetworkMessageBuilder() {
        return bisq.network.protobuf.NetworkMessage.newBuilder();
    }

    static NetworkMessage resolve(Any any) {
        return NetworkMessageResolver.resolve(any);
    }

    bisq.network.protobuf.NetworkMessage toNetworkMessageProto();

    static NetworkMessage resolveNetworkMessage(bisq.network.protobuf.NetworkMessage networkMessage) {
        switch (networkMessage.getMessageCase()) {
            case CONNECTIONHANDSHAKEREQUEST -> {
                return  ConnectionHandshake.Request.fromProto(networkMessage.getConnectionHandshakeRequest());
            }
            case CONNECTIONHANDSHAKERESPONSE -> {
                return  ConnectionHandshake.Response.fromProto(networkMessage.getConnectionHandshakeResponse());
            }
            case CLOSECONNECTIONMESSAGE -> {
                return CloseConnectionMessage.fromProto(networkMessage.getCloseConnectionMessage());
            }
            case CONFIDENTIALMESSAGE -> {
                return ConfidentialMessage.fromProto(networkMessage.getConfidentialMessage());
            }
            case INVENTORYREQUEST -> {
                return InventoryRequest.fromProto(networkMessage.getInventoryRequest());
            }
            case INVENTORYRESPONSE -> {
                return InventoryResponse.fromProto(networkMessage.getInventoryResponse());
            }
            case ADDAUTHENTICATEDDATAREQUEST -> {
                return AddAuthenticatedDataRequest.fromProto(networkMessage.getAddAuthenticatedDataRequest());
            }
            case REMOVEAUTHENTICATEDDATAREQUEST -> {
                return RemoveAuthenticatedDataRequest.fromProto(networkMessage.getRemoveAuthenticatedDataRequest());
            }
            case REFRESHAUTHENTICATEDDATAREQUEST -> {
                return RefreshAuthenticatedDataRequest.fromProto(networkMessage.getRefreshAuthenticatedDataRequest());
            }
            case ADDMAILBOXREQUEST -> {
                return AddMailboxRequest.fromProto(networkMessage.getAddMailboxRequest());
            }
            case REMOVEMAILBOXREQUEST -> {
                return RemoveMailboxRequest.fromProto(networkMessage.getRemoveMailboxRequest());
            }
            case PEEREXCHANGEREQUEST -> {
                return Ping.fromProto(networkMessage.getPing());
            }
            case PEEREXCHANGERESPONSE -> {
                return Ping.fromProto(networkMessage.getPing());
            }
            case PING -> {
                return Ping.fromProto(networkMessage.getPing());
            }
            case PONG -> {
                return Pong.fromProto(networkMessage.getPong());
            }
            case ADDRESSVALIDATIONREQUEST -> {
                return AddressValidationRequest.fromProto(networkMessage.getAddressValidationRequest());
            }
            case ADDRESSVALIDATIONRESPONSE -> {
                return AddressValidationResponse.fromProto(networkMessage.getAddressValidationResponse());
            }
            case MESSAGE_NOT_SET -> {
                throw new RuntimeException("Could not resolve message case. networkMessage.getMessageCase()=" + networkMessage.getMessageCase());
            }
            default -> {
                //todo
                NetworkMessageResolver.resolve(Any.pack(networkMessage));
            }
        }
        throw new RuntimeException("Could not resolve message case. networkMessage.getMessageCase()=" + networkMessage.getMessageCase());
    }
}
