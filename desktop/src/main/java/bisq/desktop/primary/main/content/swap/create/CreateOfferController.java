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

package bisq.desktop.primary.main.content.swap.create;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.offer.protocol.SwapProtocolType;
import bisq.oracle.marketprice.MarketPriceService;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateOfferController implements Controller {
    private final MarketPriceService marketPriceService;
    private final CreateOfferModel model;
    @Getter
    private final CreateOfferView view;

    private final ChangeListener<SwapProtocolType> selectedProtocolListener;

    public CreateOfferController(DefaultServiceProvider serviceProvider) {
        marketPriceService = serviceProvider.getMarketPriceService();
        model = new CreateOfferModel(serviceProvider);

        var marketSelection = new MarketSelection.MarketSelectionController(marketPriceService, model.getSelectedMarket());

        var direction = new DirectionSelection.DirectionController(model.getSelectedMarket(), model.getDirection());

        var amountPriceController = new AmountPriceGroup.AmountPriceController(serviceProvider.getMarketPriceService(),
                model.getSelectedMarket(),
                model.getDirection(),
                model.getBaseCurrencyAmount(),
                model.getQuoteCurrencyAmount(),
                model.getFixPriceQuote()
        );

        var protocolSelection = new ProtocolSelection.ProtocolController(model.getSelectedMarket(), model.getSelectedProtocol());

        var settlementSelection = new SettlementSelection.SettlementController(model.getSelectedMarket(),
                model.getSelectedProtocol(),
                model.getAskSettlementMethods(),
                model.getBidSettlementMethods(),
                model.getAskSelectedSettlementMethod(),
                model.getBidSelectedSettlementMethod());
        view = new CreateOfferView(model, this,
                marketSelection.getView(),
                direction.getView(),
                amountPriceController.getView(),
                protocolSelection.getView(),
                settlementSelection.getView());


        selectedProtocolListener = (observable, oldValue, newValue) -> {
            log.error("selectedProtocolListener {}", newValue);
          /*  if (model.getSelectedBaseCurrency().get() != null) {
                if (model.getSelectedBaseCurrency().get().isFiat()) {
                    model.getAskSettlementMethods().setAll(ProtocolSpecifics.getFiatSettlementMethods(newValue));
                } else {
                    model.getAskSettlementMethods().setAll(ProtocolSpecifics.getCryptoSettlementMethods(newValue));
                }
            }
            if (model.getSelectedQuoteCurrency().get() != null) {
                if (model.getSelectedQuoteCurrency().get().isFiat()) {
                    model.getBidSettlementMethods().setAll(ProtocolSpecifics.getFiatSettlementMethods(newValue));
                } else {
                    model.getBidSettlementMethods().setAll(ProtocolSpecifics.getCryptoSettlementMethods(newValue));
                }
            }*/
        };
    }

    @Override
    public void onViewAttached() {
        model.getSelectedProtocol().addListener(selectedProtocolListener);
    }

    @Override
    public void onViewDetached() {
        model.getSelectedProtocol().removeListener(selectedProtocolListener);
    }

  /*  private void setProtocolsFromQuoteCodePair() {
        if (model.getSelectedMarket().get() != null) {
            model.getProtocols().setAll(ProtocolSpecifics.getProtocols(model.getSelectedMarket().get()));
        }
    }*/

}
