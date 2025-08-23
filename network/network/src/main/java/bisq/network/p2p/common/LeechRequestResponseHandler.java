/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.network.p2p.common;

import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Request;
import bisq.network.p2p.message.Response;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;

public abstract class LeechRequestResponseHandler<T extends Request, R extends Response> extends RequestResponseHandler<T, R> {
    public LeechRequestResponseHandler(Node node, long requestTimeoutMs) {
        super(node, requestTimeoutMs);
    }

    @Override
    protected R createResponse(Connection connection, T request) {
        throw new UnsupportedOperationException("Leech mode: inbound request handling is disabled");
    }

    @Override
    protected Class<T> getRequestClass() {
        throw new UnsupportedOperationException("Leech mode: request class is not supported");
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

