package bisq.api.jax;

import bisq.api.jax.resource.ErrorMessage;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class StatusException extends RuntimeException {

    @Getter
    @Setter
    protected Response.Status httpStatus;

    public StatusException() {
    }

    public StatusException(Response.Status httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public static class StatusExceptionMapper implements ExceptionMapper<StatusException> {
        @Override
        public Response toResponse(StatusException exception) {
            return Response.status(exception.getHttpStatus())
                    .entity(new ErrorMessage(exception.getMessage()))
                    .build();
        }
    }
}