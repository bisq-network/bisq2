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

package bisq.protocol;

import bisq.contract.*;
import bisq.network.NetworkId;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.offer.Listing;
import bisq.security.PubKey;
import lombok.NonNull;

import java.util.Map;
import java.util.Set;

public class ContractMaker {
    public static TwoPartyContract makerCreatesTwoPartyContract(NetworkId takerNetworkId,
                                                                ProtocolType protocolType,
                                                                SettlementExecution settlementExecution) {
        Party maker = new Party(Role.MAKER, myNetworkId());
        Party taker = new Party(Role.TAKER, takerNetworkId);
        return new TwoPartyContract(protocolType, maker, taker, settlementExecution);
    }

    public static TwoPartyContract takerCreatesTwoPartyContract(Listing listing, ProtocolType protocolType,
                                                                SettlementExecution settlementExecution) {
        Party maker = new Party(Role.MAKER, listing.getMakerNetworkId());
        Party taker = new Party(Role.TAKER, myNetworkId());
        return new TwoPartyContract(protocolType, maker, taker, settlementExecution);
    }

    public static MultiPartyContract makerCreatesMultiPartyContract(NetworkId takerNetworkId,
                                                                    NetworkId escrowAgentNetworkId,
                                                                    ProtocolType protocolType,
                                                                    SettlementExecution settlementExecution) {
        Party maker = new Party(Role.MAKER, myNetworkId());
        Party taker = new Party(Role.TAKER, takerNetworkId);
        Party escrowAgent = new Party(Role.ESCROW_AGENT, escrowAgentNetworkId);
        return new MultiPartyContract(protocolType, maker, Set.of(taker, escrowAgent), settlementExecution);
    }

    public static MultiPartyContract takerCreatesMultiPartyContract(Listing listing,
                                                                    NetworkId escrowAgentNetworkId,
                                                                    ProtocolType protocolType,
                                                                    SettlementExecution settlementExecution) {
        Party maker = new Party(Role.MAKER, listing.getMakerNetworkId());
        Party taker = new Party(Role.TAKER, myNetworkId());
        Party escrowAgent = new Party(Role.ESCROW_AGENT, escrowAgentNetworkId);
        return new MultiPartyContract(protocolType, taker, Set.of(maker, escrowAgent), settlementExecution);
    }

    @NonNull
    private static NetworkId myNetworkId() {
        return new NetworkId(Map.of(Transport.Type.CLEAR, Address.localHost(1000)), new PubKey(null, "default"), "default");
    }
}
