package bisq.network.p2p.message;

/**
 * Used for network module scope handling of Request/Response pattern.
 * Not to be used for higher level Request/Response pattern as that would conflict with lower level
 * usage (e.g. AckRequestingMessage/AckMessage).
 */
public interface Request {
    String getRequestId();
}
