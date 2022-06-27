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

package bisq.desktop.primary.overlay.createOffer.amount;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.controls.PriceInput;
import bisq.i18n.Res;
import bisq.offer.spec.Direction;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

// TODO open bug when opening popup with amount screen the values are not set
@Slf4j
public class AmountController implements Controller {
    private final AmountModel model;
    @Getter
    private final AmountView view;
    private final BigAmountInput quoteAmount;
    private final SmallAmountInput baseAmount;
    private final ChangeListener<Monetary> baseCurrencyAmountListener, quoteCurrencyAmountListener;
    private final ChangeListener<Quote> fixPriceQuoteListener;
    private final PriceInput price;
    private final ChangeListener<Number> sliderListener;
    private long minAmount, minMaxDiff;
    private Subscription baseAmountFromModelSubscription, baseAmountFromCompSubscription,
            quoteAmountFromCompSubscription, priceFromCompSubscription;

    public AmountController(DefaultApplicationService applicationService) {
        quoteAmount = new BigAmountInput(false);
        baseAmount = new SmallAmountInput(true);
        price = new PriceInput(applicationService.getMarketPriceService());

        model = new AmountModel();
        view = new AmountView(model, this,
                baseAmount.getRoot(),
                quoteAmount.getRoot());

        // We delay with runLater to avoid that we get triggered at market change from the component's data changes and
        // apply the conversion before the other component has processed the market change event.
        // The order of the event notification is not deterministic. 
        baseCurrencyAmountListener = (observable, oldValue, newValue) -> {
            UIThread.runOnNextRenderFrame(this::setQuoteFromBase);
        };
        quoteCurrencyAmountListener = (observable, oldValue, newValue) -> {
            UIThread.runOnNextRenderFrame(this::setBaseFromQuote);
        };
        fixPriceQuoteListener = (observable, oldValue, newValue) -> {
            UIThread.runOnNextRenderFrame(this::applyFixPrice);
        };

        sliderListener = (observable, oldValue, newValue) -> {
            double sliderValue = newValue.doubleValue();
            long value = Math.round(sliderValue * minMaxDiff) + minAmount;
            Coin amount = Coin.of(value, "BTC");
            baseAmount.setAmount(amount);
        };
    }

    public ReadOnlyObjectProperty<Monetary> getBaseSideAmount() {
        return model.getBaseSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getQuoteSideAmount() {
        return model.getQuoteSideAmount();
    }

    public void setDirection(Direction direction) {
        if (direction == null) {
            return;
        }
        model.setDirection(direction);
        model.getDirectionString().set(Res.get(direction.name().toLowerCase()));
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        model.setMarket(market);
        baseAmount.setSelectedMarket(market);
        quoteAmount.setSelectedMarket(market);
        price.setSelectedMarket(market);
    }

    @Override
    public void onActivate() {
        model.getMinAmount().set(Coin.asBtc(10000));
        model.getMaxAmount().set(Coin.asBtc(1000000));
        model.getSliderMin().set(0);
        model.getSliderMax().set(1);

        model.getBaseSideAmount().addListener(baseCurrencyAmountListener);
        model.getQuoteSideAmount().addListener(quoteCurrencyAmountListener);
        model.getFixPrice().addListener(fixPriceQuoteListener);

        minAmount = model.getMinAmount().get().getValue();
        minMaxDiff = model.getMaxAmount().get().getValue() - minAmount;

        baseAmount.setAmount(null);
        if (model.getQuoteSideAmount().get() == null) {
            //todo we need to adjust to diff fiat exchange rates
            quoteAmount.setAmount(Fiat.parse("100", model.getMarket().getQuoteCurrencyCode()));
        } else {
            quoteAmount.setAmount(model.getQuoteSideAmount().get());
        }
        setBaseFromQuote();

        baseAmountFromModelSubscription = EasyBind.subscribe(model.getBaseSideAmount(), amount -> {
            // Only apply value from component to slider if we have no focus on slider (not used)
            if (amount != null && !model.getSliderFocus().get()) {
                double sliderValue = (amount.getValue() - minAmount) / (double) minMaxDiff;
                model.getSliderValue().set(sliderValue);
            }
        });

        baseAmountFromCompSubscription = EasyBind.subscribe(baseAmount.amountProperty(),
                amount -> model.getBaseSideAmount().set(amount));
        quoteAmountFromCompSubscription = EasyBind.subscribe(quoteAmount.amountProperty(),
                amount -> model.getQuoteSideAmount().set(amount));
        priceFromCompSubscription = EasyBind.subscribe(price.fixPriceProperty(),
                price -> model.getFixPrice().set(price));

        model.getSliderValue().addListener(sliderListener);
    }

    @Override
    public void onDeactivate() {
        model.getBaseSideAmount().removeListener(baseCurrencyAmountListener);
        model.getQuoteSideAmount().removeListener(quoteCurrencyAmountListener);
        model.getFixPrice().removeListener(fixPriceQuoteListener);
        model.getSliderValue().removeListener(sliderListener);
        baseAmountFromModelSubscription.unsubscribe();
        baseAmountFromCompSubscription.unsubscribe();
        quoteAmountFromCompSubscription.unsubscribe();
        priceFromCompSubscription.unsubscribe();
    }

    private void setQuoteFromBase() {
        Quote fixPrice = model.getFixPrice().get();
        if (fixPrice == null) return;
        Monetary baseCurrencyAmount = model.getBaseSideAmount().get();
        if (baseCurrencyAmount == null) return;
        if (fixPrice.getBaseMonetary().getClass() != baseCurrencyAmount.getClass()) return;

        Monetary exactAmount = fixPrice.toQuoteMonetary(baseCurrencyAmount);
        quoteAmount.setAmount( exactAmount.round(0));
    }

    private void setBaseFromQuote() {
        Quote fixPrice = model.getFixPrice().get();
        if (fixPrice == null) return;
        Monetary quoteCurrencyAmount = model.getQuoteSideAmount().get();
        if (quoteCurrencyAmount == null) return;
        if (fixPrice.getQuoteMonetary().getClass() != quoteCurrencyAmount.getClass()) return;
        baseAmount.setAmount(fixPrice.toBaseMonetary(quoteCurrencyAmount));
    }

    private void applyFixPrice() {
        if (model.getBaseSideAmount() == null) {
            setBaseFromQuote();
        } else {
            setQuoteFromBase();
        }
    }
}
