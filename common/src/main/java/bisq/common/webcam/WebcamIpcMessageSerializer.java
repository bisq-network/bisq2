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

import org.jetbrains.annotations.VisibleForTesting;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class WebcamIpcMessageSerializer {
    public static void writeFrame(OutputStream outputStream, WebcamIpcWireMessage wireMessage) throws IOException {
        checkNotNull(outputStream, "outputStream must not be null");
        byte[] frame = serialize(wireMessage);
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(frame.length);
        dataOutputStream.write(frame);
        dataOutputStream.flush();
    }

    public static WebcamIpcWireMessage readFrame(InputStream inputStream) throws IOException {
        int maxFrameLength = WebcamIpcWireMessage.MAX_FRAME_LENGTH;
        checkNotNull(inputStream, "inputStream must not be null");

        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int frameLength = dataInputStream.readInt();
        checkArgument(frameLength > 0 && frameLength <= maxFrameLength,
                "Invalid webcam IPC frame length " + frameLength);
        byte[] frame = dataInputStream.readNBytes(frameLength);
        checkArgument(frame.length == frameLength, "Incomplete webcam IPC frame");
        return deserialize(frame);
    }

    @VisibleForTesting
    static byte[] serialize(WebcamIpcWireMessage wireMessage) {
        checkNotNull(wireMessage, "wireMessage must not be null");
        byte[] signedContent = wireMessage.getSignaturePayload();
        byte[] hmac = wireMessage.getHmac();
        checkArgument(hmac.length == WebcamIpcAuthenticator.HMAC_LENGTH, "Invalid webcam IPC HMAC length");

        return ByteBuffer.allocate(signedContent.length + hmac.length)
                .put(signedContent)
                .put(hmac)
                .array();
    }

    @VisibleForTesting
    static WebcamIpcWireMessage deserialize(byte[] frame) {
        checkNotNull(frame, "frame must not be null");
        checkArgument(frame.length <= WebcamIpcWireMessage.MAX_FRAME_LENGTH,
                "Invalid webcam IPC frame length");
        checkArgument(frame.length >= WebcamIpcWireMessage.HEADER_LENGTH + WebcamIpcAuthenticator.HMAC_LENGTH,
                "Invalid webcam IPC frame length");

        ByteBuffer byteBuffer = ByteBuffer.wrap(frame);
        byte version = byteBuffer.get();
        byte signalCode = byteBuffer.get();
        int payloadLength = byteBuffer.getInt();
        checkArgument(payloadLength >= 0, "Invalid webcam IPC payload length");
        checkArgument(payloadLength <= WebcamIpcWireMessage.MAX_PAYLOAD_LENGTH, "Invalid webcam IPC payload length");
        checkArgument(payloadLength <= frame.length - WebcamIpcWireMessage.HEADER_LENGTH - WebcamIpcAuthenticator.HMAC_LENGTH,
                "Invalid webcam IPC payload length");
        checkArgument(frame.length == WebcamIpcWireMessage.HEADER_LENGTH + payloadLength + WebcamIpcAuthenticator.HMAC_LENGTH,
                "Invalid webcam IPC frame length");

        byte[] payload = new byte[payloadLength];
        byteBuffer.get(payload);
        byte[] hmac = new byte[WebcamIpcAuthenticator.HMAC_LENGTH];
        byteBuffer.get(hmac);

        return new WebcamIpcWireMessage(version, signalCode, payload, hmac);
    }
}
