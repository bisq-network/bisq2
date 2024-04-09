package bisq.network.p2p.node;

import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Request;
import bisq.network.p2p.message.Response;
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
public class PendingRequests {
    private static final long MAX_AGE = TimeUnit.SECONDS.toMillis(300);

    private final Map<String, Long> pendingRequests = new ConcurrentHashMap<>();
    private volatile long pruneDate;

    public PendingRequests() {
        pruneDate = System.currentTimeMillis();
    }

    void onReceived(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof Response) {
            String requestId = ((Response) envelopePayloadMessage).getRequestId();
            synchronized (pendingRequests) {
                if (pendingRequests.containsKey(requestId)) {
                    pendingRequests.remove(requestId);
                } else {
                    log.warn("We received a Response message but did not had a matching request. envelopePayloadMessage={}", envelopePayloadMessage);
                }
                log.debug("onReceived {} requestId={}", envelopePayloadMessage.getClass().getSimpleName(), requestId);
            }
        }
        maybeRemoveExpired();
    }

    void onSent(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof Request) {
            String requestId = ((Request) envelopePayloadMessage).getRequestId();
            synchronized (pendingRequests) {
                if (pendingRequests.containsKey(requestId)) {
                    log.warn("We sent a Request message but we had already an entry in our map for that requestId. envelopePayloadMessage={}", envelopePayloadMessage);
                }
                pendingRequests.put(requestId, System.currentTimeMillis());
                log.debug("onSent {} requestId={}", envelopePayloadMessage.getClass().getSimpleName(), requestId);
            }
        }
        maybeRemoveExpired();
    }

    void onClosed() {
        pendingRequests.clear();
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
