package bisq.network.p2p.node;

import bisq.common.util.StringUtils;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Request;
import bisq.network.p2p.message.Response;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Keeps track of request-response pairs. Used by the PeerGroupManager to check if there are any pending requests
 * on a connection and avoid that such connections gets closed.
 */
@Slf4j
public class RequestResponseManager {
    private static final long MAX_AGE = TimeUnit.MINUTES.toMillis(5);

    private final Map<String, Long> pendingRequests = new ConcurrentHashMap<>();
    private final ConnectionMetrics connectionMetrics;
    private volatile long pruneDate;

    public RequestResponseManager(ConnectionMetrics connectionMetrics) {
        this.connectionMetrics = connectionMetrics;
        pruneDate = System.currentTimeMillis();
    }

    void onReceived(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof Response response) {
            String requestId = response.getRequestId();
            synchronized (pendingRequests) {
                if (pendingRequests.containsKey(requestId)) {
                    long ts = pendingRequests.get(requestId);
                    pendingRequests.remove(requestId);
                    connectionMetrics.addRtt(System.currentTimeMillis() - ts);
                } else {
                    log.warn("We received a Response message but did not had a matching request. envelopePayloadMessage={}",
                            StringUtils.truncate(response.toString(), 500));
                }
                log.debug("onReceived {} requestId={}", response.getClass().getSimpleName(), requestId);
            }
            maybeRemoveExpired();
        }
    }

    void onSent(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof Request request) {
            String requestId = request.getRequestId();
            synchronized (pendingRequests) {
                if (pendingRequests.containsKey(requestId)) {
                    log.warn("We sent a Request message but we had already an entry in our map for that requestId. envelopePayloadMessage={}",
                            StringUtils.truncate(request.toString(), 500));
                }
                pendingRequests.put(requestId, System.currentTimeMillis());
                log.debug("onSent {} requestId={}", request.getClass().getSimpleName(), requestId);
            }
            maybeRemoveExpired();
        }
    }

    void dispose() {
        pendingRequests.clear();
    }

    public int numPendingRequests() {
        return pendingRequests.size();
    }

    public boolean hasPendingRequests() {
        return !pendingRequests.isEmpty();
    }

    private void maybeRemoveExpired() {
        long cutoffDate = System.currentTimeMillis() - MAX_AGE;
        if (pruneDate > cutoffDate) {
            return;
        }
        pruneDate = System.currentTimeMillis();
        Set<String> toRemove = pendingRequests.entrySet().stream()
                .filter(e -> e.getValue() < cutoffDate)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!toRemove.isEmpty()) {
            log.info("We have outdated pendingRequests. toRemove={}; pendingRequests={}", toRemove, pendingRequests);
            synchronized (pendingRequests) {
                toRemove.forEach(pendingRequests::remove);
            }
        }
    }
}
