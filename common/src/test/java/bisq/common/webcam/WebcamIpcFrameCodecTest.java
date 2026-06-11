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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebcamIpcFrameCodecTest {
    @Test
    void writesAndReadsAuthenticatedFrame() throws Exception {
        String secret = WebcamIpcAuthenticator.generateSessionSecret();
        String payload = "bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy";
        WebcamIpcWireMessage wireMessage = WebcamIpcWireMessage.create(secret, WebcamControlSignals.QR_CODE_PREFIX, payload);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        WebcamIpcFrameCodec.writeFrame(outputStream, wireMessage);
        WebcamIpcWireMessage decodedMessage = WebcamIpcFrameCodec.readFrame(
                new ByteArrayInputStream(outputStream.toByteArray()));

        assertTrue(WebcamIpcWireMessageValidation.verify(secret, decodedMessage));
        WebcamIpcMessage ipcMessage = WebcamIpcMessage.fromWireMessage(decodedMessage);
        assertEquals(WebcamControlSignals.QR_CODE_PREFIX, ipcMessage.getSignal());
        assertEquals(Optional.of(payload), ipcMessage.getPayload());
    }

    @Test
    void rejectsOversizedFrameLengthBeforeAllocatingPayload() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(WebcamIpcWireMessage.MAX_FRAME_LENGTH + 1);

        assertThrows(IllegalArgumentException.class,
                () -> WebcamIpcFrameCodec.readFrame(new ByteArrayInputStream(outputStream.toByteArray())));
    }

    @Test
    void rejectsIncompleteLengthPrefix() {
        byte[] incompleteLengthPrefix = new byte[]{0, 0};

        assertThrows(EOFException.class,
                () -> WebcamIpcFrameCodec.readFrame(new ByteArrayInputStream(incompleteLengthPrefix)));
    }
}
