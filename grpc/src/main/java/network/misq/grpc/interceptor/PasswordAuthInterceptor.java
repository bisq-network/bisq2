package network.misq.grpc.interceptor;

import io.grpc.*;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static io.grpc.Metadata.Key;
import static io.grpc.Status.UNAUTHENTICATED;
import static java.lang.String.format;

/**
 * Authorizes rpc server calls by comparing the value of the caller's
 * {@value PASSWORD_KEY} header to an expected value set at server startup time.
 */
public class PasswordAuthInterceptor implements ServerInterceptor {

    private static final String PASSWORD_KEY = "password";

    private final String expectedPasswordValue;

    public PasswordAuthInterceptor() {
        this.expectedPasswordValue = "password";
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> serverCallHandler) {
        var actualPasswordValue = headers.get(Key.of(PASSWORD_KEY, ASCII_STRING_MARSHALLER));

        if (actualPasswordValue == null)
            throw new StatusRuntimeException(UNAUTHENTICATED.withDescription(
                    format("missing '%s' rpc header value", PASSWORD_KEY)));

        if (!actualPasswordValue.equals(expectedPasswordValue))
            throw new StatusRuntimeException(UNAUTHENTICATED.withDescription(
                    format("incorrect '%s' rpc header value", PASSWORD_KEY)));

        return serverCallHandler.startCall(serverCall, headers);
    }
}
