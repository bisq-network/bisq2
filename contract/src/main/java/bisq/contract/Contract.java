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
import bisq.common.encoding.Proto;
import bisq.common.monetary.Monetary;
import bisq.network.NetworkId;
import bisq.offer.Offer;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public class Contract implements Proto {
    private final NetworkId takerNetworkId;
    private final SwapProtocolType protocolType;
    private final Offer offer;
    private final Monetary baseSideAmount;
    private final Monetary quoteSideAmount;
    private final String baseSideSettlementMethod;
    private final String quoteSideSettlementMethod;

    public Contract(NetworkId takerNetworkId,
                    SwapProtocolType protocolType,
                    Offer offer,
                    Monetary baseSideAmount,
                    Monetary quoteSideAmount,
                    String baseSideSettlementMethod,
                    String quoteSideSettlementMethod) {

        this.takerNetworkId = takerNetworkId;
        this.protocolType = protocolType;
        this.offer = offer;
        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
        this.baseSideSettlementMethod = baseSideSettlementMethod;
        this.quoteSideSettlementMethod = quoteSideSettlementMethod;
    }
}