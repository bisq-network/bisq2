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

import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.node.CloseConnectionMessage;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.inventory.InventoryRequest;
import bisq.network.p2p.services.data.inventory.InventoryResponse;
import bisq.network.p2p.services.peer_group.exchange.PeerExchangeRequest;
import bisq.network.p2p.services.peer_group.exchange.PeerExchangeResponse;
import bisq.network.p2p.services.peer_group.keep_alive.Ping;
import bisq.network.p2p.services.peer_group.keep_alive.Pong;
import bisq.network.p2p.services.peer_group.network_load.NetworkLoadExchangeRequest;
import bisq.network.p2p.services.peer_group.network_load.NetworkLoadExchangeResponse;

/**
 * Interface for any message sent as payload in NetworkEnvelope
 */
public interface EnvelopePayloadMessage extends NetworkProto {
    double getCostFactor();

    default bisq.network.protobuf.EnvelopePayloadMessage.Builder getNetworkMessageBuilder() {
        return bisq.network.protobuf.EnvelopePayloadMessage.newBuilder();
    }

    bisq.network.protobuf.EnvelopePayloadMessage toProto();

    static EnvelopePayloadMessage fromProto(bisq.network.protobuf.EnvelopePayloadMessage proto) {
        switch (proto.getMessageCase()) {
            case CONNECTIONHANDSHAKEREQUEST: {
                return ConnectionHandshake.Request.fromProto(proto.getConnectionHandshakeRequest());
            }
            case CONNECTIONHANDSHAKERESPONSE: {
                return ConnectionHandshake.Response.fromProto(proto.getConnectionHandshakeResponse());
            }
            case CLOSECONNECTIONMESSAGE: {
                return CloseConnectionMessage.fromProto(proto.getCloseConnectionMessage());
            }
            case PEEREXCHANGEREQUEST: {
                return PeerExchangeRequest.fromProto(proto.getPeerExchangeRequest());
            }
            case PEEREXCHANGERESPONSE: {
                return PeerExchangeResponse.fromProto(proto.getPeerExchangeResponse());
            }
            case PING: {
                return Ping.fromProto(proto.getPing());
            }
            case PONG: {
                return Pong.fromProto(proto.getPong());
            }
            case CONFIDENTIALMESSAGE: {
                return ConfidentialMessage.fromProto(proto.getConfidentialMessage());
            }
            case ACKMESSAGE: {
                return AckMessage.fromProto(proto.getAckMessage());
            }
            case INVENTORYREQUEST: {
                return InventoryRequest.fromProto(proto.getInventoryRequest());
            }
            case INVENTORYRESPONSE: {
                return InventoryResponse.fromProto(proto.getInventoryResponse());
            }
            case DATAREQUEST: {
                return DataRequest.fromProto(proto.getDataRequest());
            }
            case NETWORKLOADEXCHANGEREQUEST: {
                return NetworkLoadExchangeRequest.fromProto(proto.getNetworkLoadExchangeRequest());
            }
            case NETWORKLOADEXCHANGERESPONSE: {
                return NetworkLoadExchangeResponse.fromProto(proto.getNetworkLoadExchangeResponse());
            }
            case EXTERNALNETWORKMESSAGE: {
                // Externally defined messages
                return ExternalNetworkMessage.fromProto(proto.getExternalNetworkMessage());
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}
