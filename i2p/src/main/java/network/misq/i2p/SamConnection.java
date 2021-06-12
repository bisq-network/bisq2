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

package network.misq.i2p;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@Slf4j
class SamConnection {
    @Getter
    private final I2pSocket socket;
    private final PrintWriter printWriter;
    private final BufferedReader bufferedReader;
    private boolean handShakeCompleted;

    SamConnection(String host, int port, long timeout) throws IOException {
        socket = new I2pSocket(host, port);
        socket.setSoTimeout((int) timeout);
        socket.setTcpNoDelay(true);

        printWriter = new PrintWriter(socket.getOutputStream(), true);
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    Reply doHandShake(String request) throws IOException {
        if (handShakeCompleted) {
            throw new IllegalStateException("All expected handshakes have been completed. The ownership of the socket has been moved to the client.");
        }

        //todo hide priv keys from logs
       /* if (request.startsWith("SESSION CREATE STYLE=") && !request.contains("TRANSIENT")) {
            log.info(">> {}", request.split("DESTINATION=")[0] + "DESTINATION=... [private key hidden from logs]");
        } else {
            log.info(">> {}", request);
        }*/
        log.debug(">> {}", request);

        printWriter.write(request + "\n");
        printWriter.flush();

        Reply reply = new Reply(bufferedReader.readLine());
        log.debug("<< {}", reply);

        //todo hide priv keys from logs
       /* if (!reply.startsWith("DEST REPLY ")) {
            log.info("<< {}", reply);
        } else {
            log.info("<< {}", reply.split("PRIV=")[0] + "PRIV=... [private key hidden from logs]");
        }*/
        if (reply.isError()) {
            log.error("Handshake failed: reply={}", reply);
            close();
            throw new SamProtocolError(request, reply);
        }
        return reply;
    }

    Socket listenForClient() throws IOException {
        // Disable socket timeout
        socket.setSoTimeout(0);

        // Blocking wait for inbound connection
        String peerDestination = bufferedReader.readLine();
        log.error("Inbound connection with destination {}", peerDestination);
        socket.setPeerDestination(peerDestination);

        // We are done with the SAM control connection and hand over ownership of the socket to the client.
        handShakeCompleted = true;
        return socket;
    }

    I2pSocket getClientSocket() {
        // We are done with the SAM control connection and hand over ownership of the socket to the client.
        handShakeCompleted = true;
        return socket;
    }

    void close() {
        printWriter.write("EXIT\n");
        printWriter.flush();

        try {
            Thread.sleep(100);
        } catch (InterruptedException ignore) {
        }
        try {
            socket.close();
        } catch (IOException ignore) {
        }
    }
}
