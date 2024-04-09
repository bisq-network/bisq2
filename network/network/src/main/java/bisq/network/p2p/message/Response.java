package bisq.network.p2p.message;

/**
 * Response which is expected after having sent a Request to the peer. The requestId must match for Request and Response.
 */
public interface Response {
    String getRequestId();
}
