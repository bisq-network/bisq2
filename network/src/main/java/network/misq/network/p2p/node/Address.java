/*
 * This file is part of Misq.
 *
 * Misq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Misq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Misq. If not, see <http://www.gnu.org/licenses/>.
 */

package network.misq.network.p2p.node;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import network.misq.common.util.StringUtils;

import java.io.Serializable;
import java.util.StringTokenizer;

@EqualsAndHashCode
@Getter
public class Address implements Serializable {
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

    @Override
    public String toString() {
        if (host.equals("127.0.0.1")) {
            return "[" + port + "]";
        } else {
            return StringUtils.truncate(host, 4) + ":" + port;
        }
    }

    private String maybeConvertLocalHost(String host) {
        return host.equals("localhost") ? "127.0.0.1" : host;
    }
}
