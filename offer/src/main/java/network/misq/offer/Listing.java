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

package network.misq.offer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import network.misq.contract.ProtocolType;
import network.misq.network.p2p.NetworkId;
import network.misq.offer.options.OfferOption;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@EqualsAndHashCode
@Getter
public abstract class Listing implements Serializable {
    private final String id;
    private final long date;
    private final List<? extends ProtocolType> protocolTypes;
    private final NetworkId makerNetworkId;
    private final Set<OfferOption> offerOptions;

    /**
     * @param id             The unique nodeId for that listing.
     * @param date           The date when the listing has been created.
     * @param protocolTypes  The list of the supported protocol types. Order in the list can be used as priority.
     * @param makerNetworkId The networkId the maker used for that listing. It encapsulate the network addresses
     *                       of the supported networks and the pubKey used for data protection in the storage layer.
     * @param offerOptions   A set of options covering different context specific aspects of the offer like fees,
     *                       reputation, transfers,... It depends on the chosen protocol and contract type.
     */
    public Listing(String id,
                   long date,
                   List<? extends ProtocolType> protocolTypes,
                   NetworkId makerNetworkId,
                   Set<OfferOption> offerOptions) {
        this.id = id;
        this.date = date;
        this.protocolTypes = protocolTypes;
        this.makerNetworkId = makerNetworkId;
        this.offerOptions = offerOptions;
    }

    public Listing(List<? extends ProtocolType> protocolTypes,
                   NetworkId makerNetworkId,
                   Set<OfferOption> offerOptions) {
        this(UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                protocolTypes,
                makerNetworkId,
                offerOptions);
    }
}
