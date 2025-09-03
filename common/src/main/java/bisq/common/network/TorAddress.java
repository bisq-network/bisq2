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
import lombok.EqualsAndHashCode;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

@EqualsAndHashCode(callSuper = true)
public class TorAddress extends Address {
    private static final Pattern ONION_V3 = Pattern.compile("^[a-z2-7]{56}\\.onion$", Pattern.CASE_INSENSITIVE);

    // We support only version 3 addresses
    public static boolean isTorAddress(String host) {
        return ONION_V3.matcher(host).matches();
    }

    public TorAddress(String host, int port) {
        super(host, port);
    }

    @Override
    public void verify() {
        checkArgument(NetworkPortValidation.isValid(port), "Invalid port: "+port);
        NetworkDataValidation.validateText(host, 62, 62);
        checkArgument(isTorAddress(host), "Host must be a v3 onion address");
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.TOR;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
