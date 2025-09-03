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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.StringTokenizer;

import static com.google.common.base.Preconditions.checkArgument;

// We do not change the proto with subclasses to avoid breaking old clients.
@Slf4j
@EqualsAndHashCode
public abstract class Address implements NetworkProto, Comparable<Address> {
    public static Address from(String host, int port) {
        if (TorAddress.isTorAddress(host)) {
            return new TorAddress(host, port);
        } else if (I2PAddress.isI2pAddress(host)) {
            return new I2PAddress(host, port);
        } else {
            return new ClearnetAddress(host, port);
        }
    }

    public static Address fromFullAddress(String fullAddress) {
        try {
            fullAddress = removeProtocolPrefix(fullAddress);
            StringTokenizer tokenizer = new StringTokenizer(fullAddress, ":");
            String hostToken = tokenizer.nextToken();
            checkArgument(tokenizer.hasMoreTokens(), "Full address need to contain the port after the ':'.");
            String portToken = tokenizer.nextToken();
            int port = Integer.parseInt(portToken);
            return Address.from(hostToken, port);
        } catch (Exception e) {
            log.error("Could not resolve address from {}", fullAddress, e);
            throw e;
        }
    }

    @Getter
    protected final String host;
    @Getter
    protected final int port;

    protected Address(String host, int port) {
        try {
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
    public int compareTo(Address o) {
        return getFullAddress().compareTo(o.getFullAddress());
    }

    private static String removeProtocolPrefix(String fullAddress) {
        return fullAddress.replaceFirst("^https?://", "");
    }
}

