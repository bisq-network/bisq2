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

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodUtil;
import javafx.beans.property.ReadOnlyStringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
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

    public void init(BisqEasyOffer bisqEasyOffer, List<String> takersPaymentMethodNames) {
        model.getOfferedMethodNames().setAll(PaymentMethodUtil.getQuoteSidePaymentMethodNames(bisqEasyOffer));

        List<String> matchingNames = bisqEasyOffer.getQuoteSidePaymentMethodSpecs().stream()
                .map(PaymentMethodSpec::getPaymentMethodName)
                .filter(takersPaymentMethodNames::contains)
                .collect(Collectors.toList());
        // We only preselect if there is exactly one match
        if (matchingNames.size() == 1) {
            model.getSelectedMethodName().set(matchingNames.get(0));
        }
    }


    /**
     * @return Enum name of FiatPayment.Method or custom name
     */
    public ReadOnlyStringProperty getSelectedMethodName() {
        return model.getSelectedMethodName();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onSelect(String methodName) {
        model.getSelectedMethodName().set(methodName);
    }

    void onDeselect() {
        model.getSelectedMethodName().set(null);
    }
}
