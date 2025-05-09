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

package bisq.desktop.main.content.mu_sig.take_offer.payment_methods;

import bisq.account.payment_method.FiatPaymentRail;
import bisq.common.util.ExceptionUtil;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class MuSigTakeOfferPaymentController implements Controller {
    private final MuSigTakeOfferPaymentModel model;
    @Getter
    private final MuSigTakeOfferPaymentView view;
    private final SettingsService settingsService;

    public MuSigTakeOfferPaymentController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();

        model = new MuSigTakeOfferPaymentModel();
        view = new MuSigTakeOfferPaymentView(model, this);
    }

    public void init(MuSigOffer bisqEasyOffer) {
        model.setMarket(bisqEasyOffer.getMarket());
        model.getOfferedFiatPaymentMethodSpecs().setAll(bisqEasyOffer.getQuoteSidePaymentMethodSpecs());
        model.setSubtitle(bisqEasyOffer.getTakersDirection().isBuy()
                ? Res.get("bisqEasy.takeOffer.paymentMethods.subtitle.fiat.buyer", bisqEasyOffer.getMarket().getQuoteCurrencyCode())
                : Res.get("bisqEasy.takeOffer.paymentMethods.subtitle.fiat.seller", bisqEasyOffer.getMarket().getQuoteCurrencyCode()));
    }

    public ReadOnlyObjectProperty<FiatPaymentMethodSpec> getSelectedFiatPaymentMethodSpec() {
        return model.getSelectedFiatPaymentMethodSpec();
    }

    public boolean isValid() {
        return model.getSelectedFiatPaymentMethodSpec().get() != null;
    }

    public void handleInvalidInput() {
        if (model.getSelectedFiatPaymentMethodSpec().get() == null) {
            new Popup().invalid(Res.get("bisqEasy.tradeWizard.paymentMethods.warn.noFiatPaymentMethodSelected"))
                    .owner((Region) view.getRoot().getParent().getParent())
                    .show();
        }
    }

    @Override
    public void onActivate() {
        model.getSortedFiatPaymentMethodSpecs().setComparator(Comparator.comparing(PaymentMethodSpec::getShortDisplayString));
        model.setFiatMethodVisible(model.getOfferedFiatPaymentMethodSpecs().size() > 1);
        if (model.getOfferedFiatPaymentMethodSpecs().size() == 1) {
            model.getSelectedFiatPaymentMethodSpec().set(model.getOfferedFiatPaymentMethodSpecs().get(0));
        }
        model.setHeadline(Res.get("bisqEasy.takeOffer.paymentMethods.headline.fiat"));

        settingsService.getCookie().asString(CookieKey.TAKE_OFFER_SELECTED_FIAT_METHOD, getCookieSubKey())
                .ifPresent(name -> {
                    try {
                        FiatPaymentRail persisted = FiatPaymentRail.valueOf(FiatPaymentRail.class, name);
                        model.getOfferedFiatPaymentMethodSpecs().stream()
                                .filter(spec -> spec.getPaymentMethod().getPaymentRail() == persisted).findAny()
                                .ifPresent(spec -> model.getSelectedFiatPaymentMethodSpec().set(spec));
                    } catch (Exception e) {
                        log.warn("Could not create FiatPaymentRail from persisted name {}. {}", name, ExceptionUtil.getRootCauseMessage(e));
                    }
                });
    }

    @Override
    public void onDeactivate() {
    }

    void onToggleFiatPaymentMethod(FiatPaymentMethodSpec spec, boolean selected) {
        if (selected && spec != null) {
            model.getSelectedFiatPaymentMethodSpec().set(spec);
            settingsService.setCookie(CookieKey.TAKE_OFFER_SELECTED_FIAT_METHOD, getCookieSubKey(),
                    spec.getPaymentMethod().getPaymentRail().name());
        } else {
            model.getSelectedFiatPaymentMethodSpec().set(null);
        }
    }

    private String getCookieSubKey() {
        return model.getMarket().getMarketCodes();
    }
}
