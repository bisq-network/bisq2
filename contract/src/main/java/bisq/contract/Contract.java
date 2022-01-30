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

import bisq.account.protocol.SwapProtocolType;
import bisq.common.monetary.Monetary;
import bisq.network.NetworkId;
import bisq.offer.Offer;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class Contract implements Serializable {
    private final NetworkId takerNetworkId;
    private final SwapProtocolType protocolType;
    private final Offer offer;
    private final Monetary baseSideAmount;
    private final Monetary quoteSideAmount;
    private final String takersBaseSideSettlementMethod;
    private final String takersQuoteSideSettlementMethod;

    public Contract(NetworkId takerNetworkId,
                    SwapProtocolType protocolType,
                    Offer offer,
                    Monetary baseSideAmount,
                    Monetary quoteSideAmount,
                    String takersBaseSideSettlementMethod,
                    String takersQuoteSideSettlementMethod) {

        this.takerNetworkId = takerNetworkId;
        this.protocolType = protocolType;
        this.offer = offer;
        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
        this.takersBaseSideSettlementMethod = takersBaseSideSettlementMethod;
        this.takersQuoteSideSettlementMethod = takersQuoteSideSettlementMethod;
    }
}