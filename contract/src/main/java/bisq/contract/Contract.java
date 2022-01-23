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

package bisq.contract;

import bisq.offer.Listing;
import bisq.offer.protocol.SwapProtocolType;
import lombok.Getter;

/**
 * Defines the terms of the financial interaction with the counterparty/parties.
 */
@Getter
public abstract class Contract<T extends Listing> {
    protected final T listing;
    protected final SwapProtocolType protocolType;
    protected final Party maker;

    public Contract(T listing, SwapProtocolType protocolType) {
        this.protocolType = protocolType;
        this.maker = new Party(Role.MAKER, listing.getMakerNetworkId());
        this.listing = listing;
    }
}
