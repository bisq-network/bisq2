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

package bisq.desktop.webcam;

import bisq.common.webcam.WebcamControlSignals;
import bisq.common.webcam.WebcamIpcMessage;
import bisq.common.webcam.WebcamIpcMessageSerializer;
import bisq.common.webcam.WebcamIpcWireMessage;
import bisq.common.webcam.WebcamIpcWireMessageValidation;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static bisq.common.webcam.WebcamControlSignals.ERROR_MESSAGE_PREFIX;
import static bisq.common.webcam.WebcamControlSignals.HEART_BEAT;
import static bisq.common.webcam.WebcamControlSignals.IMAGE_RECOGNIZED;
import static bisq.common.webcam.WebcamControlSignals.QR_CODE_PREFIX;
import static bisq.common.webcam.WebcamControlSignals.RESTART;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class InputHandler {
    private final WebcamAppModel model;
    private volatile String sessionSecret;

    InputHandler(WebcamAppModel model) {
        this.model = model;
    }

    void setSessionSecret(String sessionSecret) {
        this.sessionSecret = sessionSecret;
    }

    void clearSessionSecret() {
        sessionSecret = null;
    }

    public void onSocket(Socket socket) {
        try {
            WebcamIpcWireMessage wireMessage = WebcamIpcMessageSerializer.readFrame(socket.getInputStream());
            processMessage(wireMessage);
        } catch (SocketTimeoutException e) {
            throw new IllegalArgumentException("Timed out reading webcam IPC frame", e);
        } catch (EOFException e) {
            throw new IllegalArgumentException("Incomplete webcam IPC frame", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read webcam IPC frame", e);
        }
    }

    private void processMessage(WebcamIpcWireMessage wireMessage) {
        String currentSessionSecret = sessionSecret;
        checkArgument(currentSessionSecret != null && !currentSessionSecret.isBlank(), "Missing webcam IPC session secret");

        boolean isWireMessageValid = WebcamIpcWireMessageValidation.verify(currentSessionSecret, wireMessage);
        checkArgument(isWireMessageValid, "Invalid wireMessage");

        WebcamIpcMessage ipcMessage = WebcamIpcMessage.fromWireMessage(wireMessage);
        WebcamControlSignals signal = ipcMessage.getSignal();
        Optional<String> payload = ipcMessage.getPayload();

        if (payload.isPresent()) {
            String payloadValue = payload.get();
            int payloadByteLength = payloadValue.getBytes(StandardCharsets.UTF_8).length;
            checkArgument(payloadByteLength <= WebcamIpcWireMessage.MAX_PAYLOAD_LENGTH,
                    "Received payload exceeds our limit of " + WebcamIpcWireMessage.MAX_PAYLOAD_LENGTH + " bytes");
            if (signal.equals(QR_CODE_PREFIX)) {
                model.getQrCode().set(payloadValue);
            } else if (signal.equals(ERROR_MESSAGE_PREFIX)) {
                model.getWebcamAppErrorMessage().set(payloadValue);
            } else {
                throw new IllegalArgumentException("Unexpected payload for message type " + signal);
            }
        } else {
            if (WebcamControlSignals.SHUTDOWN.equals(signal)) {
                model.getIsShutdownSignalReceived().set(true);
            } else if (RESTART.equals(signal)) {
                model.getRestartSignalReceived().set(true);
            } else if (IMAGE_RECOGNIZED.equals(signal)) {
                model.getImageRecognized().set(true);
            } else if (HEART_BEAT.equals(signal)) {
                model.getLastHeartBeatTimestamp().set(System.currentTimeMillis());
            } else {
                throw new IllegalArgumentException("Not recognized message type " + signal);
            }
        }
    }
}
