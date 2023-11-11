package bisq.network.p2p.services.confidential;

import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.p2p.services.data.DataService;
import lombok.Getter;

import java.util.Optional;

@Getter
public class SendConfidentialMessageResult {
    private final MessageDeliveryStatus messageDeliveryStatus;
    private DataService.BroadCastDataResult mailboxFuture = new DataService.BroadCastDataResult();
    private Optional<String> errorMsg = Optional.empty();

    public SendConfidentialMessageResult(MessageDeliveryStatus messageDeliveryStatus) {
        this.messageDeliveryStatus = messageDeliveryStatus;
    }

    public SendConfidentialMessageResult setMailboxFuture(DataService.BroadCastDataResult mailboxFuture) {
        this.mailboxFuture = mailboxFuture;
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
