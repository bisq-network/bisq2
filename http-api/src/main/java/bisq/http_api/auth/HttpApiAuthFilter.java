package bisq.http_api.auth;

import bisq.common.encoding.Hex;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Provider
@Priority(Priorities.AUTHENTICATION)
@Slf4j
public class HttpApiAuthFilter implements ContainerRequestFilter {
    private final SecretKey secretKey;

    public HttpApiAuthFilter(String password) {
        this.secretKey = AuthUtils.getSecretKey(password);
    }

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        URI requestUri = ctx.getUriInfo().getRequestUri();
        String normalizedPathAndQuery = AuthUtils.normalizePathAndQuery(requestUri);
        String timestamp = ctx.getHeaderString(AuthUtils.AUTH_TIMESTAMP_HEADER);
        String receivedHmac = ctx.getHeaderString(AuthUtils.AUTH_HEADER);
        String bodySha256Hex = null;
        byte[] bodyBytes = getBody(ctx);
        if (bodyBytes != null && bodyBytes.length > 0) {
            try {
                bodySha256Hex = Hex.encode(MessageDigest.getInstance("SHA-256").digest(bodyBytes));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        if (!AuthUtils.isValidAuthentication(secretKey, ctx.getMethod(), normalizedPathAndQuery, timestamp, receivedHmac, bodySha256Hex)) {
            log.warn("HttpRequest rejected: Invalid or missing authorization token");
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    @Nullable
    private byte[] getBody(ContainerRequestContext ctx) {
        if (ctx.getLength() <= 0) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream entityStream = ctx.getEntityStream();
        try {
            entityStream.transferTo(baos);
            byte[] bytes = baos.toByteArray();
            ctx.setEntityStream(new ByteArrayInputStream(bytes));
            return bytes;
        } catch (Exception e) {
            log.error("Failed to read request body for authentication, will result in auth failure", e);
            return null;
        }
    }
}