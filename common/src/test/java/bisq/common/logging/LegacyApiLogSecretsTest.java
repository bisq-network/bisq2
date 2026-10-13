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

package bisq.common.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The API of versions 2.1.3 to 2.1.13 logged whole WebSocket messages and REST bodies, which carry the private keys of
 * user profiles (see docs/specifications/security/secrets-in-logs.md). The lines have the log pattern of these
 * versions: date, thread, level, abbreviated logger name, message, and a space before the line end.
 */
class LegacyApiLogSecretsTest {
    private static final String EC_KEY = "MIGNAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHMwcQIBAQQgEcSecret0123456789abcdef+/=";
    private static final String TOR_KEY = "TorSecret0123456789abcdefTorSecret0123456789abcdef+/==";
    private static final String I2P_KEY = "I2pSecret0123456789abcdefI2pSecret0123456789==";
    private static final String DATE = "Oct-01 10:15:22.123 ";
    private static final String HANDLER = DATE + "[WebSocket-1] INFO  b.a.w.WebSocketConnectionHandler: ";
    private static final String PROXY = DATE + "[HttpClient-1] INFO  b.a.w.r.WebSocketRestApiService: ";
    // The key material of a new profile, as GET /user-identities/key-material returns it
    private static final String KEY_MATERIAL = "{\"keyPair\":{\"publicKey\":{\"encoded\":\"MFkwEwYHKoZIzj0CAQ\"}," +
            "\"privateKey\":{\"encoded\":\"" + EC_KEY + "\"}},\"id\":\"b0edc477\",\"nym\":\"Ravenously-695\"}";
    // The key bundle of the identity of a trade, as the topic of trades sends it
    private static final String TRADE = "{\"trade\":{\"id\":\"trade-1\",\"myIdentity\":{\"tag\":\"default\"," +
            "\"keyBundle\":{\"keyId\":\"b0edc477\",\"keyPair\":{\"publicKey\":{\"encoded\":\"MFkw\"}," +
            "\"privateKey\":{\"encoded\":\"" + EC_KEY + "\"}},\"torKeyPair\":{\"privateKeyEncoded\":\"" + TOR_KEY +
            "\",\"publicKeyEncoded\":\"pub\",\"onionAddress\":\"abc.onion\"},\"i2pKeyPair\":{\"identityBytes\":\"" +
            I2P_KEY + "\",\"destinationBytes\":\"dest\"}}},\"paymentAccountData\":\"IBAN DE00 1234\"}}";

    @Test
    void newProfileKeyInAReceivedRequestIsHidden() {
        String request = "{\"className\":\"WebSocketRestApiRequest\",\"requestId\":\"7b1f\",\"method\":\"POST\"," +
                "\"path\":\"/api/v1/user-identities\",\"body\":\"" + escape("{\"nickName\":\"alice\"," +
                "\"keyMaterialResponse\":" + KEY_MATERIAL + "}") + "\"}";

        assertThat(LegacyLogSecrets.redact(HANDLER + "Received message " + request + " \n"))
                .isEqualTo(HANDLER + "Received message [hidden]\n");
    }

    @Test
    void keyMaterialInAProxiedResponseIsHidden() {
        String line = PROXY + "httpResponse WebSocketRestApiResponse(requestId=7b1f, statusCode=201, body=" +
                KEY_MATERIAL + ") \n";

        assertThat(LegacyLogSecrets.redact(line))
                .isEqualTo(PROXY + "httpResponse WebSocketRestApiResponse([hidden]\n");
    }

    // Versions 2.1.3 to 2.1.8 logged the HttpResponse of the JDK, without its body
    @Test
    void responseLineWithoutBodyStaysUnchanged() {
        String line = DATE + "[HttpClient-1] INFO  b.h.w.r.WebSocketRestApiService: " +
                "httpResponse (GET http://127.0.0.1:8090/api/v1/settings) 200 \n";

        assertThat(LegacyLogSecrets.redact(line)).isEqualTo(line);
    }

    @Test
    void keyBundleInTheEventsOfTheTopicOfTradesIsHidden() {
        String base = DATE + "[Trades] INFO  b.a.w.d.BaseWebSocketService: ";
        String subscriber = DATE + "[Subscriber-TRADES] INFO  b.a.w.s.Subscriber: ";
        String subscription = DATE + "[WebSocket-1] INFO  b.a.w.s.SubscriptionService: ";
        String event = "{\"topic\":\"TRADES\",\"payload\":\"" + escape("[" + TRADE + "]") + "\"}";
        // Versions 2.1.3 to 2.1.8 passed the subscriber and the modification type in the other order
        String log = base + "Sending json with modificationType ADDED to subscriber: Subscriber(topic=TRADES). json=[" +
                TRADE + "] \n" +
                base + "Sending json with modificationType Subscriber(topic=TRADES) to subscriber: ADDED. json=[" +
                TRADE + "] \n" +
                subscriber + "Send json: " + event + " \n" +
                subscription + "Send SubscriptionResponse json: " + event + " \n";

        assertThat(LegacyLogSecrets.redact(log)).isEqualTo(
                base + "Sending json with modificationType ADDED to subscriber: Subscriber(topic=TRADES). " +
                        "json=[hidden]\n" +
                        base + "Sending json with modificationType Subscriber(topic=TRADES) to subscriber: ADDED. " +
                        "json=[hidden]\n" +
                        subscriber + "Send json: [hidden]\n" +
                        subscription + "Send SubscriptionResponse json: [hidden]\n");
    }

