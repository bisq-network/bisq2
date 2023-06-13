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

package bisq.desktop.primary.main.content.trade.multisig.old.closedTrades;

import bisq.common.currency.TradeCurrency;
import bisq.common.monetary.PriceQuote;
import bisq.contract.poc.PocContract;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.poc.PocOffer;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.protocol.poc.PocProtocol;
import bisq.protocol.poc.PocProtocolModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClosedTradeListItem implements TableItem {
    @EqualsAndHashCode.Include
    private final String id;
    private final PocOffer offer;
    private final String market;
    private final String price;
    private final String baseAmount;
    private final String quoteAmount;
    private final String paymentMethod;
    private final String options;
    private final PocProtocol<? extends PocProtocolModel> protocol;

    public ClosedTradeListItem(PocProtocol<? extends PocProtocolModel> protocol) {
        this.protocol = protocol;
        PocContract contract = protocol.getContract();
        offer = contract.getOffer();
        id = offer.getId();

        market = offer.getMarket().toString();
        baseAmount = AmountFormatter.formatAmount(contract.getBaseSideAmount());
        quoteAmount = AmountFormatter.formatAmount(contract.getQuoteSideAmount());
        price = PriceFormatter.format(PriceQuote.from(contract.getBaseSideAmount(), contract.getQuoteSideAmount()));

        String baseSidePayment = offer.getBaseSidePaymentMethodSpecs().stream()
                .map(PaymentMethodSpec::getPaymentMethodName)
                .map(Res::get)
                .collect(Collectors.joining("\n"));
        String quoteSidePayment = offer.getQuoteSidePaymentMethodSpecs().stream()
                .map(PaymentMethodSpec::getPaymentMethodName)
                .map(Res::get)
                .collect(Collectors.joining("\n"));

        String baseCurrencyCode = offer.getMarket().getBaseCurrencyCode();
        String quoteCurrencyCode = offer.getMarket().getQuoteCurrencyCode();

        boolean isBaseCurrencyFiat = TradeCurrency.isFiat(baseCurrencyCode);
        boolean isQuoteCurrencyFiat = TradeCurrency.isFiat(quoteCurrencyCode);

        boolean isBaseSideFiatOrMultiple = isBaseCurrencyFiat || offer.getBaseSidePaymentMethodSpecs().size() > 1;
        boolean isQuoteSideFiatOrMultiple = isQuoteCurrencyFiat || offer.getQuoteSidePaymentMethodSpecs().size() > 1;
        if (isBaseSideFiatOrMultiple && !isQuoteSideFiatOrMultiple) {
            paymentMethod = baseSidePayment;
        } else if (isQuoteSideFiatOrMultiple && !isBaseSideFiatOrMultiple) {
            paymentMethod = quoteSidePayment;
        } else if (isBaseSideFiatOrMultiple) {
            // both
            paymentMethod = Res.get("offerbook.table.paymentMethod.multi",
                    baseCurrencyCode, baseSidePayment, quoteCurrencyCode, quoteSidePayment);
        } else {
            paymentMethod = "";
        }

        options = ""; //todo
    }


    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
}
