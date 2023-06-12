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

package bisq.desktop.primary.overlay.bisq_easy.take_offer.payment_method;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
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

    public TakeOfferPaymentController(DefaultApplicationService applicationService) {
        model = new TakeOfferPaymentModel();
        view = new TakeOfferPaymentView(model, this);
    }

    public void init(BisqEasyOffer bisqEasyOffer, List<FiatPaymentMethod> takersPaymentMethods) {
        List<FiatPaymentMethod> fiatPaymentMethods = PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.getQuoteSidePaymentMethodSpecs());
        model.getOfferedFiatPaymentMethods().setAll(fiatPaymentMethods);
        Set<FiatPaymentMethod> takersPaymentMethodSet = new HashSet<>(takersPaymentMethods);
        List<FiatPaymentMethod> matchingPaymentMethods = bisqEasyOffer.getQuoteSidePaymentMethodSpecs().stream()
                .map(PaymentMethodSpec::getPaymentMethod)
                .filter(takersPaymentMethodSet::contains)
                .collect(Collectors.toList());
        // We only preselect if there is exactly one match
        if (matchingPaymentMethods.size() == 1) {
            model.getSelectedFiatPaymentMethod().set(matchingPaymentMethods.get(0));
        }
    }


    /**
     * @return Enum name of FiatPayment.Method or custom name
     */
    public ReadOnlyObjectProperty<FiatPaymentMethod> getSelectedFiatPaymentMethod() {
        return model.getSelectedFiatPaymentMethod();
    }

    @Override
    public void onActivate() {
        model.getSortedOfferedFiatPaymentMethods().setComparator(Comparator.comparing(PaymentMethod::getShortDisplayString));
    }

    @Override
    public void onDeactivate() {
    }

    public void onTogglePaymentMethod(FiatPaymentMethod fiatPaymentMethod, boolean selected) {
        if (selected && fiatPaymentMethod != null) {
            model.getSelectedFiatPaymentMethod().set(fiatPaymentMethod);
        } else {
            model.getSelectedFiatPaymentMethod().set(null);
        }
    }
}
