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

package bisq.common.network.clear_net_address_types;

import bisq.common.network.Address;
import com.google.common.annotations.VisibleForTesting;

public class LocalHostAddressTypeFacade implements ClearNetAddressTypeFacade {
    @VisibleForTesting
    public static Address toLocalHostAddress(int port) {
        return new Address("127.0.0.1", port);
    }

    @Override
    public Address toMyLocalAddress(int port) {
        return toLocalHostAddress(port);
    }
}
