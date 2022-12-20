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

package bisq.socks5_socket_channel.messages;

import java.nio.ByteBuffer;

public class Socks5RequestMessage {
    private static final byte VERSION = 5;
    private static final byte CONNECT_CMD = 1;
    private static final byte RESERVED_FIELD = 0;
    private static final byte DOMAIN_ADDRESS_TYPE = 3;

    private static final byte CONNECTION_SUCCEEDED_RESPONSE = 0;

    private final String hostName;
    private final int port;

    public Socks5RequestMessage(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    public void writeToByteBuffer(ByteBuffer byteBuffer) {
        byteBuffer.put(VERSION);
        byteBuffer.put(CONNECT_CMD);
        byteBuffer.put(RESERVED_FIELD);
        byteBuffer.put(DOMAIN_ADDRESS_TYPE);

        // Destination domain name
        byte[] destinationAddressAsBytes = hostName.getBytes();
        byteBuffer.put((byte) destinationAddressAsBytes.length); // destination length (no nul termination)
        byteBuffer.put(destinationAddressAsBytes);

        byte[] portInBytes = new byte[2];
        portInBytes[1] = (byte) (port & 0xFF);
        portInBytes[0] = (byte) ((port >> 8) & 0xFF);
        byteBuffer.put(portInBytes);
    }

    public void processServerReply(ByteBuffer byteBuffer) {
        byte serverVersion = byteBuffer.get();
        if (serverVersion != VERSION) {
            throw new IllegalStateException("Server does not support SOCKS5 proxy");
        }

        byte replyField = byteBuffer.get();
        if (replyField != CONNECTION_SUCCEEDED_RESPONSE) {
            throw new IllegalStateException("Couldn't connect to destination.");
        }
    }
}
