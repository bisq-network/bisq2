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

import bisq.account.protocol_type.SwapProtocolType;
import bisq.common.monetary.Monetary;
import bisq.common.proto.Proto;
import bisq.network.NetworkId;
import bisq.offer.poc.PocOffer;
import com.google.protobuf.Message;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EqualsAndHashCode
@Getter
public final class Contract implements Proto {
    private final NetworkId takerNetworkId;
    private final SwapProtocolType protocolType;
    private final PocOffer offer;
    private final Monetary baseSideAmount;
    private final Monetary quoteSideAmount;
    private final String baseSideSettlementMethod;
    private final String quoteSideSettlementMethod;

    public Contract(NetworkId takerNetworkId,
                    SwapProtocolType protocolType,
                    PocOffer offer,
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

    @Override
    public Message toProto() {
        log.error("Not impl yet");
        return null;
    }
}