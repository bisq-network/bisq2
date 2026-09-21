package bisq.network.p2p.services.confidential.ack;

import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.message.Response;
import bisq.network.p2p.services.data.storage.MaxMapSize;
import bisq.network.p2p.services.data.storage.Priority;
import bisq.network.p2p.services.data.storage.StoragePolicy;
import bisq.network.p2p.services.data.storage.Ttl;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Acknowledge message sent back to the sender of an AckRequestingMessage as notification that the message has been
 * received.
 */
@Getter
@EqualsAndHashCode
@ToString
@StoragePolicy(ttl = Ttl.DAYS_5, priority = Priority.LOW, maxMapSize = MaxMapSize.SIZE_100)
public final class AckMessage implements MailboxMessage, Response {
    private final String id;

    /**
     * @param id The message ID of the AckRequestingMessage
     */
    public AckMessage(String id) {
        this.id = id;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateText(id, 100);
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
        return newEnvelopePayloadMessageBuilder().setAckMessage(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.AckMessage toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.AckMessage.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.AckMessage.newBuilder().setId(id);
    }

    public static AckMessage fromProto(bisq.network.protobuf.AckMessage proto) {
        return new AckMessage(proto.getId());
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.05, 0.1);
    }

    @Override
    public String getRequestId() {
        return id;
    }
}
