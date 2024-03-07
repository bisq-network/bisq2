package bisq.network.p2p.services.confidential.resend;

import bisq.network.common.Address;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.security.keys.PubKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.security.KeyPair;

@Getter
@EqualsAndHashCode
@ToString
public class ResendMessageData {
    private final EnvelopePayloadMessage envelopePayloadMessage;
    private final Address address;
    private final PubKey receiverPubKey;
    private final KeyPair senderKeyPair;
    private final NetworkId senderNetworkId;

    public ResendMessageData(EnvelopePayloadMessage envelopePayloadMessage,
                             Address address,
                             PubKey receiverPubKey,
                             KeyPair senderKeyPair,
                             NetworkId senderNetworkId) {
        this.envelopePayloadMessage = envelopePayloadMessage;
        this.address = address;
        this.receiverPubKey = receiverPubKey;
        this.senderKeyPair = senderKeyPair;
        this.senderNetworkId = senderNetworkId;
    }
}
