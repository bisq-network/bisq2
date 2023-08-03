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
import bisq.network.NetworkId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyAccountDataMessage extends BisqEasyTradeMessage {
    public final static int MAX_LENGTH = 1000;

    private final String paymentAccountData;

    public BisqEasyAccountDataMessage(String tradeId, NetworkId sender, String paymentAccountData) {
        super(tradeId, sender);

        this.paymentAccountData = paymentAccountData;

        NetworkDataValidation.validateText(paymentAccountData, MAX_LENGTH);
        // log.error("{} {}", metaData.getClassName(), toProto().getSerializedSize()); //631
    }

    @Override
    protected bisq.trade.protobuf.TradeMessage toTradeMessageProto() {
        return getTradeMessageBuilder()
                .setBisqEasyTradeMessage(bisq.trade.protobuf.BisqEasyTradeMessage.newBuilder()
                        .setBisqEasyAccountDataMessage(
                                bisq.trade.protobuf.BisqEasyAccountDataMessage.newBuilder()
                                        .setPaymentAccountData(paymentAccountData)))
                .build();
    }

    public static BisqEasyAccountDataMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.BisqEasyAccountDataMessage bisqEasyAccountDataMessage = proto.getBisqEasyTradeMessage().getBisqEasyAccountDataMessage();
        return new BisqEasyAccountDataMessage(
                proto.getTradeId(),
                NetworkId.fromProto(proto.getSender()),
                bisqEasyAccountDataMessage.getPaymentAccountData());
    }
}