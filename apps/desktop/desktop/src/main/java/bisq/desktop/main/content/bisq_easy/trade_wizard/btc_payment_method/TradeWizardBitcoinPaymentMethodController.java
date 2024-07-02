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

package bisq.desktop.main.content.bisq_easy.trade_wizard.btc_payment_method;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodUtil;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.PaymentMethodUtil;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import com.google.common.base.Joiner;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TradeWizardBitcoinPaymentMethodController implements Controller {
    private final TradeWizardBitcoinPaymentMethodModel model;
    @Getter
    private final TradeWizardBitcoinPaymentMethodView view;
    private final SettingsService settingsService;
    private final Runnable onNextHandler;
    private final Region owner;

    public TradeWizardBitcoinPaymentMethodController(ServiceProvider serviceProvider, Region owner, Runnable onNextHandler) {
        settingsService = serviceProvider.getSettingsService();
        this.onNextHandler = onNextHandler;
        this.owner = owner;

        model = new TradeWizardBitcoinPaymentMethodModel();
        view = new TradeWizardBitcoinPaymentMethodView(model, this);
    }

    public ObservableList<BitcoinPaymentMethod> getBitcoinPaymentMethods() {
        return model.getSelectedBitcoinPaymentMethods();
    }

    public boolean validate() {
        if (model.getSelectedBitcoinPaymentMethods().isEmpty()) {
            new Popup().invalid(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.noPaymentMethodSelected"))
                    .owner(owner)
                    .show();
            return false;
        } else {
            return true;
        }
    }

    public void setDirection(Direction direction) {
        if (direction != null) {
            model.setDirection(direction);
        }
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        List<BitcoinPaymentMethod> paymentMethods = Stream.of(BitcoinPaymentRail.ONCHAIN,
                        BitcoinPaymentRail.LN,
                        BitcoinPaymentRail.LBTC,
                        BitcoinPaymentRail.RBTC
                )
                .map(BitcoinPaymentMethod::fromPaymentRail)
                .collect(Collectors.toList());
        model.getBitcoinPaymentMethod().setAll(paymentMethods);

        model.setHeadline(model.getDirection().isBuy() ?
                Res.get("bisqEasy.tradeWizard.paymentMethod.btc.headline.buyer") :
                Res.get("bisqEasy.tradeWizard.paymentMethod.btc.headline.seller"));
        model.getSortedBitcoinPaymentMethods().setComparator(Comparator.comparingInt(o -> o.getPaymentRail().ordinal()));
        settingsService.getCookie().asString(CookieKey.CREATE_OFFER_BITCOIN_METHODS)
                .ifPresent(names -> {
                    List.of(names.split(",")).forEach(name -> {
                        if (name.isEmpty()) {
                            return;
                        }
                        BitcoinPaymentMethod bitcoinPaymentMethod = BitcoinPaymentMethodUtil.getPaymentMethod(name);
                        maybeAddBitcoinPaymentMethod(bitcoinPaymentMethod);
                    });
                });
    }

    @Override
    public void onDeactivate() {
    }

    boolean onTogglePaymentMethod(BitcoinPaymentMethod bitcoinPaymentMethod, boolean isSelected) {
        if (isSelected) {
            if (model.getSelectedBitcoinPaymentMethods().size() >= 4) {
                new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.maxMethodsReached")).show();
                return false;
            }
            maybeAddBitcoinPaymentMethod(bitcoinPaymentMethod);
        } else {
            model.getSelectedBitcoinPaymentMethods().remove(bitcoinPaymentMethod);
            setCookie();
        }
        return true;
    }

    private void maybeAddBitcoinPaymentMethod(BitcoinPaymentMethod bitcoinPaymentMethod) {
        if (!model.getSelectedBitcoinPaymentMethods().contains(bitcoinPaymentMethod)) {
            model.getSelectedBitcoinPaymentMethods().add(bitcoinPaymentMethod);
            setCookie();
        }
        if (!model.getBitcoinPaymentMethod().contains(bitcoinPaymentMethod)) {
            model.getBitcoinPaymentMethod().add(bitcoinPaymentMethod);
        }
    }

    private void setCookie() {
        settingsService.setCookie(CookieKey.CREATE_OFFER_BITCOIN_METHODS,
                Joiner.on(",").join(PaymentMethodUtil.getPaymentMethodNames(model.getSelectedBitcoinPaymentMethods())));
    }
}
