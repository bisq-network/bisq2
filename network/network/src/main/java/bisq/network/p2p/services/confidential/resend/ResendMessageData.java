package bisq.network.p2p.services.confidential.resend;

import bisq.common.proto.NetworkProto;
import bisq.network.common.Address;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ack.AckRequestingMessage;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.security.keys.KeyPairProtoUtil;
import bisq.security.keys.PubKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.security.KeyPair;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@EqualsAndHashCode
@ToString
public class ResendMessageData implements NetworkProto {
    public static ResendMessageData from(ResendMessageData data, MessageDeliveryStatus messageDeliveryStatus) {
        return new ResendMessageData(data.getAckRequestingMessage(),
                data.getAddress(),
                data.getReceiverResendMessageData(),
                data.getSenderKeyPair(),
                data.getSenderNetworkId(),
                messageDeliveryStatus);
    }

    private final EnvelopePayloadMessage envelopePayloadMessage;
    private final Address address;
    private final PubKey receiverResendMessageData;
    private final KeyPair senderKeyPair;
    private final NetworkId senderNetworkId;
    private final MessageDeliveryStatus messageDeliveryStatus;

    public ResendMessageData(AckRequestingMessage ackRequestingMessage,
                             Address address,
                             PubKey receiverResendMessageData,
                             KeyPair senderKeyPair,
                             NetworkId senderNetworkId,
                             MessageDeliveryStatus messageDeliveryStatus) {

        this((EnvelopePayloadMessage) ackRequestingMessage,
                address,
                receiverResendMessageData,
                senderKeyPair,
                senderNetworkId,
                messageDeliveryStatus);
    }

    private ResendMessageData(EnvelopePayloadMessage envelopePayloadMessage,
                              Address address,
                              PubKey receiverResendMessageData,
                              KeyPair senderKeyPair,
                              NetworkId senderNetworkId,
                              MessageDeliveryStatus messageDeliveryStatus) {
        this.envelopePayloadMessage = envelopePayloadMessage;
        this.address = address;
        this.receiverResendMessageData = receiverResendMessageData;
        this.senderKeyPair = senderKeyPair;
        this.senderNetworkId = senderNetworkId;
        this.messageDeliveryStatus = messageDeliveryStatus;

        verify();
    }

    @Override
    public void verify() {
        checkArgument(envelopePayloadMessage instanceof AckRequestingMessage,
                "envelopePayloadMessage must be of type AckRequestingMessage");
    }

    @Override
    public bisq.network.protobuf.ResendMessageData toProto() {
        return bisq.network.protobuf.ResendMessageData.newBuilder()
                .setEnvelopePayloadMessage(envelopePayloadMessage.toProto())
                .setAddress(address.toProto())
                .setReceiverPubKey(receiverResendMessageData.toProto())
                .setSenderKeyPair(KeyPairProtoUtil.toProto(senderKeyPair))
                .setSenderNetworkId(senderNetworkId.toProto())
                .setMessageDeliveryStatus(messageDeliveryStatus.toProto())
                .build();
    }

    public static ResendMessageData fromProto(bisq.network.protobuf.ResendMessageData proto) {
        return new ResendMessageData(EnvelopePayloadMessage.fromProto(proto.getEnvelopePayloadMessage()),
                Address.fromProto(proto.getAddress()),
                PubKey.fromProto(proto.getReceiverPubKey()),
                KeyPairProtoUtil.fromProto(proto.getSenderKeyPair()),
                NetworkId.fromProto(proto.getSenderNetworkId()),
                MessageDeliveryStatus.fromProto(proto.getMessageDeliveryStatus())
        );
    }

    public String getId() {
        return getAckRequestingMessage().getId();
    }

    public AckRequestingMessage getAckRequestingMessage() {
        return (AckRequestingMessage) envelopePayloadMessage;
    }
}
