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

package bisq.api.access.filter;

/**
 * Names of the request attributes and properties which the filters of the API record after they authenticated a
 * request.
 * <p>
 * Jersey keeps the properties of a REST request as attributes of the Grizzly request object, and Grizzly reuses
 * request objects for later requests. So the identity of a WebSocket and the properties of the REST filter have
 * different names, and each name has one writer. A value which the REST filter recorded can then never skip the
 * authentication of an upgrade request or become the identity of a WebSocket, also if Grizzly stopped clearing the
 * attributes of a reused request object.
 */
public final class Attributes {
    // The identity of a WebSocket: attributes of its upgrade request. Only the WebSocket filters write them, while
    // Grizzly handles the first content of the upgrade request (see WebSocketIdentity).
    public static final String IS_AUTHENTICATED = "bisq.web_socket.authn.is_authenticated";
    public static final String SESSION_ID = "bisq.web_socket.authn.session_id";
    public static final String CLIENT_ID = "bisq.web_socket.authz.client_id";

    // Properties of an authenticated REST request. Only RestApiSessionAuthenticationFilter writes them.
    public static final String REST_IS_AUTHENTICATED = "bisq.rest_api.authn.is_authenticated";
    public static final String REST_SESSION_ID = "bisq.rest_api.authn.session_id";
    public static final String REST_CLIENT_ID = "bisq.rest_api.authz.client_id";
}

