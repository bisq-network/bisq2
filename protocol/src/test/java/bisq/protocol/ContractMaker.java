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

import bisq.account.settlement.Settlement;
import bisq.contract.MultiPartyContract;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.SwapContract;
import bisq.network.NetworkId;
import bisq.offer.Listing;
import bisq.offer.SwapOffer;
import bisq.offer.protocol.SwapProtocolType;

import java.util.Set;

public class ContractMaker {
    public static SwapContract makerCreatesSwapContract(SwapOffer listing,
                                                        SwapProtocolType protocolType,
                                                        NetworkId takerNetworkId,
                                                        Settlement<? extends Settlement.Method> askSideSettlement,
                                                        Settlement<? extends Settlement.Method> bidSideSettlement) {
        Party taker = new Party(Role.TAKER, takerNetworkId);
        return new SwapContract(listing, protocolType, taker, askSideSettlement, bidSideSettlement);
    }

    public static SwapContract takerCreatesSwapContract(SwapOffer listing,
                                                        SwapProtocolType protocolType,
                                                        NetworkId takerNetworkId,
                                                        Settlement<? extends Settlement.Method> askSideSettlement,
                                                        Settlement<? extends Settlement.Method> bidSideSettlement) {
        Party taker = new Party(Role.TAKER, takerNetworkId);
        return new SwapContract(listing, protocolType, taker, askSideSettlement, bidSideSettlement);
    }

    public static MultiPartyContract makerCreatesMultiPartyContract(Listing listing,
                                                                    NetworkId takerNetworkId,
                                                                    NetworkId escrowAgentNetworkId,
                                                                    SwapProtocolType protocolType) {
        Party taker = new Party(Role.TAKER, takerNetworkId);
        Party escrowAgent = new Party(Role.ESCROW_AGENT, escrowAgentNetworkId);
        return new MultiPartyContract(listing, protocolType, Set.of(taker, escrowAgent));
    }

    public static MultiPartyContract takerCreatesMultiPartyContract(Listing listing,
                                                                    NetworkId takerNetworkId,
                                                                    NetworkId escrowAgentNetworkId,
                                                                    SwapProtocolType protocolType) {
        Party taker = new Party(Role.TAKER, takerNetworkId);
        Party escrowAgent = new Party(Role.ESCROW_AGENT, escrowAgentNetworkId);
        return new MultiPartyContract(listing, protocolType, Set.of(taker, escrowAgent));
    }
}
