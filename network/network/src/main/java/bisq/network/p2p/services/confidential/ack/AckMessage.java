package bisq.network.p2p.services.confidential.ack;

import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static bisq.network.p2p.services.data.storage.MetaData.*;

/**
 * Acknowledge message sent back to the sender of an AckRequestingMessage as notification that the message has been
 * received.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class AckMessage implements MailboxMessage {
    private final MetaData metaData = new MetaData(TTL_2_DAYS, LOW_PRIORITY, getClass().getSimpleName(), MAX_MAP_SIZE_100);

    private final String id;

    /**
     * @param id The message ID of the AckRequestingMessage
     */
    public AckMessage(String id) {
        this.id = id;

        NetworkDataValidation.validateText(id, 100);
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage toProto() {
        return getNetworkMessageBuilder().setAckMessage(
                bisq.network.protobuf.AckMessage.newBuilder().setId(id)).build();
    }

    public static AckMessage fromProto(bisq.network.protobuf.AckMessage proto) {
        return new AckMessage(proto.getId());
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.05, 0.1);
    }
}
