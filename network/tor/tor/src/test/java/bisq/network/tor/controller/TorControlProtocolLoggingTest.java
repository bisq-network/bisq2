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

package bisq.network.tor.controller;

import bisq.common.encoding.Hex;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.keys.TorKeyPair;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.freehaven.tor.control.PasswordDigest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Users share their log files, so the Tor control commands and replies must not appear in the log with the secrets
 * which they carry. The test runs the protocol against a local fake control port and checks the logged lines.
 */
@Isolated("Captures the log of TorControlProtocol and TorControlReader")
@Timeout(10)
class TorControlProtocolLoggingTest {
    private final Logger protocolLogger = (Logger) LoggerFactory.getLogger(TorControlProtocol.class);
    private final Logger readerLogger = (Logger) LoggerFactory.getLogger(TorControlReader.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final TorControlProtocol torControlProtocol = new TorControlProtocol();
    private ServerSocket serverSocket;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws IOException {
        appender.start();
        protocolLogger.addAppender(appender);
        readerLogger.addAppender(appender);
        // TorControlProtocol connects to 127.0.0.1
        serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    }

    @AfterEach
    void tearDown() throws Exception {
        torControlProtocol.close();
        serverSocket.close();
        if (serverThread != null) {
            serverThread.join(5000);
        }
        protocolLogger.detachAppender(appender);
        readerLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void addOnionDoesNotLogThePrivateKey() {
        TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair();
        String secretScalar = torKeyPair.getBase64SecretScalar();
        String serviceId = torKeyPair.getOnionAddress().replace(".onion", "");
        startControlServer(command -> "250-ServiceID=" + serviceId + "\r\n250 OK\r\n");

        torControlProtocol.addOnion(torKeyPair, 9999, 12345);

        List<String> messages = loggedMessages();
        assertThat(messages).noneMatch(message -> message.contains(secretScalar));
        assertThat(messages).contains("Send Tor control command: ADD_ONION ED25519-V3:[hidden] Port=9999,12345",
                "Received reply from Tor control server: 250-ServiceID=" + serviceId,
                "Received reply from Tor control server: 250 OK");
    }

    @Test
    void replyWithPrivateKeyIsNotLogged() {
        TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair();
        String serviceId = torKeyPair.getOnionAddress().replace(".onion", "");
        // Tor sends a PrivateKey line if it generates the key (ADD_ONION NEW:...)
        String generatedSecretScalar = TorKeyGeneration.generateKeyPair().getBase64SecretScalar();
        startControlServer(command -> "250-ServiceID=" + serviceId + "\r\n" +
                "250-PrivateKey=ED25519-V3:" + generatedSecretScalar + "\r\n" +
                "250 OK\r\n");

        torControlProtocol.addOnion(torKeyPair, 9999, 12345);

        List<String> messages = loggedMessages();
        assertThat(messages).noneMatch(message -> message.contains(generatedSecretScalar));
        assertThat(messages).contains("Received reply from Tor control server: 250-PrivateKey=[hidden]");
    }

    @Test
    void authenticateDoesNotLogThePassword() {
        PasswordDigest passwordDigest = PasswordDigest.generateDigest();
        String password = Hex.encode(passwordDigest.getSecret());
        startControlServer(command -> "250 OK\r\n");

        torControlProtocol.authenticate(passwordDigest);

        List<String> messages = loggedMessages();
        assertThat(messages).noneMatch(message -> message.contains(password));
        assertThat(messages).anyMatch(message -> message.startsWith("Send Tor control command: AUTHENTICATE"));
    }

    @Test
    void failedSetConfigDoesNotLogOrThrowTheValue() {
        String secretValue = "secret-proxy-password";
        startControlServer(command -> "552 Unrecognized option\r\n");

        assertThatThrownBy(() -> torControlProtocol.setConfig("Socks5ProxyPassword", secretValue))
                .isInstanceOf(ControlCommandFailedException.class)
                .hasMessageContaining("Socks5ProxyPassword")
                .hasMessageNotContaining(secretValue);

        List<String> messages = loggedMessages();
        assertThat(messages).noneMatch(message -> message.contains(secretValue));
        assertThat(messages).contains("Send Tor control command: SETCONF [hidden]");
    }

    private void startControlServer(Function<String, String> replyByCommand) {
        serverThread = new Thread(() -> {
            try (Socket socket = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                 OutputStream outputStream = socket.getOutputStream()) {
                String command;
                while ((command = reader.readLine()) != null) {
                    outputStream.write(replyByCommand.apply(command).getBytes(StandardCharsets.US_ASCII));
                    outputStream.flush();
                }
            } catch (IOException ignore) {
                // The test closes the sockets
            }
        }, "FakeTorControlServer");
        serverThread.start();
        torControlProtocol.initialize(serverSocket.getLocalPort());
    }

    private List<String> loggedMessages() {
        // The reader thread logs a reply before the protocol can take it, so all lines of the reply are logged here
        synchronized (appender) {
            return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        }
    }
}
