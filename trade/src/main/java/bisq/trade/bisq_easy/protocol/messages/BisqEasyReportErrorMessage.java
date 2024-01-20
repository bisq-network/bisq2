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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyReportErrorMessage extends BisqEasyTradeMessage {
    public static final int MAX_LENGTH = 1000;

    private final String errorMessage;

    public BisqEasyReportErrorMessage(String id,
                                      String tradeId,
                                      NetworkId sender,
                                      NetworkId receiver,
                                      String errorMessage) {
        super(id, tradeId, sender, receiver);
        this.errorMessage = errorMessage;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateText(errorMessage, MAX_LENGTH);
    }

    @Override
    protected bisq.trade.protobuf.TradeMessage toTradeMessageProto() {
        return getTradeMessageBuilder()
                .setBisqEasyTradeMessage(bisq.trade.protobuf.BisqEasyTradeMessage.newBuilder()
                        .setBisqEasyReportErrorMessage(
                                bisq.trade.protobuf.BisqEasyReportErrorMessage.newBuilder()
                                        .setErrorMessage(errorMessage)))
                .build();
    }

    public static BisqEasyReportErrorMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        return new BisqEasyReportErrorMessage(
                proto.getId(),
                proto.getTradeId(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                proto.getBisqEasyTradeMessage().getBisqEasyReportErrorMessage().getErrorMessage());
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}