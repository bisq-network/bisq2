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

package bisq.network.tor;

import bisq.network.tor.nio.TorSocketChannel;
import bisq.network.tor.nio.TorSocksConnectionData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

public class BasicTest {
    private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4096);

    @Test
    public void test() throws IOException {
        try (TorSocketChannel socketChannel = TorSocketChannel.open()) {
            TorSocksConnectionData connectionData = new TorSocksConnectionData(
                    9050,
                    "mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion",
                    80
            );

            socketChannel.connect(connectionData, byteBuffer);

            // Test request to mempool.space
            String testHttpRequest = "GET / HTTP/1.1\r\n" +
                    "Host: mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion\r\n" +
                    "User-Agent: Bisq2\r\n" +
                    "Accept: */*\r\n" +
                    "\r\n";
            byteBuffer.put(testHttpRequest.getBytes());

            byteBuffer.flip();
            socketChannel.write(byteBuffer);

            byteBuffer.clear();
            socketChannel.read(byteBuffer);
            byteBuffer.flip();

            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            CharBuffer charBuffer = decoder.decode(byteBuffer);
            System.out.print(charBuffer);
        }
    }
}
