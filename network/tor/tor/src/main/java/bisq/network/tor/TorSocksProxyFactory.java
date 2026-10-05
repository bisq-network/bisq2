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

import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class TorSocksProxyFactory {
    private static final byte SOCKS_VERSION = 5;
    private static final byte USERNAME_PASSWORD_METHOD = 2;
    private static final byte USERNAME_PASSWORD_VERSION = 1;
    private static final byte CONNECT_COMMAND = 1;
    private static final byte DOMAIN_NAME_ADDRESS_TYPE = 3;
    private static final byte[] PASSWORD = {'0'};

    private final int socksPort;

    public TorSocksProxyFactory(int socksPort) {
        this.socksPort = socksPort;
    }

    // Connects to host and port through the SOCKS port of Tor. Tor isolates streams by the SOCKS username and password,
    // so streams with different stream IDs do not share a circuit (see tor-stream-isolation.md).
    public Socket getSocket(String host, int port, String streamId, int socketTimeout) throws IOException {
        checkArguments(host, port, streamId);
        Socket socket = new Socket();
        try {
            socket.setSoTimeout(socketTimeout);
            socket.connect(new InetSocketAddress("127.0.0.1", socksPort), socketTimeout);
            performHandshake(socket, host, port, streamId);
            return socket;
        } catch (IOException | RuntimeException e) {
            try {
                socket.close();
            } catch (IOException closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }
    }

    // Performs the SOCKS5 handshake on a socket which is connected to the SOCKS port of Tor. Afterwards, the socket is
    // connected to host and port. The socket timeout limits each read. The caller closes the socket if this fails.
    public static void performHandshake(Socket socket, String host, int port, String streamId) throws IOException {
        checkArguments(host, port, streamId);
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        OutputStream outputStream = socket.getOutputStream();

        outputStream.write(new byte[]{SOCKS_VERSION, 1, USERNAME_PASSWORD_METHOD});
        outputStream.flush();
        if (inputStream.readByte() != SOCKS_VERSION || inputStream.readByte() != USERNAME_PASSWORD_METHOD) {
            throw new SocketException("The SOCKS proxy did not accept username and password authentication");
        }

        byte[] username = toUsername(streamId);
        ByteArrayOutputStream authentication = new ByteArrayOutputStream();
        authentication.write(USERNAME_PASSWORD_VERSION);
        authentication.write(username.length);
        authentication.write(username);
        authentication.write(PASSWORD.length);
        authentication.write(PASSWORD);
        outputStream.write(authentication.toByteArray());
        outputStream.flush();
        if (inputStream.readByte() != USERNAME_PASSWORD_VERSION || inputStream.readByte() != 0) {
            throw new SocketException("The SOCKS proxy rejected the username and password");
        }

        byte[] hostBytes = host.getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream connect = new ByteArrayOutputStream();
        connect.write(new byte[]{SOCKS_VERSION, CONNECT_COMMAND, 0, DOMAIN_NAME_ADDRESS_TYPE,
                (byte) hostBytes.length});
        connect.write(hostBytes);
        connect.write(port >> 8);
        connect.write(port & 0xFF);
        outputStream.write(connect.toByteArray());
        outputStream.flush();

        if (inputStream.readByte() != SOCKS_VERSION) {
            throw new SocketException("Invalid SOCKS reply");
        }
        int reply = inputStream.readUnsignedByte();
        if (reply != 0) {
            throw new ConnectException("SOCKS: " + toReplyName(reply));
        }
        inputStream.readByte();
        int addressLength = switch (inputStream.readUnsignedByte()) {
            case 1 -> 4;
            case 3 -> inputStream.readUnsignedByte();
            case 4 -> 16;
            default -> throw new SocketException("Invalid address type in SOCKS reply");
        };
        inputStream.readFully(new byte[addressLength + 2]);
    }

    private static void checkArguments(String host, int port, String streamId) {
        checkNotNull(streamId, "streamId must not be null");
        checkArgument(StandardCharsets.US_ASCII.newEncoder().canEncode(host) && !host.isEmpty() && host.length() <= 255,
                "Invalid host name");
        checkArgument(port > 0 && port <= 0xFFFF, "Invalid port %s", port);
    }

    private static String toReplyName(int reply) {
        return switch (reply) {
            case 1 -> "General failure";
            case 2 -> "Connection not allowed by ruleset";
            case 3 -> "Network unreachable";
            case 4 -> "Host unreachable";
            case 5 -> "Connection refused";
            case 6 -> "TTL expired";
            case 7 -> "Command not supported";
            case 8 -> "Address type not supported";
            // Extended errors of Tor for onion services, if the SocksPort has the flag ExtendedErrors
            case 0xF0 -> "Onion service descriptor not found";
            case 0xF1 -> "Onion service descriptor invalid";
            case 0xF2 -> "Onion service introduction failed";
            case 0xF3 -> "Onion service rendezvous failed";
            case 0xF4 -> "Onion service missing client authorization";
            case 0xF5 -> "Onion service wrong client authorization";
            case 0xF6 -> "Onion service invalid address";
            case 0xF7 -> "Onion service introduction timed out";
            default -> "Unknown reply " + reply;
        };
    }

    public Socks5Proxy getSocks5Proxy() throws IOException {
        Socks5Proxy socks5Proxy = new Socks5Proxy("127.0.0.1", socksPort);
        socks5Proxy.resolveAddrLocally(false);
        return socks5Proxy;
    }

    private static byte[] toUsername(String streamId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(streamId.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(digest).toString(26).getBytes(StandardCharsets.US_ASCII);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
