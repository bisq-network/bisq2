package bisq.http_api.auth;

import bisq.http_api.access.session.SessionService;
import bisq.http_api.access.session.SessionToken;
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
import java.util.Optional;

@Slf4j
public class WebSocketAuthFilter extends BaseFilter {
    private static final String BISQ_SESSION_ID_HEADER = "Bisq-Session-Id";
    private static final String BISQ_CLIENT_ID_HEADER = "Bisq-Client-Id";

    private final SecretKey secretKey;
    private final Optional<SessionService> sessionService;

    public WebSocketAuthFilter(String password, Optional<SessionService> sessionService) {
        this.secretKey = AuthUtils.getSecretKey(password);
        this.sessionService = sessionService;
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

                // Try HMAC auth first (desktop/CLI clients)
                if (isValidHmacAuth(request)) {
                    setAttribute(ctx, "ws_authenticated", true);
                    return ctx.getInvokeAction();
                }

                // Fall back to session-based auth (mobile clients that paired via QR code)
                if (isValidSessionAuth(request)) {
                    setAttribute(ctx, "ws_authenticated", true);
                    return ctx.getInvokeAction();
                }

                log.warn("WebSocket connection rejected: No valid HMAC or session credentials");
                sendUnauthorizedResponse(ctx, request);
                return ctx.getStopAction();
            }
        }

        // http api requests are handled using HttpApiAuthFilter
        return ctx.getInvokeAction();
    }

    private boolean isValidHmacAuth(HttpRequestPacket request) {
        String method = request.getMethod().getMethodString().toUpperCase();
        String normalizedPathAndQuery = AuthUtils.normalizePathAndQuery(URI.create(request.getRequestURI()));
        String timestamp = request.getHeader(AuthUtils.AUTH_TIMESTAMP_HEADER);
        String receivedHmac = request.getHeader(AuthUtils.AUTH_HEADER);
        String nonce = request.getHeader(AuthUtils.AUTH_NONCE_HEADER);

        return AuthUtils.isValidAuthentication(secretKey, method, normalizedPathAndQuery, nonce, timestamp, receivedHmac);
    }

    private boolean isValidSessionAuth(HttpRequestPacket request) {
        if (sessionService.isEmpty()) {
            return false;
        }

        String sessionId = request.getHeader(BISQ_SESSION_ID_HEADER);
        String clientId = request.getHeader(BISQ_CLIENT_ID_HEADER);

        if (sessionId == null || clientId == null) {
            return false;
        }

        Optional<SessionToken> token = sessionService.get().find(sessionId);
        if (token.isEmpty()) {
            log.debug("Session not found for sessionId: {}", sessionId);
            return false;
        }

        SessionToken sessionToken = token.get();
        if (sessionToken.isExpired()) {
            log.debug("Session expired for sessionId: {}", sessionId);
            return false;
        }

        if (!sessionToken.getClientId().equals(clientId)) {
            log.warn("Client ID mismatch for sessionId: {}", sessionId);
            return false;
        }

        log.info("WebSocket connection authenticated via session credentials (clientId: {})", clientId);
        return true;
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
