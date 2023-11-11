package bisq.network.utils;

import bisq.network.common.TransportType;
import bisq.network.p2p.services.confidential.SendConfidentialMessageResult;

import java.util.HashMap;
import java.util.Optional;

public class SendMessageResult extends HashMap<TransportType, SendConfidentialMessageResult> {
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
