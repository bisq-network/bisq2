package bisq.network.p2p.services.data;

import bisq.network.common.TransportType;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BroadCastDataResult extends HashMap<TransportType, CompletableFuture<BroadcastResult>> {
    public BroadCastDataResult(Map<TransportType, CompletableFuture<BroadcastResult>> map) {
        super(map);
    }

    public BroadCastDataResult() {
        super();
    }
}
