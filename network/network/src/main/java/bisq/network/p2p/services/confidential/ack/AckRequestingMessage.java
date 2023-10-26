package bisq.network.p2p.services.confidential.ack;


import bisq.network.identity.NetworkId;

/**
 * Interface marking a message which expects an AckMessage to be sent back as confidential message to the message sender.
 */
public interface AckRequestingMessage {
    /**
     * @return The message ID
     */
    String getId();

    /**
     * @return The NetworkId of the sender of that message.
     */
    NetworkId getSender();

    /**
     * @return The NetworkId of the receiver of that message.
     */
    NetworkId getReceiver();
}
