package bisq.network.p2p.services.confidential;

import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.p2p.services.data.BroadcastResult;
import lombok.Getter;

import java.util.Optional;

@Getter
public class SendConfidentialMessageResult {
    private final MessageDeliveryStatus messageDeliveryStatus;
    private Optional<BroadcastResult> mailboxFuture = Optional.empty();
    private Optional<String> errorMsg = Optional.empty();

    public SendConfidentialMessageResult(MessageDeliveryStatus messageDeliveryStatus) {
        this.messageDeliveryStatus = messageDeliveryStatus;
    }

    public SendConfidentialMessageResult setMailboxFuture(BroadcastResult mailboxFuture) {
        this.mailboxFuture = Optional.of(mailboxFuture);
        return this;
    }

    public SendConfidentialMessageResult setErrorMsg(String errorMsg) {
        this.errorMsg = Optional.of(errorMsg);
        return this;
    }

    @Override
    public String toString() {
        return "[messageDeliveryStatus=" + messageDeliveryStatus + errorMsg.map(error -> ", errorMsg=" + error + "]").orElse("]");
    }
}
