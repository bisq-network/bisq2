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

package bisq.network.p2p.node;

import bisq.common.proto.Proto;
import bisq.common.util.StringUtils;
import bisq.tor.OnionAddress;
import com.google.common.net.InetAddresses;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.StringTokenizer;

@EqualsAndHashCode
@Getter
public final class Address implements Proto, Comparable<Address> {
    public static Address localHost(int port) {
        return new Address("127.0.0.1", port);
    }

    private final String host;
    private final int port;

    public Address(String fullAddress) {
        StringTokenizer st = new StringTokenizer(fullAddress, ":");
        this.host = maybeConvertLocalHost(st.nextToken());
        if (st.hasMoreTokens()) {
            this.port = Integer.parseInt(st.nextToken());
        } else {
            this.port = -1;
        }
    }

    public Address(String host, int port) {
        this.host = maybeConvertLocalHost(host);
        this.port = port;
    }

    public Address(OnionAddress onionAddress) {
        this(onionAddress.getHost(), onionAddress.getPort());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Protobuf
    ///////////////////////////////////////////////////////////////////////////////////////////

    public bisq.network.protobuf.Address toProto() {
        return bisq.network.protobuf.Address.newBuilder()
                .setHost(host)
                .setPort(port)
                .build();
    }

    public static Address fromProto(bisq.network.protobuf.Address proto) {
        return new Address(proto.getHost(), proto.getPort());
    }

    public boolean isClearNetAddress() {
        return InetAddresses.isInetAddress(host);
    }

    public boolean isTorAddress() {
        return host.endsWith(".onion");
    }

    public boolean isI2pAddress() {
        //TODO
        return !isClearNetAddress() && !isTorAddress();
    }

    public String getFullAddress() {
        return host + ":" + port;
    }

    @Override
    public String toString() {
        if (host.equals("127.0.0.1")) {
            return "[" + port + "]";
        } else {
            return "[" + StringUtils.truncate(host, 1000) + ":" + port + "]";
        }
    }

    private String maybeConvertLocalHost(String host) {
        return host.equals("localhost") ? "127.0.0.1" : host;
    }

    @Override
    public int compareTo(Address o) {
        return getFullAddress().compareTo(o.getFullAddress());
    }
}
