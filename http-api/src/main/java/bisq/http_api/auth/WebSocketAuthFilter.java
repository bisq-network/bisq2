package bisq.http_api.auth;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.util.HttpStatus;

@Slf4j
public class WebSocketAuthFilter extends BaseFilter {
    private final String password;

    public WebSocketAuthFilter(String password) {
        this.password = password;
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) {
        Object message = ctx.getMessage();

        if (message instanceof HttpContent httpContent && httpContent.getHttpHeader() instanceof HttpRequestPacket request) {
            String upgradeHeader = request.getHeader("Upgrade");
            if ("websocket".equalsIgnoreCase(upgradeHeader)) {
                String timestamp = request.getHeader(AuthConstants.AUTH_TIMESTAMP_HEADER);
                String receivedHmac = request.getHeader(AuthConstants.AUTH_HEADER);
                if (!AuthConstants.isValidAuthentication(password, timestamp, receivedHmac)) {
                    log.warn("WebSocket connection rejected: Invalid or missing authorization token");
                    sendUnauthorizedResponse(ctx, request);
                    return ctx.getStopAction();
                }
            }
        }

        // http api requests are handled using HttpApiAuthFilter
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
}