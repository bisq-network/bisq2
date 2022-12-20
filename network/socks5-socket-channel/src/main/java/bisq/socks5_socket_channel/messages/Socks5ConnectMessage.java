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

public class Socks5ConnectMessage {
    private static final byte VERSION = 5;
    private static final byte NUMBER_OF_SUPPORTED_AUTH_METHODS = 1;
    private static final byte NO_AUTHENTICATION_METHOD = 0;

    public static void writeToByteBuffer(ByteBuffer byteBuffer) {
        byteBuffer.put(VERSION);
        byteBuffer.put(NUMBER_OF_SUPPORTED_AUTH_METHODS);
        byteBuffer.put(NO_AUTHENTICATION_METHOD);
    }

    public static void processServerReply(ByteBuffer byteBuffer) {
        byte serverVersion = byteBuffer.get();
        if (serverVersion != VERSION) {
            throw new IllegalStateException("Server does not support SOCKS5 proxy");
        }

        byte serverSelectedMethod = byteBuffer.get();
        if (serverSelectedMethod != NO_AUTHENTICATION_METHOD) {
            throw new IllegalStateException("Server does not support SOCKS5 username / password authentication.");
        }
    }
}
