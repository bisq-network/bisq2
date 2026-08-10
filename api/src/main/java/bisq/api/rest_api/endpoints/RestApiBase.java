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

package bisq.api.rest_api.endpoints;

import bisq.api.rest_api.pagination.PaginatedResponse;
import bisq.api.rest_api.pagination.PaginationParams;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

    /**
     * Builds a successful 204 NO_CONTENT response.
     *
     * @return The HTTP response.
     */
    protected Response buildNoContentResponse() {
        return Response.status(Response.Status.NO_CONTENT).build();
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
        return buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, errorMessage);
    }

    protected Response buildErrorResponse(Response.Status status, String errorMessage) {
        return Response.status(status)
                .entity(Map.of("error", errorMessage))
                .build();
    }

    protected <T> Response buildPaginatedResponse(List<T> items, PaginationParams pagination) {
        return buildPaginatedResponse(items, pagination, Function.identity());
    }

    protected <S, T> Response buildPaginatedResponse(List<S> items,
                                                     PaginationParams pagination,
                                                     Function<S, T> mapper) {
        PaginatedResponse<S> page = pagination.paginate(items);
        List<T> mapped = page.items().stream().map(mapper).toList();
        PaginatedResponse<T> body = new PaginatedResponse<>(
                mapped,
                page.page(),
                page.pageSize(),
                page.totalItems(),
                page.totalPages());
        return Response.status(Response.Status.OK).entity(body).build();
    }
}
