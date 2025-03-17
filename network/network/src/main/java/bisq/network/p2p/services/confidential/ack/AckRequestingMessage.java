package bisq.network.p2p.services.confidential.ack;


import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.Request;

/**
 * Interface marking a message which expects an AckMessage to be sent back as confidential message to the message sender.
 */
public interface AckRequestingMessage extends Request {
    /**
     * @return The message ID
     */
    String getAckRequestingMessageId();

    /**
     * @return The NetworkId of the sender of that message.
     */
    NetworkId getSender();

    /**
     * @return The NetworkId of the receiver of that message.
     */
    NetworkId getReceiver();

    // This was added for supporting backward compatibility with messages which have been previously not
    // implemented AckRequestingMessage and thus would not have the required fields set.
    default boolean allFieldsValid() {
        return true;
    }

    default String getRequestId() {
        return getAckRequestingMessageId();
    }
}
