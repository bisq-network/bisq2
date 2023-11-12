package bisq.network;

import bisq.network.common.TransportType;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;

import java.util.HashMap;
import java.util.Optional;

public class SendMessageResult extends HashMap<TransportType, ConfidentialMessageService.Result> {
    public SendMessageResult() {
        super();
    }

    public static Optional<String> findAnyErrorMsg(SendMessageResult result) {
        return result.entrySet().stream()
                .filter(e -> e.getValue().getErrorMsg().isPresent())
                .map(e -> e.getKey().name() + ": " + e.getValue().getErrorMsg().get())
                .findAny();
    }
}
