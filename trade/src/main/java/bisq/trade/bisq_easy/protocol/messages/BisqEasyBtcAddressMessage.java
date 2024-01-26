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

import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyBtcAddressMessage extends BisqEasyTradeMessage {
    private final String btcAddress;
    private final BisqEasyOffer bisqEasyOffer;

    public BisqEasyBtcAddressMessage(String id,
                                     String tradeId,
                                     NetworkId sender,
                                     NetworkId receiver,
                                     String btcAddress,
                                     BisqEasyOffer bisqEasyOffer) {
        super(id, tradeId, sender, receiver);

        this.btcAddress = btcAddress;
        this.bisqEasyOffer = bisqEasyOffer;
        verify();
    }

    @Override
    public void verify() {
        super.verify();

        // We tolerate non-btc address data as well (e.g. LN invoice)
        // The minimum possible length of an LN invoice is around 190 characters
        // Max. length depends on optional fields
        NetworkDataValidation.validateText(btcAddress, 1000);
    }

    @Override
    protected bisq.trade.protobuf.TradeMessage toTradeMessageProto() {
        return getTradeMessageBuilder()
                .setBisqEasyTradeMessage(bisq.trade.protobuf.BisqEasyTradeMessage.newBuilder()
                        .setBisqEasyBtcAddressMessage(
                                bisq.trade.protobuf.BisqEasyBtcAddressMessage.newBuilder()
                                        .setBtcAddress(btcAddress)
                                        .setBisqEasyOffer(bisqEasyOffer.toProto())))
                .build();
    }

    public static BisqEasyBtcAddressMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.BisqEasyBtcAddressMessage bisqEasyBtcAddressMessage = proto.getBisqEasyTradeMessage().getBisqEasyBtcAddressMessage();
        return new BisqEasyBtcAddressMessage(
                proto.getId(),
                proto.getTradeId(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                bisqEasyBtcAddressMessage.getBtcAddress(),
                BisqEasyOffer.fromProto(bisqEasyBtcAddressMessage.getBisqEasyOffer()));
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}
