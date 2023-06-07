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
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
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
    private final AmountComponent minAmountComponent, maxAmountComponent;
    private final SettingsService settingsService;
    private Subscription isMinAmountEnabledPin, maxAmountBaseSideAmountPin, minAmountBaseSideAmountPin,
            maxAmountQuoteSideAmountPin, minAmountQuoteSideAmountPin;

    public AmountController(DefaultApplicationService applicationService) {
        settingsService = applicationService.getSettingsService();
        model = new AmountModel();

        minAmountComponent = new AmountComponent(applicationService, model.getMinAmountDescription());
        maxAmountComponent = new AmountComponent(applicationService, model.getMaxAmountDescription());

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

        maxAmountBaseSideAmountPin = EasyBind.subscribe(maxAmountComponent.getBaseSideAmount(),
                maxAmountBaseSideAmount -> {
                    if (model.getIsMinAmountEnabled().get() &&
                            maxAmountBaseSideAmount != null && minAmountComponent.getBaseSideAmount().get() != null &&
                            maxAmountBaseSideAmount.getValue() < minAmountComponent.getBaseSideAmount().get().getValue()) {
                        minAmountComponent.setBaseSideAmount(maxAmountBaseSideAmount);
                    }
                });
        maxAmountQuoteSideAmountPin = EasyBind.subscribe(maxAmountComponent.getQuoteSideAmount(),
                maxAmountQuoteSideAmount -> {
                    if (model.getIsMinAmountEnabled().get() &&
                            maxAmountQuoteSideAmount != null && minAmountComponent.getQuoteSideAmount().get() != null &&
                            maxAmountQuoteSideAmount.getValue() < minAmountComponent.getQuoteSideAmount().get().getValue()) {
                        minAmountComponent.setQuoteSideAmount(maxAmountQuoteSideAmount);
                    }
                });

        minAmountBaseSideAmountPin = EasyBind.subscribe(minAmountComponent.getBaseSideAmount(),
                minAmountBaseSideAmount -> {
                    if (model.getIsMinAmountEnabled().get()) {
                        if (minAmountBaseSideAmount != null && maxAmountComponent.getBaseSideAmount().get() != null &&
                                minAmountBaseSideAmount.getValue() > maxAmountComponent.getBaseSideAmount().get().getValue()) {
                            maxAmountComponent.setBaseSideAmount(minAmountBaseSideAmount);
                        }
                    }
                });
        minAmountQuoteSideAmountPin = EasyBind.subscribe(minAmountComponent.getQuoteSideAmount(),
                minAmountQuoteSideAmount -> {
                    if (model.getIsMinAmountEnabled().get()) {
                        if (minAmountQuoteSideAmount != null && maxAmountComponent.getQuoteSideAmount().get() != null &&
                                minAmountQuoteSideAmount.getValue() > maxAmountComponent.getQuoteSideAmount().get().getValue()) {
                            maxAmountComponent.setQuoteSideAmount(minAmountQuoteSideAmount);
                        }
                    }
                });

        isMinAmountEnabledPin = EasyBind.subscribe(model.getIsMinAmountEnabled(), isMinAmountEnabled -> {
            model.getToggleButtonText().set(isMinAmountEnabled ?
                    Res.get("onboarding.amount.removeMinAmountOption") :
                    Res.get("onboarding.amount.addMinAmountOption"));
            model.getMinAmountDescription().set(Res.get("onboarding.amount.description.minAmount"));
            model.getMaxAmountDescription().set(isMinAmountEnabled ?
                    Res.get("onboarding.amount.description.maxAmount") :
                    Res.get("onboarding.amount.description.maxAmountOnly"));
        });
    }

    @Override
    public void onDeactivate() {
        isMinAmountEnabledPin.unsubscribe();
        maxAmountBaseSideAmountPin.unsubscribe();
        maxAmountQuoteSideAmountPin.unsubscribe();
        minAmountBaseSideAmountPin.unsubscribe();
        minAmountQuoteSideAmountPin.unsubscribe();
    }

    void onToggleMinAmountVisibility() {
        boolean value = !model.getIsMinAmountEnabled().get();
        model.getIsMinAmountEnabled().set(value);
        settingsService.setCookie(CookieKey.CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED, value);
    }
}
