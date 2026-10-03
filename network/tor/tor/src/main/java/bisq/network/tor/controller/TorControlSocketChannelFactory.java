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

package bisq.network.tor.controller;

import bisq.network.tor.controller.exceptions.CannotConnectWithTorException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.SocketChannel;

/**
 * Creates and connects a {@link SocketChannel} to a Tor control endpoint, retrying on failure.
 * Supports both TCP ({@link InetSocketAddress}) and Unix domain ({@code UnixDomainSocketAddress})
 * control endpoints via a single {@link SocketAddress} abstraction.
 */
@Slf4j
public class TorControlSocketChannelFactory {
    private static final int MAX_CONNECTION_ATTEMPTS = 10;

    private final SocketAddress socketAddress;
    private int connectionAttempt;

    TorControlSocketChannelFactory(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    SocketChannel createAndConnect() throws InterruptedException {
        Exception lastException = null;

        while (connectionAttempt < MAX_CONNECTION_ATTEMPTS) {
            SocketChannel socketChannel = null;
            try {
                ++connectionAttempt;
                socketChannel = connectToControlSocketRaw();
                return socketChannel;

            } catch (Exception e) {
                lastException = e;
                log.warn("Connection attempt {} to Tor control endpoint {} failed. Closing channel.",
                        connectionAttempt, socketAddress, e);
                closeSocketChannel(socketChannel, socketAddress);
            }

            if (connectionAttempt < MAX_CONNECTION_ATTEMPTS) {
                log.debug("Connection attempt to Tor control endpoint {} failed. Retrying in 200ms...", socketAddress);
                //noinspection BusyWait
                Thread.sleep(200);
            }
        }

        String errorMessage = "Failed to connect to Tor control endpoint " + socketAddress +
                " after " + MAX_CONNECTION_ATTEMPTS + " attempts.";
        IOException wrapperException = new IOException(errorMessage, lastException);
        log.error(errorMessage, wrapperException);
        throw new CannotConnectWithTorException(wrapperException);
    }

    private SocketChannel connectToControlSocketRaw() throws IOException {
        log.debug("Attempting to connect to Tor control endpoint {} ({}/{})",
                socketAddress, connectionAttempt, MAX_CONNECTION_ATTEMPTS);
        SocketChannel socketChannel = openSocketChannel();
        socketChannel.connect(socketAddress);
        log.info("Successfully connected control socket to Tor endpoint {} after {} attempts",
                socketAddress, connectionAttempt);
        return socketChannel;
    }

    private SocketChannel openSocketChannel() throws IOException {
        if (socketAddress instanceof InetSocketAddress) {
            // Explicitly use INET (IPv4) so the socket's local address appears as
            // "127.0.0.1" in /proc/net/tcp.  Java's default NioSocketImpl creates
            // AF_INET6 dual-stack sockets, causing the local address to appear as
            // "::ffff:127.0.0.1" in /proc/net/tcp6.  On Tails, onion-grater resolves
            // the client PID via psutil.net_connections() matching on local address;
            // the IPv6-mapped form does not match, so PID lookup fails and
            // onion-grater silently closes the connection.
            return SocketChannel.open(StandardProtocolFamily.INET);
        }
        return SocketChannel.open(StandardProtocolFamily.UNIX);
    }

    private void closeSocketChannel(SocketChannel socketChannel, SocketAddress socketAddress) {
        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                log.warn("Failed to close channel on endpoint {}", socketAddress, e);
            }
        }
    }
}
