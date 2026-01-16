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

package bisq.common.network;

import bisq.common.proto.NetworkProto;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkPortValidation;
import com.google.common.annotations.VisibleForTesting;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

// We do not change the proto with subclasses to avoid breaking old clients.
@Slf4j
@EqualsAndHashCode
public abstract class Address implements NetworkProto, Comparable<Address> {

    public static Address from(String host, int port) {
        if (TorAddress.isTorAddress(host)) {
            return new TorAddress(host, port);
        } else if (I2PAddress.isBase64Destination(host)) {
            return new I2PAddress(host, port);
        } else if (I2PAddress.isBase32Destination(host)) {
            throw new IllegalArgumentException("Base32 I2P destination is not permitted as host: " + host);
        } else {
            return new ClearnetAddress(host, port);
        }
    }

    public static Address fromFullAddress(String socketAddress) {
        String original = socketAddress;
        checkArgument(StringUtils.isNotEmpty(socketAddress), "SocketAddress must not be null or empty");
        try {
            socketAddress = removeProtocolPrefix(socketAddress.trim());
            checkArgument(!socketAddress.isEmpty(), "SocketAddress must not be empty");
            // IPv6 bracketed form: [host]:port
            if (socketAddress.startsWith("[")) {
                int end = socketAddress.indexOf(']');
                checkArgument(end > 0 && end + 1 < socketAddress.length() && socketAddress.charAt(end + 1) == ':',
                        "Invalid IPv6 socket address, expected [host]:port");
                String hostToken = socketAddress.substring(1, end);
                String portToken = socketAddress.substring(end + 2).trim();
                int port = Integer.parseInt(portToken);
                return Address.from(hostToken, port);
            }

            // IPv4/hostname: split at last colon
            checkArgument(socketAddress.split(":").length == 2, "Socket address must be of form host:port");
            int sep = socketAddress.lastIndexOf(':');
            checkArgument(sep > 0 && sep < socketAddress.length() - 1, "Socket address must be of form host:port");
            String hostToken = socketAddress.substring(0, sep).trim();
            String portToken = socketAddress.substring(sep + 1).trim();
            int port = Integer.parseInt(portToken);
            return Address.from(hostToken, port);
        } catch (IllegalArgumentException e) {
            log.error("Could not resolve address from {}", original, e);
            throw new IllegalArgumentException("Could not resolve address from " + original, e);
        } catch (Exception e) {
            log.error("Could not resolve address from {}", original, e);
            throw new RuntimeException("Could not resolve address from " + original, e);
        }
    }

    @Getter
    protected final String host;
    @Getter
    protected final int port;

    protected Address(String host, int port) {
        try {
            checkArgument(StringUtils.isNotEmpty(host), "Host must not be null/blank");
            host = host.trim();
            checkArgument(NetworkPortValidation.isValid(port), "Invalid port: " + port);
            this.host = host;
            this.port = port;
            verify();
        } catch (Exception e) {
            log.error("Could not resolve address from {}:{}", host, port, e);
            throw e;
        }
    }


    /* --------------------------------------------------------------------- */
    // Protobuf
    /* --------------------------------------------------------------------- */

    @Override
    public bisq.common.protobuf.Address toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.common.protobuf.Address.Builder getBuilder(boolean serializeForHash) {
        return bisq.common.protobuf.Address.newBuilder()
                .setHost(host)
                .setPort(port);
    }

    abstract public TransportType getTransportType();

    public static Address fromProto(bisq.common.protobuf.Address proto) {
        return Address.from(proto.getHost(), proto.getPort());
    }

    public boolean isClearNetAddress() {
        return this instanceof ClearnetAddress;
    }

    public boolean isTorAddress() {
        return this instanceof TorAddress;
    }

    public boolean isI2pAddress() {
        return this instanceof I2PAddress;
    }

    public String getFullAddress() {
        return host + ":" + port;
    }

    @Override
    public String toString() {
        return getFullAddress();
    }

    @Override
    public int compareTo(Address o) {
        return getFullAddress().compareTo(o.getFullAddress());
    }

    @VisibleForTesting
    static String removeProtocolPrefix(String fullAddress) {
        // Match leading scheme
        // RFC 3986 scheme: ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
        String schemePattern = "^[a-zA-Z][a-zA-Z0-9+.-]*://";
        if (fullAddress.matches("(?i)^://.*")) {
            throw new IllegalArgumentException("Address has missing scheme before ://: " + fullAddress);
        }

        String withoutScheme = fullAddress.replaceFirst("(?i)" + schemePattern, "");
        // After removing scheme, ensure the remaining string is not another scheme
        if (withoutScheme.matches("(?i)^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            throw new IllegalArgumentException("Address has repeated scheme: " + fullAddress);
        }

        if (withoutScheme.isEmpty()) {
            throw new IllegalArgumentException("Address is empty after removing scheme: " + fullAddress);
        }
        return withoutScheme;
    }
}

