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

package bisq.desktop.main.content.bisq_easy.trade_wizard.review;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.PriceSpec;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

@Getter
class TradeWizardReviewModel implements Model {
    @Setter
    private boolean isCreateOfferMode;
    @Setter
    private BisqEasyOffer bisqEasyOffer;
    @Setter
    private BisqEasyTrade bisqEasyTrade;
    @Setter
    private BisqEasyOfferbookChannel selectedChannel;
    @Setter
    private FiatPaymentMethod takersSelectedPaymentMethod;
    @Setter
    private Monetary minBaseSideAmount;
    @Setter
    private Monetary maxBaseSideAmount;
    @Setter
    private Monetary fixBaseSideAmount;
    @Setter
    private Monetary minQuoteSideAmount;
    @Setter
    private Monetary maxQuoteSideAmount;
    @Setter
    private Monetary fixQuoteSideAmount;
    @Setter
    private PriceSpec priceSpec;
    @Setter
    private BisqEasyOfferbookMessage myOfferMessage;
    @Setter
    private String headline;
    @Setter
    private String directionHeadline;
    @Setter
    private String minAmountsHeadline;
    @Setter
    private String maxAmountsHeadline;
    @Setter
    private String fixAmountsHeadline;
    @Setter
    private String detailsHeadline;
    @Setter
    private String toSendAmountDescription;
    @Setter
    private String toSendAmount;
    @Setter
    private String toReceiveAmountDescription;
    @Setter
    private String toReceiveAmount;
    @Setter
    private String priceDescription;
    @Setter
    private String price;
    @Setter
    private String priceDetails;
    @Setter
    private String paymentMethodDescription;
    @Setter
    private String paymentMethod;
    @Setter
    private String fee;
    private final ObservableList<FiatPaymentMethod> takersPaymentMethods = FXCollections.observableArrayList();
    private final BooleanProperty showCreateOfferSuccess = new SimpleBooleanProperty();
    private final BooleanProperty showTakeOfferSuccess = new SimpleBooleanProperty();

    public void reset() {
        isCreateOfferMode = false;
        bisqEasyOffer = null;
        bisqEasyTrade = null;
        selectedChannel = null;
        takersSelectedPaymentMethod = null;
        minBaseSideAmount = null;
        maxBaseSideAmount = null;
        fixBaseSideAmount = null;
        minQuoteSideAmount = null;
        maxQuoteSideAmount = null;
        fixQuoteSideAmount = null;
        priceSpec = null;
        myOfferMessage = null;
        headline = null;
        directionHeadline = null;
        minAmountsHeadline = null;
        maxAmountsHeadline = null;
        fixAmountsHeadline = null;
        detailsHeadline = null;
        toSendAmountDescription = null;
        toSendAmount = null;
        toReceiveAmountDescription = null;
        toReceiveAmount = null;
        priceDescription = null;
        price = null;
        priceDetails = null;
        paymentMethodDescription = null;
        paymentMethod = null;
        fee = null;
        takersPaymentMethods.clear();
        showCreateOfferSuccess.set(false);
        showTakeOfferSuccess.set(false);

    }
}