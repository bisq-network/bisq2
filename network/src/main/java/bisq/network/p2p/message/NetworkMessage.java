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
import bisq.common.proto.UnresolvableProtobufMessageException;
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

    static NetworkMessage resolveNetworkMessage(bisq.network.protobuf.NetworkMessage proto) {
       /* switch (proto.getMessageCase()) {
        }*/
        switch (proto.getMessageCase()) {
            case CONNECTIONHANDSHAKEREQUEST -> {
                return ConnectionHandshake.Request.fromProto(proto.getConnectionHandshakeRequest());
            }
            case CONNECTIONHANDSHAKERESPONSE -> {
                return ConnectionHandshake.Response.fromProto(proto.getConnectionHandshakeResponse());
            }
            case CLOSECONNECTIONMESSAGE -> {
                return CloseConnectionMessage.fromProto(proto.getCloseConnectionMessage());
            }
            case CONFIDENTIALMESSAGE -> {
                return ConfidentialMessage.fromProto(proto.getConfidentialMessage());
            }
            case INVENTORYREQUEST -> {
                return InventoryRequest.fromProto(proto.getInventoryRequest());
            }
            case INVENTORYRESPONSE -> {
                return InventoryResponse.fromProto(proto.getInventoryResponse());
            }
            case ADDAUTHENTICATEDDATAREQUEST -> {
                return AddAuthenticatedDataRequest.fromProto(proto.getAddAuthenticatedDataRequest());
            }
            case REMOVEAUTHENTICATEDDATAREQUEST -> {
                return RemoveAuthenticatedDataRequest.fromProto(proto.getRemoveAuthenticatedDataRequest());
            }
            case REFRESHAUTHENTICATEDDATAREQUEST -> {
                return RefreshAuthenticatedDataRequest.fromProto(proto.getRefreshAuthenticatedDataRequest());
            }
            case ADDMAILBOXREQUEST -> {
                return AddMailboxRequest.fromProto(proto.getAddMailboxRequest());
            }
            case REMOVEMAILBOXREQUEST -> {
                return RemoveMailboxRequest.fromProto(proto.getRemoveMailboxRequest());
            }
            case PEEREXCHANGEREQUEST -> {
                return Ping.fromProto(proto.getPing());
            }
            case PEEREXCHANGERESPONSE -> {
                return Ping.fromProto(proto.getPing());
            }
            case PING -> {
                return Ping.fromProto(proto.getPing());
            }
            case PONG -> {
                return Pong.fromProto(proto.getPong());
            }
            case ADDRESSVALIDATIONREQUEST -> {
                return AddressValidationRequest.fromProto(proto.getAddressValidationRequest());
            }
            case ADDRESSVALIDATIONRESPONSE -> {
                return AddressValidationResponse.fromProto(proto.getAddressValidationResponse());
            }
            case EXTERNALNETWORKMESSAGE -> {
                // Externally defined messages
                return ExternalNetworkMessage.fromProto(proto.getExternalNetworkMessage());
            }
            case MESSAGE_NOT_SET -> {
                throw new UnresolvableProtobufMessageException(proto);
            }
           /* default -> {
                //todo
                return  NetworkMessageResolver.resolve(Any.pack(proto));
            }*/
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}
