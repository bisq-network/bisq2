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

package bisq.desktop.main.content.mu_sig.offer.draft.take_offer.review;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.user.profile.UserProfile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
class MuSigTakeOfferReviewModel implements Model {
    enum TakeOfferStatus {
        NOT_STARTED,
        SENT,
        SUCCESS
    }

    @Setter
    private MuSigOffer muSigOffer;
    @Setter
    private MuSigTrade muSigTrade;
    @Setter
    private UserProfile peersUserProfile;
    @Setter
    private List<String> paymentMethodNames;
    @Setter
    private PaymentMethodSpec<?> takersPaymentMethodSpec;
    @Setter
    private Account<?, ?> takersAccount;
    @Setter
    private Monetary takersBaseSideAmount;
    @Setter
    private Monetary takersQuoteSideAmount;
    private final StringProperty priceWithCode = new SimpleStringProperty();

    public void setPriceWithCode(String value) {
        priceWithCode.set(value);
    }

    private final StringProperty priceDetails = new SimpleStringProperty();

    public void setPriceDetails(String value) {
        priceDetails.set(value);
    }

    @Setter
    private String price;
    @Setter
    private String priceCode;
    @Setter
    private String paymentMethodDisplayString;
    @Setter
    private String paymentMethodDetails;
    @Setter
    private double securityDepositAsPercent;
    @Setter
    private String formattedSecurityDepositAsPercent;
    @Setter
    private String securityDepositAsBtc;
    private final StringProperty fee = new SimpleStringProperty();

    public void setFee(String value) {
        fee.set(value);
    }

    private final StringProperty feeDetails = new SimpleStringProperty();

    public void setFeeDetails(String value) {
        feeDetails.set(value);
    }
    @Setter
    private long marketPrice;
    private final ObjectProperty<TakeOfferStatus> takeOfferStatus = new SimpleObjectProperty<>(TakeOfferStatus.NOT_STARTED);

    void reset() {
        muSigOffer = null;
        muSigTrade = null;
        peersUserProfile = null;
        paymentMethodNames = null;
        takersPaymentMethodSpec = null;
        takersAccount = null;
        takersBaseSideAmount = null;
        takersQuoteSideAmount = null;
        priceWithCode.set(null);
        priceDetails.set(null);
        price = null;
        priceCode = null;
        paymentMethodDisplayString = null;
        securityDepositAsPercent = 0;
        formattedSecurityDepositAsPercent = null;
        securityDepositAsBtc = null;
        fee.set(null);
        feeDetails.set(null);
        marketPrice = 0;
        // Back to the initial state, not null: the controller is cached across wizard sessions
        // and the confirmation gate admits a submission only while the status is NOT_STARTED.
        takeOfferStatus.set(TakeOfferStatus.NOT_STARTED);
    }
}
