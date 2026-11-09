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

import bisq.common.validation.RequestedAmountValidation;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.handler.BisqEasyTradeMessageHandler;
import bisq.trade.bisq_easy.validation.BisqEasyBitcoinPaymentDataValidator;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class BisqEasyBtcAddressMessageHandler extends BisqEasyTradeMessageHandler<BisqEasyTrade, BisqEasyBtcAddressMessage> {
    private String bitcoinPaymentData;

    public BisqEasyBtcAddressMessageHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(BisqEasyBtcAddressMessage message) {
        String bitcoinPaymentData = message.getBitcoinPaymentData();
        // Invalid data is a mistake of the buyer, not a bug, so the trade fails with an expected failure (#5210)
        BisqEasyBitcoinPaymentDataValidator.validate(bitcoinPaymentData,
                trade.getContract().getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail());
        // The seller's wallet pays the amount which the data requests, for example from its QR code
        // (docs/specifications/trade/bisq-easy-bitcoin-payment-data.md)
        RequestedAmountValidation.checkRequestsOnlyAmount(bitcoinPaymentData, trade.getContract().getBaseSideAmount());

        checkNotNull(message.getBisqEasyOffer(), "BisqEasyOffer must not be null");
    }

    @Override
    protected void process(BisqEasyBtcAddressMessage message) {
        bitcoinPaymentData = message.getBitcoinPaymentData();
    }

    @Override
    protected void commit() {
        trade.setBitcoinPaymentData(Optional.of(bitcoinPaymentData));
    }
}