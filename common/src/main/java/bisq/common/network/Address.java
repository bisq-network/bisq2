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
import bisq.common.validation.NetworkDataValidation;
import com.google.common.net.InetAddresses;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.StringTokenizer;

import static com.google.common.base.Preconditions.checkArgument;
@Slf4j
@EqualsAndHashCode
@Getter
public final class Address implements NetworkProto, Comparable<Address> {
    public static Address fromFullAddress(String fullAddress) {
        StringTokenizer st = new StringTokenizer(fullAddress, ":");
        String host = maybeConvertLocalHost(st.nextToken());
        checkArgument(st.hasMoreTokens(), "Full address need to contain the port after the ':'. fullAddress=" + fullAddress);
        int port = Integer.parseInt(st.nextToken());
        return new Address(host, port);
    }

    private final String host;
    private final int port;

    public Address(String host, int port) {
        this.host = maybeConvertLocalHost(host);
        this.port = port;

        verify();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Protobuf
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void verify() {
        if (isTorAddress()) {
            NetworkDataValidation.validateText(host, 62);
        } else if (isClearNetAddress()) {
            NetworkDataValidation.validateText(host, 45);
        } else {
            // I2P
            NetworkDataValidation.validateText(host, 512);
        }
    }

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

    public static Address fromProto(bisq.common.protobuf.Address proto) {
        return new Address(proto.getHost(), proto.getPort());
    }

    public boolean isClearNetAddress() {
        return InetAddresses.isInetAddress(host);
    }

    public boolean isTorAddress() {
        return host.endsWith(".onion");
    }

    public boolean isI2pAddress() {
        //TODO (deferred) add more specific check
        return !isClearNetAddress() && !isTorAddress();
    }

    public boolean isLocalhost() {
        return host.equals("127.0.0.1");
    }

    public TransportType getTransportType() {
        if (isClearNetAddress()) {
            return TransportType.CLEAR;
        } else if (isTorAddress()) {
            return TransportType.TOR;
        } else if (isI2pAddress()) {
            return TransportType.I2P;
        } else {
            throw new IllegalArgumentException("Could not derive TransportType from address: " + getFullAddress());
        }
    }

    public String getFullAddress() {
        return host + ":" + port;
    }

    @Override
    public String toString() {
        if (isLocalhost()) {
            return "[" + port + "]";
        } else {
            return StringUtils.truncate(host, 1000) + ":" + port;
        }
    }

    private static String maybeConvertLocalHost(String host) {
        return host.equals("localhost") ? "127.0.0.1" : host;
    }

    @Override
    public int compareTo(Address o) {
        return getFullAddress().compareTo(o.getFullAddress());
    }
}

