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
package bisq.network.tor.onionservice;

import lombok.Getter;
import lombok.ToString;

/**
 * The key of an onion service in the form which the ADD_ONION command of the Tor control protocol needs.
 */
@Getter
@ToString
public final class OnionServiceKey {
    private final String onionAddress;
    @ToString.Exclude
    private final String base64SecretScalar;

    public OnionServiceKey(String onionAddress, String base64SecretScalar) {
        this.onionAddress = onionAddress;
        this.base64SecretScalar = base64SecretScalar;
    }
}
