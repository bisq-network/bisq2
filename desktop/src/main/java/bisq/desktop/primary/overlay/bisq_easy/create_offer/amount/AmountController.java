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

package bisq.desktop.primary.overlay.bisq_easy.create_offer.amount;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.overlay.bisq_easy.components.AmountComponent;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.price_spec.FixPriceSpec;
import bisq.offer.price_spec.FloatPriceSpec;
import bisq.offer.price_spec.PriceSpec;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class AmountController implements Controller {
    private final AmountModel model;
    @Getter
    private final AmountView view;
    private final AmountComponent minAmountComponent, maxAmountComponent;
    private final SettingsService settingsService;
    private final MarketPriceService marketPriceService;
    private Subscription isMinAmountEnabledPin, maxAmountCompBaseSideAmountPin, minAmountCompBaseSideAmountPin,
            maxAmountCompQuoteSideAmountPin, minAmountCompQuoteSideAmountPin;

    public AmountController(DefaultApplicationService applicationService) {
        settingsService = applicationService.getSettingsService();
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
        model = new AmountModel();

        minAmountComponent = new AmountComponent(applicationService, true);
        minAmountComponent.setDescription(Res.get("onboarding.amount.description.minAmount"));
        maxAmountComponent = new AmountComponent(applicationService, true);

        view = new AmountView(model, this,
                minAmountComponent,
                maxAmountComponent);
    }

    public void setDirection(Direction direction) {
        if (direction == null) {
            return;
        }
        minAmountComponent.setDirection(direction);
        maxAmountComponent.setDirection(direction);
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        minAmountComponent.setMarket(market);
        maxAmountComponent.setMarket(market);
        model.setMarket(market);
    }

    public void setPriceSpec(PriceSpec priceSpec) {
        if (priceSpec instanceof FixPriceSpec) {
            Quote quote = ((FixPriceSpec) priceSpec).getQuote();
            minAmountComponent.setQuote(quote);
            maxAmountComponent.setQuote(quote);
        } else if (priceSpec instanceof FloatPriceSpec) {
            double percentage = ((FloatPriceSpec) priceSpec).getPercentage();
           /* findMarketPriceQuote().map(marketPrice->{
                
            });*/
        } else {
            
     /*   }
        Quote quote = PriceSpec.findFixPriceSpec(priceSpec)
                .map(FixPriceSpec::getQuote).orElse(
                        PriceSpec.findFloatPriceSpec(priceSpec)
                                .map(FloatPriceSpec::getPercentage)
                                .map(p -> Quote.fromPrice(0L, null))).orElse(
                        Quote.fromPrice(0, null)
                );*/
        }
    }

    private Optional<Quote> findMarketPriceQuote() {
        return marketPriceService.findMarketPrice(model.getMarket()).map(MarketPrice::getQuote).stream().findAny();
    }

    public void setSellerPriceQuote(Quote sellersPriceQuote) {
        minAmountComponent.setQuote(sellersPriceQuote);
        maxAmountComponent.setQuote(sellersPriceQuote);
    }

    public void reset() {
        minAmountComponent.reset();
        maxAmountComponent.reset();
        model.reset();
    }

    public ReadOnlyObjectProperty<Monetary> getBaseSideMinAmount() {
        return minAmountComponent.getBaseSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getQuoteSideMinAmount() {
        return minAmountComponent.getQuoteSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getBaseSideMaxAmount() {
        return maxAmountComponent.getBaseSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getQuoteSideMaxAmount() {
        return maxAmountComponent.getQuoteSideAmount();
    }

    public ReadOnlyBooleanProperty getIsMinAmountEnabled() {
        return model.getIsMinAmountEnabled();
    }

    @Override
    public void onActivate() {
        model.getIsMinAmountEnabled().set(settingsService.getCookie().asBoolean(CookieKey.CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED).orElse(false));

        minAmountCompBaseSideAmountPin = EasyBind.subscribe(minAmountComponent.getBaseSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get()) {
                        if (value != null && maxAmountComponent.getBaseSideAmount().get() != null &&
                                value.getValue() > maxAmountComponent.getBaseSideAmount().get().getValue()) {
                            maxAmountComponent.setBaseSideAmount(value);
                        }
                    }
                });
        maxAmountCompBaseSideAmountPin = EasyBind.subscribe(maxAmountComponent.getBaseSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get() &&
                            value != null && minAmountComponent.getBaseSideAmount().get() != null &&
                            value.getValue() < minAmountComponent.getBaseSideAmount().get().getValue()) {
                        minAmountComponent.setBaseSideAmount(value);
                    }
                });

        minAmountCompQuoteSideAmountPin = EasyBind.subscribe(minAmountComponent.getQuoteSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get()) {
                        if (value != null && maxAmountComponent.getQuoteSideAmount().get() != null &&
                                value.getValue() > maxAmountComponent.getQuoteSideAmount().get().getValue()) {
                            maxAmountComponent.setQuoteSideAmount(value);
                        }
                    }
                });
        maxAmountCompQuoteSideAmountPin = EasyBind.subscribe(maxAmountComponent.getQuoteSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get() &&
                            value != null && minAmountComponent.getQuoteSideAmount().get() != null &&
                            value.getValue() < minAmountComponent.getQuoteSideAmount().get().getValue()) {
                        minAmountComponent.setQuoteSideAmount(value);
                    }
                });


        isMinAmountEnabledPin = EasyBind.subscribe(model.getIsMinAmountEnabled(), isMinAmountEnabled -> {
            model.getToggleButtonText().set(isMinAmountEnabled ?
                    Res.get("onboarding.amount.removeMinAmountOption") :
                    Res.get("onboarding.amount.addMinAmountOption"));

            maxAmountComponent.setDescription(isMinAmountEnabled ?
                    Res.get("onboarding.amount.description.maxAmount") :
                    Res.get("onboarding.amount.description.maxAmountOnly"));
        });
    }

    @Override
    public void onDeactivate() {
        isMinAmountEnabledPin.unsubscribe();
        maxAmountCompBaseSideAmountPin.unsubscribe();
        maxAmountCompQuoteSideAmountPin.unsubscribe();
        minAmountCompBaseSideAmountPin.unsubscribe();
        minAmountCompQuoteSideAmountPin.unsubscribe();
    }

    void onToggleMinAmountVisibility() {
        boolean value = !model.getIsMinAmountEnabled().get();
        model.getIsMinAmountEnabled().set(value);
        settingsService.setCookie(CookieKey.CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED, value);
    }
}
