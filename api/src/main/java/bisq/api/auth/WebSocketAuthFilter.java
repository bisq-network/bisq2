package bisq.api.auth;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.attributes.AttributeHolder;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.util.HttpStatus;

import javax.crypto.SecretKey;
import java.net.URI;

@Slf4j
public class WebSocketAuthFilter extends BaseFilter {
    private final SecretKey secretKey;

    public WebSocketAuthFilter(String password) {
        this.secretKey = AuthUtils.getSecretKey(password);
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) {
        Object message = ctx.getMessage();

        if (message instanceof HttpContent httpContent && httpContent.getHttpHeader() instanceof HttpRequestPacket request) {
            String upgradeHeader = request.getHeader("Upgrade");
            if ("websocket".equalsIgnoreCase(upgradeHeader)) {
                // The Grizzly filter chain may deliver the HTTP upgrade request in multiple chunks
                // Check if we've already authenticated this connection
                if (hasAttribute(ctx, "ws_authenticated")) {
                    return ctx.getInvokeAction();
                }

                String method = request.getMethod().getMethodString().toUpperCase();
                String normalizedPathAndQuery = AuthUtils.normalizePathAndQuery(URI.create(request.getRequestURI()));
                String timestamp = request.getHeader(AuthUtils.AUTH_TIMESTAMP_HEADER);
                String receivedHmac = request.getHeader(AuthUtils.AUTH_HEADER);
                String nonce = request.getHeader(AuthUtils.AUTH_NONCE_HEADER);

                if (!AuthUtils.isValidAuthentication(secretKey, method, normalizedPathAndQuery, nonce, timestamp, receivedHmac)) {
                    log.warn("WebSocket connection rejected: Invalid or missing authorization token");
                    sendUnauthorizedResponse(ctx, request);
                    return ctx.getStopAction();
                }

                // Mark connection as authenticated
                setAttribute(ctx, "ws_authenticated", true);
            }
        }

        // http api requests are handled using ApiAuthFilter
        return ctx.getInvokeAction();
    }

    private void sendUnauthorizedResponse(FilterChainContext ctx, HttpRequestPacket request) {
        HttpResponsePacket response = HttpResponsePacket.builder(request)
                .status(HttpStatus.UNAUTHORIZED_401.getStatusCode())
                .protocol(Protocol.HTTP_1_1)
                .contentLength(0)
                .build();

        ctx.write(response);
        ctx.getConnection().closeSilently();
    }

    private boolean hasAttribute(FilterChainContext ctx, String attribute) {
        AttributeHolder holder = ctx.getConnection().getAttributes();
        return holder != null && holder.getAttribute(attribute) != null;
    }

    private void setAttribute(FilterChainContext ctx, String attribute, Object value) {
        AttributeHolder holder = ctx.getConnection().getAttributes();
        if (holder != null) {
            holder.setAttribute(attribute, value);
        } else {
            log.warn("Failed to set attribute of {} on holder as it was not initialized yet", attribute);
        }
    }
}