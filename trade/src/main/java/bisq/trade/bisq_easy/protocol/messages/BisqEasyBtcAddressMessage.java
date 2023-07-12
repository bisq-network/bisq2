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

package bisq.trade.bisq_easy.protocol.messages;

import bisq.network.NetworkId;
import bisq.network.p2p.services.data.storage.MetaData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class BisqEasyBtcAddressMessage extends BisqEasyTradeMessage {
    public final static long TTL = TimeUnit.DAYS.toMillis(10);

    private final String btcAddress;

    public BisqEasyBtcAddressMessage(String tradeId, NetworkId sender, String btcAddress) {
        this(tradeId,
                sender,
                btcAddress,
                new MetaData(TTL, 100000, BisqEasyBtcAddressMessage.class.getSimpleName()));
    }

    private BisqEasyBtcAddressMessage(String tradeId, NetworkId sender, String btcAddress, MetaData metaData) {
        super(tradeId, sender, metaData);

        this.btcAddress = btcAddress;
    }

    @Override
    protected bisq.trade.protobuf.TradeMessage toTradeMessageProto() {
        return getTradeMessageBuilder()
                .setBisqEasyTradeMessage(bisq.trade.protobuf.BisqEasyTradeMessage.newBuilder()
                        .setBisqEasyBtcAddressMessage(
                                bisq.trade.protobuf.BisqEasyBtcAddressMessage.newBuilder()
                                        .setBtcAddress(btcAddress)))
                .build();
    }

    public static BisqEasyBtcAddressMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.BisqEasyBtcAddressMessage bisqEasyConfirmFiatSentMessage = proto.getBisqEasyTradeMessage().getBisqEasyBtcAddressMessage();
        return new BisqEasyBtcAddressMessage(
                proto.getTradeId(),
                NetworkId.fromProto(proto.getSender()),
                bisqEasyConfirmFiatSentMessage.getBtcAddress(),
                MetaData.fromProto(proto.getMetaData()));
    }
}