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

package bisq.http_api.rest_api.domain;

import jakarta.ws.rs.core.Response;

import java.util.Map;

public abstract class RestApiBase {
    protected Response buildResponse(Response.Status status, Object entity) {
        return Response.status(status).entity(entity).build();
    }

    /**
     * Builds a successful 200 OK response.
     *
     * @param entity The response entity.
     * @return The HTTP response.
     */
    protected Response buildOkResponse(Object entity) {
        return Response.status(Response.Status.OK).entity(entity).build();
    }

    protected Response buildNotFoundResponse(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(message)
                .build();
    }

    /**
     * Builds an error response with a 500 status.
     *
     * @param errorMessage The error message.
     * @return The HTTP response.
     */
    protected Response buildErrorResponse(String errorMessage) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", errorMessage))
                .build();
    }
}
