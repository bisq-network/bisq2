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
import bisq.common.currency.BisqCurrency;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.common.monetary.QuoteCodePair;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class CreateOfferController implements Controller, MarketPriceService.Listener {
    private final CreateOfferModel model;
    @Getter
    private final CreateOfferView view;
    private final ChangeListener<Monetary> askListener, bidListener;
    private final ChangeListener<Quote> fixPriceQuoteListener;
    private final ChangeListener<BisqCurrency> selectedAskCurrencyListener, selectedBidCurrencyListener;
    private final MarketPriceService marketPriceService;

    public CreateOfferController(DefaultServiceProvider serviceProvider) {
        marketPriceService = serviceProvider.getMarketPriceService();
        model = new CreateOfferModel(serviceProvider);

        MonetaryInput.MonetaryController ask = new MonetaryInput.MonetaryController(model.getCurrencies(),
                model.getAsk(),
                model.getSelectedAskCurrency(),
                Res.offerbook.get("createOffer.ask.description"),
                Res.offerbook.get("createOffer.ask.prompt"));
        MonetaryInput.MonetaryController bid = new MonetaryInput.MonetaryController(model.getCurrencies(),
                model.getBid(),
                model.getSelectedBidCurrency(),
                Res.offerbook.get("createOffer.bid.description"),
                Res.offerbook.get("createOffer.bid.prompt"));
        PriceInput.PriceController price = new PriceInput.PriceController(serviceProvider.getMarketPriceService(),
                model.getFixPriceQuote(),
                model.getBaseCurrencyCode(),
                model.getQuoteCurrencyCode(),
                Res.offerbook.get("createOffer.price.fix.description"));
        view = new CreateOfferView(model, this, ask.getView(), bid.getView(), price.getView());

        askListener = (observable, oldValue, newValue) -> {
            log.error("amountListener {}", newValue);
            updateBidFromAsk();
        };
        bidListener = (observable, oldValue, newValue) -> {
            log.error("volumeListener {}", newValue);
            updateAskFromBid();
        };
        fixPriceQuoteListener = (observable, oldValue, newValue) -> {
            log.error("fixPriceQuoteListener {}", newValue);
            if (model.getAsk().get() != null && model.getBid().get() == null) {
                updateBidFromAsk();
            } else if (model.getBid().get() != null && model.getAsk().get() == null) {
                updateAskFromBid();
            } else {
                //todo use last selected side to update the opposite side
                updateBidFromAsk();
            }
        };

        selectedAskCurrencyListener = (observable, oldValue, newValue) -> {
            updateBaseAndQuoteCurrencyCodes(newValue.getCode(), true);
            updateBidFromAsk();
        };
        selectedBidCurrencyListener = (observable, oldValue, newValue) -> {
            updateBaseAndQuoteCurrencyCodes(newValue.getCode(), false);
            updateAskFromBid();
        };
    }

    private void updateBaseAndQuoteCurrencyCodes(String code, boolean isAsk) {
        Optional<MarketPrice> marketPriceOptional = marketPriceService.getMarketPrice(model.getQuoteCodePair().get());
        if (marketPriceOptional.isPresent()) {
            MarketPrice marketPrice = marketPriceOptional.get();
            model.getFixPriceQuote().set(marketPrice.quote());
            if (marketPrice.quote().getBaseMonetary().getCode().equals(code)) {
                model.getBaseCurrencyCode().set(code);
                model.getQuoteCurrencyCode().set(marketPrice.quote().getQuoteMonetary().getCode());
            } else if (marketPrice.quote().getQuoteMonetary().getCode().equals(code)) {
                model.getQuoteCurrencyCode().set(code);
                model.getBaseCurrencyCode().set(marketPrice.quote().getBaseMonetary().getCode());
            } else {
                log.error("No market price available for that currency pair");
            }
        } else {
            // We do not have a market price for that currency pair
            // We try to guess what base and quote currency is from what seems to be the dominant currency
            // todo 
        }
    }

    private void updateBidFromAsk() {
        if (model.getAsk().get() != null &&
                model.getFixPriceQuote().get() != null &&
                model.getBaseCurrencyCode().get() != null &&
                model.getQuoteCurrencyCode().get() != null) {
            if (model.getAsk().get().getCode().equals(model.getBaseCurrencyCode().get())) {
                model.getBid().set(model.getFixPriceQuote().get().toQuoteMonetary(model.getAsk().get()));
            } else if (model.getAsk().get().getCode().equals(model.getQuoteCurrencyCode().get())) {
                model.getBid().set(model.getFixPriceQuote().get().toBaseMonetary(model.getAsk().get()));
            }
        }
    }

    private void updateAskFromBid() {
        if (model.getBid().get() != null &&
                model.getFixPriceQuote().get() != null &&
                model.getBaseCurrencyCode().get() != null &&
                model.getQuoteCurrencyCode().get() != null) {
            if (model.getBid().get().getCode().equals(model.getBaseCurrencyCode().get())) {
                model.getAsk().set(model.getFixPriceQuote().get().toQuoteMonetary(model.getBid().get()));
            } else if (model.getBid().get().getCode().equals(model.getQuoteCurrencyCode().get())) {
                model.getAsk().set(model.getFixPriceQuote().get().toBaseMonetary(model.getBid().get()));
            }
        }
    }


    @Override
    public void onViewAttached() {
        marketPriceService.addListener(this);
        model.getAsk().addListener(askListener);
        model.getBid().addListener(bidListener);
        model.getFixPriceQuote().addListener(fixPriceQuoteListener);
        model.getSelectedAskCurrency().addListener(selectedAskCurrencyListener);
        model.getSelectedBidCurrency().addListener(selectedBidCurrencyListener);
    }

    @Override
    public void onViewDetached() {
        marketPriceService.removeListener(this);
        model.getAsk().removeListener(askListener);
        model.getBid().removeListener(bidListener);
        model.getFixPriceQuote().removeListener(fixPriceQuoteListener);
        model.getSelectedAskCurrency().removeListener(selectedAskCurrencyListener);
        model.getSelectedBidCurrency().removeListener(selectedBidCurrencyListener);
    }

    @Override
    public void onMarketPriceUpdate(Map<QuoteCodePair, MarketPrice> map) {
        log.error("");
    }

    @Override
    public void onMarketPriceSelected(MarketPrice selected) {
        UIThread.run(() -> {
            if (selected != null && model.getQuoteCodePair().get() == null) {
                model.getQuoteCodePair().set(selected.quote().getQuoteCodePair());
                model.getFixPriceQuote().set(selected.quote());
                updateBaseAndQuoteCurrencyCodes(selected.code(), true);
                updateBidFromAsk();
            }
        });
    }
}
