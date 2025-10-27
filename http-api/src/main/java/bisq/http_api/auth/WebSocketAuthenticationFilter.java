package bisq.http_api.auth;

import bisq.common.encoding.Hex;
import bisq.security.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.util.HttpStatus;

import javax.annotation.Nullable;
import java.security.MessageDigest;

@Slf4j
public class WebSocketAuthenticationFilter extends BaseFilter {
    private final byte[] passwordHash;

    public WebSocketAuthenticationFilter(String password) {
        this.passwordHash = DigestUtil.sha256(password.getBytes());
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) {
        Object message = ctx.getMessage();

        if (message instanceof HttpContent httpContent && httpContent.getHttpHeader() instanceof HttpRequestPacket request) {
            String upgradeHeader = request.getHeader("Upgrade");
            if ("websocket".equalsIgnoreCase(upgradeHeader)) {
                String passwordHash = request.getHeader(AuthConstants.AUTH_HEADER);
                if (!isCorrectPassword(passwordHash)) {
                    log.warn("WebSocket connection rejected: Invalid or missing authorization token");
                    sendUnauthorizedResponse(ctx, request);
                    return ctx.getStopAction();
                }
            }
        }
        // http api requests are handled using HttpApiAuthFilter

        return ctx.getInvokeAction();
    }

    private boolean isCorrectPassword(@Nullable String receivedPassHash) {
        byte[] receivedPassHashBytes = null;
        if (receivedPassHash != null) {
            try {
                receivedPassHashBytes = Hex.decode(receivedPassHash);
            } catch (Exception e) {
                // no need to handle
            }
        }
        return receivedPassHashBytes != null && MessageDigest.isEqual(receivedPassHashBytes, passwordHash);
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
}