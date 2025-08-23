package bisq.network.p2p.common;

import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Request;
import bisq.network.p2p.message.Response;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;

public abstract class LeechRequestResponseHandler<T extends Request, R extends Response> extends RequestResponseHandler<T, R> {
    public LeechRequestResponseHandler(Node node, long timeout) {
        super(node, timeout);
    }

    @Override
    protected R createResponse(Connection connection, T request) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Class<T> getRequestClass() {
        throw new UnsupportedOperationException();
    }

    /* --------------------------------------------------------------------- */
    // Node.Listener implementation
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        // Leech mode: only sends requests but does not handle any incoming requests.
        resolveResponse(envelopePayloadMessage).ifPresent(response -> processResponse(connection, response));
    }
}

