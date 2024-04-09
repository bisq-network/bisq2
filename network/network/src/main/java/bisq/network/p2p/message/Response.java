package bisq.network.p2p.message;

/**
 * Response which is expected after having sent a Request to the peer. The requestId must match for Request and Response.
 * <p>
 * Used for network module scope handling of Request/Response pattern.
 * Not to be used for higher level Request/Response pattern as that would conflict with lower level
 * usage (e.g. AckRequestingMessage/AckMessage).
 */
public interface Response {
    String getRequestId();
}
