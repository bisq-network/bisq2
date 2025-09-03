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

import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.NetworkPortValidation;
import com.google.common.net.InetAddresses;
import lombok.EqualsAndHashCode;

import static com.google.common.base.Preconditions.checkArgument;

@EqualsAndHashCode(callSuper = true)
public class ClearnetAddress extends Address {
    private static final int MIN_HOST_LENGTH = 2;
    // IPv4: 7-15 characters, IPv6: 2-39 characters, FQDNs can be up to 253
    private static final int MAX_HOST_LENGTH = 253;

    public ClearnetAddress(String host, int port) {
        super(maybeConvertLocalHost(host), port);
    }

    @Override
    public void verify() {
        checkArgument(NetworkPortValidation.isValid(port), "Invalid port: " + port);
        NetworkDataValidation.validateText(host, MIN_HOST_LENGTH, MAX_HOST_LENGTH);
        checkArgument(InetAddresses.isInetAddress(host), "Invalid inetAddress");
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.CLEAR;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    public boolean isLocalhost() {
        return host.equals("127.0.0.1");
    }

    private static String maybeConvertLocalHost(String host) {
        return host.equals("localhost") ? "127.0.0.1" : host;
    }
}
