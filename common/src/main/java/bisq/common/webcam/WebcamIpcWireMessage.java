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

package bisq.common.webcam;

import bisq.common.util.ByteArrayUtils;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.VisibleForTesting;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class WebcamIpcWireMessage {
    public static final byte VERSION = 2;
    public static final int MAX_FRAME_LENGTH = 2000;
    public static final int MAX_PAYLOAD_LENGTH = 1000;
    static final int HEADER_LENGTH = Byte.BYTES + Byte.BYTES + Integer.BYTES; // 6

    @Getter(AccessLevel.PACKAGE)
    private final byte version;
    @Getter(AccessLevel.PACKAGE)
    private final byte signalCode;
    private final byte[] payload;
    private final byte[] hmac;

    WebcamIpcWireMessage(byte version, byte signalCode, byte[] payload, byte[] hmac) {
        this.version = version;
        this.signalCode = signalCode;
        this.payload = ByteArrayUtils.copyOf(checkNotNull(payload, "payload must not be null"));
        this.hmac = ByteArrayUtils.copyOf(checkNotNull(hmac, "hmac must not be null"));
    }

    byte[] getPayload() {
        return ByteArrayUtils.copyOf(payload);
    }

    byte[] getHmac() {
        return ByteArrayUtils.copyOf(hmac);
    }

    byte[] getSignaturePayload() {
        return createSignaturePayload(version, signalCode, payload);
    }

    static byte[] createSignaturePayload(byte version, byte signalCode, byte[] payload) {
        checkNotNull(payload, "payload must not be null");
        return ByteBuffer.allocate(HEADER_LENGTH + payload.length)
                .put(version)
                .put(signalCode)
                .putInt(payload.length)
                .put(payload)
                .array();
    }

    @VisibleForTesting
    public static WebcamIpcWireMessage create(String secretKey, WebcamControlSignals signal) {
        return create(secretKey, signal, Optional.empty());
    }

    @VisibleForTesting
    public static WebcamIpcWireMessage create(String secretKey, WebcamControlSignals signal, String payload) {
        return create(secretKey, signal, Optional.of(checkNotNull(payload, "payload must not be null")));
    }

    public static String truncatePayloadToMaxByteLength(String payload) {
        checkNotNull(payload, "payload must not be null");
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        if (payloadBytes.length <= MAX_PAYLOAD_LENGTH) {
            return payload;
        }

        int endIndex = 0;
        int byteLength = 0;
        while (endIndex < payload.length()) {
            int codePoint = payload.codePointAt(endIndex);
            int codePointByteLength = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
            if (byteLength + codePointByteLength > MAX_PAYLOAD_LENGTH) {
                break;
            }
            byteLength += codePointByteLength;
            endIndex += Character.charCount(codePoint);
        }
        return payload.substring(0, endIndex);
    }

    public static WebcamIpcWireMessage create(String sessionSecret,
                                              WebcamControlSignals signal,
                                              Optional<String> payload) {
        checkArgument(sessionSecret != null && !sessionSecret.isBlank(), "sessionSecret must not be empty");
        checkNotNull(signal, "signal must not be null");
        checkNotNull(payload, "payload must not be null");

        byte[] payloadBytes = payload.map(WebcamIpcWireMessage::toPayloadBytes).orElseGet(() -> new byte[0]);
        byte signalCode = signal.getCode();
        byte[] message = WebcamIpcWireMessage.createSignaturePayload(WebcamIpcWireMessage.VERSION, signalCode, payloadBytes);
        byte[] hmac = WebcamIpcAuthenticator.createHmac(sessionSecret, message);
        return new WebcamIpcWireMessage(WebcamIpcWireMessage.VERSION, signalCode, payloadBytes, hmac);
    }

    private static byte[] toPayloadBytes(String payload) {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        checkArgument(payloadBytes.length <= MAX_PAYLOAD_LENGTH,
                "Webcam IPC payload exceeds max length of " + MAX_PAYLOAD_LENGTH + " bytes");
        return payloadBytes;
    }
}
