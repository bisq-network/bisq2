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

package bisq.common.rest_api.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
@Provider
@Getter
public class RestApiException extends RuntimeException {
    protected Response.Status httpStatus;

    private static Response.Status getStatus(Exception exception) {
        if (exception instanceof InterruptedException) {
            return Response.Status.REQUEST_TIMEOUT;
        } else if (exception instanceof ExecutionException) {
            return Response.Status.INTERNAL_SERVER_ERROR;
        } else if (exception instanceof JsonProcessingException) {
            return Response.Status.BAD_REQUEST;
        } else {
            return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }


    private static String getMessage(Exception exception) {
        if (exception instanceof InterruptedException) {
            return "The request was interrupted or timed out. ";
        } else if (exception instanceof ExecutionException) {
            return "An error occurred while processing the request. ";
        } else if (exception instanceof JsonProcessingException) {
            return "Invalid input: Unable to process JSON. ";
        } else {
            return "An error occurred. ";
        }
    }

    public RestApiException() {
    }

    public RestApiException(Exception exception) {
        this(getStatus(exception), getMessage(exception));
    }

    public RestApiException(Response.Status httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public static class Mapper implements ExceptionMapper<RestApiException> {
        @Override
        public Response toResponse(RestApiException exception) {
            return Response.status(exception.getHttpStatus())
                    .entity(new ErrorMessage(exception.getMessage()))
                    .build();
        }
    }
}