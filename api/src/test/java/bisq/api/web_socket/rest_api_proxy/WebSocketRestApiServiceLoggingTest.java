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

package bisq.api.web_socket.rest_api_proxy;

import bisq.api.ApiConfig;
import bisq.api.access.transport.ApiAccessTransportType;
import bisq.api.access.transport.TlsContextService;
import bisq.common.json.JsonMapperProvider;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServletRequest;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A REST call through the WebSocket carries a body in both directions. A body can contain a private key, payment
 * account data or chat messages. Users share their log files, so the proxy must not write a body to the log.
 */
@Isolated("Captures the log of the api module")
class WebSocketRestApiServiceLoggingTest {
    private static final String PRIVATE_VALUE = "private-sentinel-value";
    private static final String PATH = "/api/v1/user-identities/key-material";
    private static final String REQUEST_BODY = "{\"accountData\":\"" + PRIVATE_VALUE + "\"}";
    private static final String RESPONSE_BODY = "{\"keyPair\":{\"privateKey\":{\"encoded\":\"" + PRIVATE_VALUE + "\"}}}";

    // The parent of all loggers of the api module, so that no class of the module can log the body unnoticed
    private final Logger logger = (Logger) LoggerFactory.getLogger("bisq.api");
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private HttpServer restServer;
    private WebSocketRestApiService service;

    @BeforeEach
    void setUp() throws IOException {
        restServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        restServer.createContext(PATH, exchange -> {
            byte[] body = RESPONSE_BODY.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        restServer.start();
        service = new WebSocketRestApiService(apiConfig(restServer.getAddress().getPort()),
                mock(TlsContextService.class));
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
        service.shutdown();
        restServer.stop(0);
    }

    @Test
    void responseBodyIsSentToTheClientAndNotWrittenToTheLog() throws Exception {
        DefaultWebSocket webSocket = webSocket();

        service.onMessage(requestNode("GET", "").toString(), webSocket);

        JsonNode response = sentMessage(webSocket);
        assertThat(response.get("statusCode").asInt()).isEqualTo(200);
        assertThat(response.get("body").asText()).isEqualTo(RESPONSE_BODY);
        List<String> logLines = logLines();
        assertThat(logLines).noneMatch(line -> line.contains(PRIVATE_VALUE));
        // The response is still logged, with its status code and the length of its body
        assertThat(logLines).anyMatch(line -> line.contains("statusCode=200")
                && line.contains("bodyLength=" + RESPONSE_BODY.length()));
    }

    // A client of a newer version can send a property which this node does not know. The node then cannot parse
    // the message and logs that. The message contains the body, so the log must not contain the message.
    @Test
    void requestWhichCannotBeParsedIsLoggedWithoutItsBody() {
        String json = requestNode("POST", REQUEST_BODY).put("aPropertyThisNodeDoesNotKnow", 1).toString();

        service.onMessage(json, webSocket());

        List<String> logLines = logLines();
        assertThat(logLines).noneMatch(line -> line.contains(PRIVATE_VALUE));
        assertThat(logLines).anyMatch(line -> line.contains("Message length=" + json.length()));
    }

    @Test
    void failedCallIsLoggedWithoutTheBodyOfTheRequest() throws Exception {
        DefaultWebSocket webSocket = webSocket();

        // The HTTP client of the proxy does not support the method CONNECT, so the call fails
        service.onMessage(requestNode("CONNECT", REQUEST_BODY).toString(), webSocket);

        assertThat(sentMessage(webSocket).get("statusCode").asInt()).isEqualTo(500);
        List<String> logLines = logLines();
        assertThat(logLines).noneMatch(line -> line.contains(PRIVATE_VALUE));
        // The failed request is still logged, with its ID and the length of its body
        assertThat(logLines).anyMatch(line -> line.contains("REST API call failed")
                && line.contains("requestId=request-id")
                && line.contains("bodyLength=" + REQUEST_BODY.length()));
    }

    private JsonNode sentMessage(DefaultWebSocket webSocket) throws Exception {
        ArgumentCaptor<String> sentMessage = ArgumentCaptor.forClass(String.class);
        // The proxy writes its log lines before it sends the response
        verify(webSocket, timeout(10_000)).send(sentMessage.capture());
        return JsonMapperProvider.get().readTree(sentMessage.getValue());
    }

    // Each log event as the log file shows it: the message and the stack trace with the exception messages
    private List<String> logLines() {
        return List.copyOf(appender.list).stream()
                .map(event -> event.getThrowableProxy() == null
                        ? event.getFormattedMessage()
                        : event.getFormattedMessage() + "\n" + ThrowableProxyUtil.asString(event.getThrowableProxy()))
                .toList();
    }

    private static ObjectNode requestNode(String method, String body) {
        return JsonMapperProvider.get().createObjectNode()
                .put("type", "WebSocketRestApiRequest")
                .put("requestId", "request-id")
                .put("method", method)
                .put("path", PATH)
                .put("body", body);
    }

    // The identity of the connection is on the upgrade request, which only a DefaultWebSocket exposes
    private static DefaultWebSocket webSocket() {
        DefaultWebSocket webSocket = mock(DefaultWebSocket.class);
        when(webSocket.getUpgradeRequest()).thenReturn(mock(HttpServletRequest.class));
        return webSocket;
    }

    private static ApiConfig apiConfig(int restServerPort) {
        return new ApiConfig(ApiAccessTransportType.CLEAR,
                3600,
                false,
                true,
                true,
                false,
                "127.0.0.1",
                restServerPort,
                80,
                false,
                false,
                false,
                "",
                List.of(),
                false,
                60);
    }
}