    @Test
    void requestsInErrorLinesAreHidden() {
        String request = "{\"className\":\"WebSocketRestApiRequest\",\"body\":\"" + escape(KEY_MATERIAL) + "\"}";
        String warn = DATE + "[HttpClient-1] WARN  b.a.w.r.WebSocketRestApiService: ";
        String error = DATE + "[WebSocket-1] ERROR b.a.w.WebSocketConnectionHandler: ";
        // The log pattern of these versions writes the first line of an exception after the message
        String stackTrace = "java.util.concurrent.CompletionException: java.net.ConnectException: " +
                "Connection refused\n" +
                "\tat java.base/java.util.concurrent.CompletableFuture.encodeThrowable(CompletableFuture.java:368)\n";
        String log = warn + "REST API call failed for request: " + request + " " + stackTrace +
                error + "No service found for handling message: " + request + " \n" +
                DATE + "[WebSocket-1] ERROR b.a.w.r.WebSocketRestApiRequest: Json deserialization failed. Message=" +
                request + " \n" +
                DATE + "[WebSocket-1] ERROR b.a.w.s.SubscriptionRequest: Json deserialization failed. Message=" +
                request + " \n";

        assertThat(LegacyLogSecrets.redact(log)).isEqualTo(
                warn + "REST API call failed for request: [hidden]\n" +
                        stackTrace.substring(stackTrace.indexOf('\n') + 1) +
                        error + "No service found for handling message: [hidden]\n" +
                        DATE + "[WebSocket-1] ERROR b.a.w.r.WebSocketRestApiRequest: Json deserialization failed. " +
                        "Message=[hidden]\n" +
                        DATE + "[WebSocket-1] ERROR b.a.w.s.SubscriptionRequest: Json deserialization failed. " +
                        "Message=[hidden]\n");
    }

    // The message continues in the next line with the JSON of the request
    @Test
    void requestInTheLineAfterMessageWasNotSentIsHidden() {
        String warn = DATE + "[HttpClient-1] WARN  b.a.w.r.WebSocketRestApiService: " +
                "Message was not sent to websocket.\n";
        String log = warn + "Json={\"body\":\"" + escape(KEY_MATERIAL) + "\"} \n";

        assertThat(LegacyLogSecrets.redact(log)).isEqualTo(warn + "Json=[hidden]\n");
    }

    // Versions 2.1.3 to 2.1.8 logged the body of a request which the proxy could not forward
    @Test
    void bodyOfARequestWhichCouldNotBeForwardedIsHidden() {
        String error = DATE + "[WebSocket-1] ERROR b.h.w.r.WebSocketRestApiService: " +
                "Error at sending a 'POST' request " +
                "to 'http://127.0.0.1:8090/api/v1/user-identities' with body: ";
        String line = error + "'{\"keyMaterialResponse\":" + KEY_MATERIAL + "}'. Error: Connection refused \n";

        assertThat(LegacyLogSecrets.redact(line)).isEqualTo(error + "[hidden]\n");
    }

    @Test
    void aLineOfAnotherClassWithTheSameTextStaysUnchanged() {
        String line = DATE + "[Network] INFO  b.n.p.s.c.ConfidentialMessageService: Received message " +
                "AckMessage(id=1) \n";

        assertThat(LegacyLogSecrets.redact(line)).isEqualTo(line);
    }

    // A line form which the patterns do not know keeps its text, but not the values of private keys
    @Test
    void privateKeyValuesAreHiddenInAnyLine() {
        String line = DATE + "[Unknown] INFO  b.a.UnknownService: Payload " + TRADE + " Escaped " +
                escape(TRADE) + " Twice " + escape(escape(TRADE)) + " \n";

        String redacted = LegacyLogSecrets.redact(line);

        assertThat(redacted).doesNotContain(EC_KEY).doesNotContain(TOR_KEY).doesNotContain(I2P_KEY)
                .contains("\"privateKey\":{\"encoded\":\"[hidden]\"}")
                .contains("\\\"privateKeyEncoded\\\":\\\"[hidden]\\\"")
                .contains("\\\\\\\"identityBytes\\\\\\\":\\\\\\\"[hidden]\\\\\\\"")
                .contains("\"publicKey\":{\"encoded\":\"MFkw\"}")
                .contains("\"paymentAccountData\":\"IBAN DE00 1234\"");
    }

    @Test
    void lineEndsAndOtherBytesStayUnchanged() {
        // U+0085 is a byte of a UTF-8 sequence which the redactor reads as ISO-8859-1, for example of the letter A with
        // ring above. It must not end the line, and a "Json=" after it does not start a line.
        String payload = "{\"text\":\"\u00c3\u0085Json=x\",\"privateKey\":{\"encoded\":\"" + EC_KEY + "\"}}";
        String log = HANDLER + "Received message " + payload + " \r\n" +
                DATE + "[Unknown] INFO  b.a.UnknownService: text \u00c3\u0085Json=x \r\n";

        assertThat(LegacyLogSecrets.redact(log)).isEqualTo(HANDLER + "Received message [hidden]\r\n" +
                DATE + "[Unknown] INFO  b.a.UnknownService: text \u00c3\u0085Json=x \r\n");
    }

    // JSON within a JSON string: quotes and backslashes are escaped with a backslash
    private static String escape(String json) {
        return json.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
