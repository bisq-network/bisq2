package bisq.network.p2p.services.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BroadcastResult extends ArrayList<CompletableFuture<bisq.network.p2p.services.data.broadcast.BroadcastResult>> {
    public BroadcastResult(Stream<CompletableFuture<bisq.network.p2p.services.data.broadcast.BroadcastResult>> list) {
        super(list.collect(Collectors.toList()));
    }

    public BroadcastResult(List<CompletableFuture<bisq.network.p2p.services.data.broadcast.BroadcastResult>> list) {
        super(list);
    }

    public BroadcastResult() {
        super();
    }
}
