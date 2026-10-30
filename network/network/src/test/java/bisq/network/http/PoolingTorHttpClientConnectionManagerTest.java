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

package bisq.network.http;

import org.apache.hc.client5.http.io.ManagedHttpClientConnection;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.MessageConstraintException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The connections of the Tor HTTP client parse responses of servers which Bisq does not control, so the limits of the
 * response head must hold whatever the defaults of the httpcore5 version are. httpcore5 5.4.2 had no limit for the
 * number of headers and the line length, so it would accept every response of these tests.
 */
class PoolingTorHttpClientConnectionManagerTest {
    private static final String STATUS_LINE = "HTTP/1.1 200 OK\r\n";

    @Test
    void responseWithinTheLimitsIsAccepted() throws Exception {
        String response = STATUS_LINE + "X-Header: value\r\n".repeat(50) + "\r\n";

        ClassicHttpResponse parsed = receiveResponseHead(response);

        assertThat(parsed.getCode()).isEqualTo(200);
        assertThat(parsed.getHeaders()).hasSize(50);
    }

    @Test
    void responseWithMoreThanOneHundredHeadersIsRefused() {
        String response = STATUS_LINE + "X-Header: value\r\n".repeat(150) + "\r\n";

        assertThatThrownBy(() -> receiveResponseHead(response))
                .isInstanceOf(MessageConstraintException.class);
    }

    @Test
    void headerLineLongerThanTheLimitIsRefused() {
        String response = STATUS_LINE + "X-Header: " + "a".repeat(10_000) + "\r\n\r\n";

        assertThatThrownBy(() -> receiveResponseHead(response))
                .isInstanceOf(MessageConstraintException.class);
    }

    private static ClassicHttpResponse receiveResponseHead(String response) throws IOException, HttpException {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        try (ServerSocket serverSocket = new ServerSocket(0, 1, loopback);
             Socket clientSocket = new Socket(loopback, serverSocket.getLocalPort());
             Socket serverSideSocket = serverSocket.accept()) {
            serverSideSocket.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));
            serverSideSocket.shutdownOutput();

            ManagedHttpClientConnection connection =
                    PoolingTorHttpClientConnectionManager.CONNECTION_FACTORY.createConnection(clientSocket);
            return connection.receiveResponseHeader();
        }
    }
}
