package bisq.network;

import bisq.network.common.TransportType;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;

import java.util.HashMap;

public class SendMessageResult extends HashMap<TransportType, ConfidentialMessageService.Result> {
    public SendMessageResult() {
        super();
    }
}
