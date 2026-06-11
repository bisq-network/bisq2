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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class WebcamIpcFrameCodec {
    public static void writeFrame(OutputStream outputStream, WebcamIpcWireMessage wireMessage) throws IOException {
        checkNotNull(outputStream, "outputStream must not be null");
        byte[] frame = WebcamIpcMessageSerializer.serialize(wireMessage);
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
        return WebcamIpcMessageSerializer.deserialize(frame);
    }
}
