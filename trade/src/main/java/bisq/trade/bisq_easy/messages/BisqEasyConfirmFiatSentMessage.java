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

package bisq.trade.bisq_easy.messages;

import bisq.network.NetworkId;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.trade.TradeMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class BisqEasyConfirmFiatSentMessage extends TradeMessage {
    public final static long TTL = TimeUnit.DAYS.toMillis(10);

    private final String buyersBtcAddress;

    public BisqEasyConfirmFiatSentMessage(String tradeId, NetworkId sender, String buyersBtcAddress) {
        this(tradeId,
                sender,
                buyersBtcAddress,
                new MetaData(TTL, 100000, BisqEasyConfirmFiatSentMessage.class.getSimpleName()));
    }

    private BisqEasyConfirmFiatSentMessage(String tradeId, NetworkId sender, String buyersBtcAddress, MetaData metaData) {
        super(tradeId, sender, metaData);

        this.buyersBtcAddress = buyersBtcAddress;
    }

    @Override
    protected bisq.trade.protobuf.TradeMessage toTradeMessageProto() {
        return getTradeMessageBuilder().setBisqEasyConfirmFiatSentMessage(
                        bisq.trade.protobuf.BisqEasyConfirmFiatSentMessage.newBuilder()
                                .setBuyersBtcAddress(buyersBtcAddress))
                .build();
    }

    public static BisqEasyConfirmFiatSentMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.BisqEasyConfirmFiatSentMessage bisqEasyTakeOfferRequest = proto.getBisqEasyConfirmFiatSentMessage();
        return new BisqEasyConfirmFiatSentMessage(
                proto.getTradeId(),
                NetworkId.fromProto(proto.getSender()),
                bisqEasyTakeOfferRequest.getBuyersBtcAddress(),
                MetaData.fromProto(proto.getMetaData()));
    }
}