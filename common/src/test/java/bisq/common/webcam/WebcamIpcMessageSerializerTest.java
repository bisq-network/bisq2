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

package bisq.common.webcam;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static bisq.common.encoding.NonPrintingCharacters.UNIT_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebcamIpcMessageSerializerTest {
    private static final String LEGACY_SEPARATOR = String.valueOf(UNIT_SEPARATOR.getNonPrintingChar());

    @Test
    void serializesAndVerifiesAuthenticatedQrCodeMessage() {
        String secret = WebcamIpcAuthenticator.generateSessionSecret();
        String payload = "bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy?message=test";
        WebcamIpcWireMessage wireMessage = WebcamIpcWireMessage.create(secret, WebcamControlSignals.QR_CODE_PREFIX, payload);

        WebcamIpcWireMessage deserializedMessage = WebcamIpcMessageSerializer.deserialize(WebcamIpcMessageSerializer.serialize(wireMessage));
        boolean isValid = WebcamIpcWireMessageValidation.verify(secret, deserializedMessage);
        assertTrue(isValid);

        WebcamIpcMessage verifiedMessage = WebcamIpcMessage.fromWireMessage(deserializedMessage);
        assertEquals(WebcamControlSignals.QR_CODE_PREFIX, verifiedMessage.getSignal());
        assertEquals(Optional.of(payload), verifiedMessage.getPayload());
    }

    @Test
    void serializesAndVerifiesAuthenticatedMessageWithoutPayload() {
        String secret = WebcamIpcAuthenticator.generateSessionSecret();
        WebcamIpcWireMessage wireMessage = WebcamIpcWireMessage.create(secret, WebcamControlSignals.HEART_BEAT);

        WebcamIpcWireMessage deserializedMessage = WebcamIpcMessageSerializer.deserialize(WebcamIpcMessageSerializer.serialize(wireMessage));
        boolean isValid = WebcamIpcWireMessageValidation.verify(secret, deserializedMessage);
        assertTrue(isValid);

        WebcamIpcMessage verifiedMessage = WebcamIpcMessage.fromWireMessage(deserializedMessage);
        assertEquals(WebcamControlSignals.HEART_BEAT, verifiedMessage.getSignal());
        assertEquals(Optional.empty(), verifiedMessage.getPayload());
    }

    @Test
    void rejectsMessageSignedWithWrongSecret() {
        WebcamIpcWireMessage wireMessage = WebcamIpcWireMessage.create("secret-1", WebcamControlSignals.HEART_BEAT);

        assertThrows(IllegalArgumentException.class, () -> WebcamIpcWireMessageValidation.verify("secret-2", wireMessage));
    }

    @Test
    void rejectsTamperedPayload() {
        String secret = WebcamIpcAuthenticator.generateSessionSecret();
        WebcamIpcWireMessage wireMessage = WebcamIpcWireMessage.create(secret, WebcamControlSignals.QR_CODE_PREFIX, "abc");
        byte[] frame = WebcamIpcMessageSerializer.serialize(wireMessage);
        frame[WebcamIpcWireMessage.HEADER_LENGTH] = (byte) 'x';

        WebcamIpcWireMessage tamperedMessage = WebcamIpcMessageSerializer.deserialize(frame);

        assertThrows(IllegalArgumentException.class, () -> WebcamIpcWireMessageValidation.verify(secret, tamperedMessage));
    }

    @Test
    void rejectsLegacyUnauthenticatedMessage() {
        byte[] legacyMessage = (LEGACY_SEPARATOR + "HEART_BEAT").getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> WebcamIpcMessageSerializer.deserialize(legacyMessage));
    }

    @Test
    void rejectsUnsupportedVersion() {
        String secret = WebcamIpcAuthenticator.generateSessionSecret();
        byte unsupportedVersion = 3;
        byte[] payload = new byte[0];
        byte[] signedContent = WebcamIpcWireMessage.createSignaturePayload(unsupportedVersion,
                WebcamControlSignals.HEART_BEAT.getCode(),
                payload);
        byte[] hmac = WebcamIpcAuthenticator.createHmac(secret, signedContent);
        WebcamIpcWireMessage unsupportedVersionMessage = new WebcamIpcWireMessage(unsupportedVersion,
                WebcamControlSignals.HEART_BEAT.getCode(),
                payload,
                hmac);

        assertThrows(IllegalArgumentException.class,
                () -> WebcamIpcWireMessageValidation.verify(secret, unsupportedVersionMessage));
    }

    @Test
    void rejectsInvalidFrameLength() {
        WebcamIpcWireMessage wireMessage = WebcamIpcWireMessage.create("secret", WebcamControlSignals.HEART_BEAT);
        byte[] frame = WebcamIpcMessageSerializer.serialize(wireMessage);
        byte[] truncatedFrame = new byte[frame.length - 1];
        System.arraycopy(frame, 0, truncatedFrame, 0, truncatedFrame.length);

        assertThrows(IllegalArgumentException.class, () -> WebcamIpcMessageSerializer.deserialize(truncatedFrame));
    }
}
