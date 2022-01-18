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

import bisq.account.Settlement;
import bisq.account.SettlementMethod;
import bisq.offer.SwapOffer;
import bisq.offer.protocol.ProtocolType;
import lombok.Getter;

@Getter
public class SwapContract extends TwoPartyContract<SwapOffer> {
    private final Settlement<? extends SettlementMethod> askSideSettlement;
    private final Settlement<? extends SettlementMethod> bidSideSettlement;

    public SwapContract(SwapOffer swapOffer,
                        ProtocolType protocolType,
                        Party taker,
                        Settlement<? extends SettlementMethod> askSideSettlement,
                        Settlement<? extends SettlementMethod> bidSideSettlement) {
        super(swapOffer, protocolType, taker);
        this.askSideSettlement = askSideSettlement;
        this.bidSideSettlement = bidSideSettlement;
    }
}
