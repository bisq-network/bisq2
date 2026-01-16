package bisq.api.access.filter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Headers {
    // Upgrade from http to websocket protocol
    public static final String UPGRADE = "Upgrade";

    // 'Sec-WebSocket-Key' is an HTTP request header used during the WebSocket opening handshake to ensure
    // protocol compliance and prevent connections from non-WebSocket clients. It does not provide security,
    // authentication, or encryption for the connection data itself.
    public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";

    public static final String USER_AGENT = "User-Agent";

    // Bisq specific
    public static final String SESSION_ID = "Bisq-Session-Id";
    public static final String TIMESTAMP = "Bisq-Timestamp";
    public static final String NONCE = "Bisq-Nonce";
    public static final String SIGNATURE = "Bisq-Signature";
    public static final String DEVICE_ID = "Bisq-Device-Id";
}
