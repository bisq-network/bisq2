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

package bisq.desktop.overlay.bisq_easy.take_offer.payment_method;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpec;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TakeOfferPaymentController implements Controller {
    private final TakeOfferPaymentModel model;
    @Getter
    private final TakeOfferPaymentView view;

    public TakeOfferPaymentController(ServiceProvider serviceProvider) {
        model = new TakeOfferPaymentModel();
        view = new TakeOfferPaymentView(model, this);
    }

    public void init(BisqEasyOffer bisqEasyOffer, List<FiatPaymentMethod> takersPaymentMethods) {
        List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs = bisqEasyOffer.getQuoteSidePaymentMethodSpecs();
        model.getOfferedSpecs().setAll(quoteSidePaymentMethodSpecs);
        Set<FiatPaymentMethod> takersPaymentMethodSet = new HashSet<>(takersPaymentMethods);
        List<FiatPaymentMethodSpec> matchingPaymentMethodSpecs = quoteSidePaymentMethodSpecs.stream()
                .filter(e -> takersPaymentMethodSet.contains(e.getPaymentMethod()))
                .collect(Collectors.toList());
        // We only preselect if there is exactly one match
        if (matchingPaymentMethodSpecs.size() == 1) {
            model.getSelectedSpec().set(matchingPaymentMethodSpecs.get(0));
        }

        model.setHeadline(bisqEasyOffer.getTakersDirection().isBuy() ?
                Res.get("bisqEasy.takeOffer.method.headline.buyer", bisqEasyOffer.getMarket().getQuoteCurrencyCode()) :
                Res.get("bisqEasy.takeOffer.method.headline.seller", bisqEasyOffer.getMarket().getQuoteCurrencyCode()));
    }

    /**
     * @return Enum name of FiatPayment.Method or custom name
     */
    public ReadOnlyObjectProperty<FiatPaymentMethodSpec> getSelectedFiatPaymentMethodSpec() {
        return model.getSelectedSpec();
    }

    @Override
    public void onActivate() {
        model.getSortedSpecs().setComparator(Comparator.comparing(PaymentMethodSpec::getShortDisplayString));
    }

    @Override
    public void onDeactivate() {
    }

    public void onTogglePaymentMethod(FiatPaymentMethodSpec spec, boolean selected) {
        if (selected && spec != null) {
            model.getSelectedSpec().set(spec);
        } else {
            model.getSelectedSpec().set(null);
        }
    }
}
