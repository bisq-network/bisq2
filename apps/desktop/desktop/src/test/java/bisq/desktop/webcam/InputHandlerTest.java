/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.webcam;

import bisq.application.ApplicationService;
import bisq.common.webcam.WebcamControlSignals;
import bisq.common.webcam.WebcamIpcAuthenticator;
import bisq.common.webcam.WebcamIpcMessageSerializer;
import bisq.common.webcam.WebcamIpcWireMessage;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static bisq.common.encoding.NonPrintingCharacters.UNIT_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InputHandlerTest {
    private static final String LEGACY_SEPARATOR = String.valueOf(UNIT_SEPARATOR.getNonPrintingChar());

    @TempDir
    Path tempDir;

    @Test
    void acceptsAuthenticatedQrCodeMessage() {
        WebcamAppModel model = createModel();
        InputHandler inputHandler = new InputHandler(model);
        String sessionSecret = WebcamIpcAuthenticator.generateSessionSecret();
        String payload = "bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy";

        inputHandler.setSessionSecret(sessionSecret);
        inputHandler.onSocket(socketWith(WebcamIpcWireMessage.create(sessionSecret, WebcamControlSignals.QR_CODE_PREFIX, payload)));

        assertEquals(payload, model.getQrCode().get());
    }

    @Test
    void acceptsAuthenticatedQrCodeMessageAtMaxPayloadBytes() {
        WebcamAppModel model = createModel();
        InputHandler inputHandler = new InputHandler(model);
        String sessionSecret = WebcamIpcAuthenticator.generateSessionSecret();
        String payload = "a".repeat(WebcamIpcWireMessage.MAX_PAYLOAD_LENGTH);

        inputHandler.setSessionSecret(sessionSecret);
        inputHandler.onSocket(socketWith(WebcamIpcWireMessage.create(sessionSecret, WebcamControlSignals.QR_CODE_PREFIX, payload)));

        assertEquals(payload, model.getQrCode().get());
    }

    @Test
    void acceptsAuthenticatedShutdownMessage() {
        WebcamAppModel model = createModel();
        InputHandler inputHandler = new InputHandler(model);
        String sessionSecret = WebcamIpcAuthenticator.generateSessionSecret();
        inputHandler.setSessionSecret(sessionSecret);

        inputHandler.onSocket(socketWith(WebcamIpcWireMessage.create(sessionSecret, WebcamControlSignals.SHUTDOWN)));

        assertEquals(Boolean.TRUE, model.getIsShutdownSignalReceived().get());
    }

    @Test
    void acceptsAuthenticatedRestartMessage() {
        WebcamAppModel model = createModel();
        InputHandler inputHandler = new InputHandler(model);
        String sessionSecret = WebcamIpcAuthenticator.generateSessionSecret();
        inputHandler.setSessionSecret(sessionSecret);

        inputHandler.onSocket(socketWith(WebcamIpcWireMessage.create(sessionSecret, WebcamControlSignals.RESTART)));

        assertEquals(Boolean.TRUE, model.getRestartSignalReceived().get());
    }

    @Test
    void acceptsAuthenticatedImageRecognizedMessage() {
        WebcamAppModel model = createModel();
        InputHandler inputHandler = new InputHandler(model);
        String sessionSecret = WebcamIpcAuthenticator.generateSessionSecret();
        inputHandler.setSessionSecret(sessionSecret);

        inputHandler.onSocket(socketWith(WebcamIpcWireMessage.create(sessionSecret, WebcamControlSignals.IMAGE_RECOGNIZED)));

        assertEquals(Boolean.TRUE, model.getImageRecognized().get());
    }

    @Test
    void acceptsAuthenticatedHeartBeatMessage() {
        WebcamAppModel model = createModel();
        InputHandler inputHandler = new InputHandler(model);
        String sessionSecret = WebcamIpcAuthenticator.generateSessionSecret();
        inputHandler.setSessionSecret(sessionSecret);

        long beforeHeartBeat = System.currentTimeMillis();
        inputHandler.onSocket(socketWith(WebcamIpcWireMessage.create(sessionSecret, WebcamControlSignals.HEART_BEAT)));

        assertTrue(model.getLastHeartBeatTimestamp().get() >= beforeHeartBeat);
    }

    @Test
    void rejectsLegacyUnauthenticatedQrCodeMessage() {
        WebcamAppModel model = createModel();
        InputHandler inputHandler = new InputHandler(model);
        inputHandler.setSessionSecret(WebcamIpcAuthenticator.generateSessionSecret());

        assertThrows(IllegalArgumentException.class,
                () -> inputHandler.onSocket(legacySocketWith(LEGACY_SEPARATOR + "QR_CODE_PREFIX" + LEGACY_SEPARATOR + "attacker-address")));
        assertNull(model.getQrCode().get());
    }

    @Test
    void rejectsMessageAuthenticatedWithWrongSecret() {
        WebcamAppModel model = createModel();
        InputHandler inputHandler = new InputHandler(model);
        WebcamIpcWireMessage message = WebcamIpcWireMessage.create("secret-1", WebcamControlSignals.QR_CODE_PREFIX, "attacker-address");

        inputHandler.setSessionSecret("secret-2");

        assertThrows(IllegalArgumentException.class, () -> inputHandler.onSocket(socketWith(message)));
        assertNull(model.getQrCode().get());
    }

    @Test
    void rejectsSocketReadTimeout() {
        WebcamAppModel model = createModel();
        InputHandler inputHandler = new InputHandler(model);

        assertThrows(IllegalArgumentException.class, () -> inputHandler.onSocket(timingOutSocket()));
    }

    private WebcamAppModel createModel() {
        return new WebcamAppModel(new ApplicationService.Config(
                ConfigFactory.empty(),
                tempDir,
                "Bisq-Test",
                false,
                0,
                false,
                "",
                false,
                false,
                0,
                false,
                false));
    }

    private static Socket socketWith(WebcamIpcWireMessage message) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            WebcamIpcMessageSerializer.writeFrame(outputStream, message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] bytes = outputStream.toByteArray();
        return socketWith(bytes);
    }

    private static Socket legacySocketWith(String message) {
        return socketWith(message.getBytes(StandardCharsets.UTF_8));
    }

    private static Socket timingOutSocket() {
        return new Socket() {
            @Override
            public InputStream getInputStream() {
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new SocketTimeoutException("timeout");
                    }
                };
            }
        };
    }

    private static Socket socketWith(byte[] bytes) {
        return new Socket() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(bytes);
            }
        };
    }
}
