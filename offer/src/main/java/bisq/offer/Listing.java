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

package bisq.offer;

import bisq.common.util.StringUtils;
import bisq.network.NetworkId;
import bisq.offer.options.ListingOption;
import bisq.offer.protocol.ProtocolType;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode
@Getter
public abstract class Listing implements Serializable {
    protected final String id;
    protected final long date;
    protected final NetworkId makerNetworkId;
    protected final List<? extends ProtocolType> protocolTypes;
    protected final Set<ListingOption> listingOptions;

    /**
     * @param id             The unique nodeId for that listing.
     * @param date           The date when the listing has been created.
     * @param protocolTypes  The list of the supported protocol types. Order in the list can be used as priority.
     * @param makerNetworkId The networkId the maker used for that listing. It encapsulates the network addresses
     *                       of the supported networks and the pubKey used for data protection in the storage layer.
     * @param listingOptions   A set of options covering different context specific aspects of the offer like fees,
     *                       reputation, transfers,... It depends on the chosen protocol and contract type.
     */
    public Listing(String id,
                   long date,
                   NetworkId makerNetworkId,
                   List<? extends ProtocolType> protocolTypes,
                   Set<ListingOption> listingOptions) {
        this.id = id;
        this.date = date;
        this.makerNetworkId = makerNetworkId;
        this.protocolTypes = protocolTypes;
        this.listingOptions = listingOptions;
    }

    public Listing(NetworkId makerNetworkId,
                   List<? extends ProtocolType> protocolTypes,
                   Set<ListingOption> listingOptions) {
        this(StringUtils.createUid(),
                System.currentTimeMillis(),
                makerNetworkId,
                protocolTypes,
                listingOptions);
    }
}
