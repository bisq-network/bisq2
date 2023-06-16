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

package bisq.protocol.bisq_easy;

import bisq.common.proto.Proto;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.protocol.bisq_easy.buyer_as_maker.BisqEasyBuyerAsMakerTrade;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public abstract class BisqEasyTrade<M extends BisqEasyProtocolModel, P extends BisqEasyProtocol<M>> implements Proto {
    public static String createTradeId(String offerId, String takerNodeId) {
        return offerId + "." + takerNodeId;
    }

    private final String tradeId;
    private final P bisqEasyProtocol;

    public BisqEasyTrade(P bisqEasyProtocol) {
        this.bisqEasyProtocol = bisqEasyProtocol;

        this.tradeId = createTradeId(getOffer().getId(), getTakerNetworkId().getNodeId());
    }

    private NetworkId getTakerNetworkId() {
        return getTaker().getNetworkId();
    }

    private ProtocolParty getTaker() {
        return getModel().getTaker();
    }

    @Override
    public bisq.protocol.protobuf.BisqEasyTrade toProto() {
        return bisq.protocol.protobuf.BisqEasyTrade.newBuilder()
                .build();
    }

    public static BisqEasyTrade<?, ?> fromProto(bisq.protocol.protobuf.BisqEasyTrade proto) {
        return new BisqEasyBuyerAsMakerTrade(null);//todo
    }

    public M getModel() {
        return bisqEasyProtocol.getModel();
    }

    public BisqEasyOffer getOffer() {
        return getModel().getOffer();
    }
}