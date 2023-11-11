package bisq.network.p2p.services.data;

import bisq.network.common.TransportType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BroadcastResult extends HashMap<TransportType, CompletableFuture<bisq.network.p2p.services.data.broadcast.BroadcastResult>> {
    public BroadcastResult(Map<TransportType, CompletableFuture<bisq.network.p2p.services.data.broadcast.BroadcastResult>> map) {
        super(map);
    }

    public BroadcastResult() {
        super();
    }
}
