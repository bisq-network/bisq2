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
import bisq.network.p2p.services.reporting.ReportRequest;
import bisq.network.p2p.services.reporting.ReportResponse;
import com.google.protobuf.Message;

/**
 * Interface for any message sent as payload in NetworkEnvelope
 */
public interface EnvelopePayloadMessage extends NetworkProto {
    double getCostFactor();

    default bisq.network.protobuf.EnvelopePayloadMessage.Builder newEnvelopePayloadMessageBuilder() {
        return bisq.network.protobuf.EnvelopePayloadMessage.newBuilder();
    }

    // EnvelopePayloadMessage level
    @Override
    default bisq.network.protobuf.EnvelopePayloadMessage toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash);


    // Implementation class level (this versus interface)
    default <T extends Message> T resolveValueProto(boolean serializeForHash) {
        //noinspection unchecked
        return (T) resolveBuilder(getValueBuilder(serializeForHash), serializeForHash).build();
    }

    Message.Builder getValueBuilder(boolean serializeForHash);

    default Message toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }


    static EnvelopePayloadMessage fromProto(bisq.network.protobuf.EnvelopePayloadMessage proto) {
        return switch (proto.getMessageCase()) {
            case CONNECTIONHANDSHAKEREQUEST ->
                    ConnectionHandshake.Request.fromProto(proto.getConnectionHandshakeRequest());
            case CONNECTIONHANDSHAKERESPONSE ->
                    ConnectionHandshake.Response.fromProto(proto.getConnectionHandshakeResponse());
            case CLOSECONNECTIONMESSAGE -> CloseConnectionMessage.fromProto(proto.getCloseConnectionMessage());
            case PEEREXCHANGEREQUEST -> PeerExchangeRequest.fromProto(proto.getPeerExchangeRequest());
            case PEEREXCHANGERESPONSE -> PeerExchangeResponse.fromProto(proto.getPeerExchangeResponse());
            case PING -> Ping.fromProto(proto.getPing());
            case PONG -> Pong.fromProto(proto.getPong());
            case CONFIDENTIALMESSAGE -> ConfidentialMessage.fromProto(proto.getConfidentialMessage());
            case ACKMESSAGE -> AckMessage.fromProto(proto.getAckMessage());
            case INVENTORYREQUEST -> InventoryRequest.fromProto(proto.getInventoryRequest());
            case INVENTORYRESPONSE -> InventoryResponse.fromProto(proto.getInventoryResponse());
            case DATAREQUEST -> DataRequest.fromProto(proto.getDataRequest());
            case NETWORKLOADEXCHANGEREQUEST ->
                    NetworkLoadExchangeRequest.fromProto(proto.getNetworkLoadExchangeRequest());
            case NETWORKLOADEXCHANGERESPONSE ->
                    NetworkLoadExchangeResponse.fromProto(proto.getNetworkLoadExchangeResponse());
            case EXTERNALNETWORKMESSAGE ->
                // Externally defined messages
                    ExternalNetworkMessage.fromProto(proto.getExternalNetworkMessage());
            case REPORTREQUEST -> ReportRequest.fromProto(proto.getReportRequest());
            case REPORTRESPONSE -> ReportResponse.fromProto(proto.getReportResponse());
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}
