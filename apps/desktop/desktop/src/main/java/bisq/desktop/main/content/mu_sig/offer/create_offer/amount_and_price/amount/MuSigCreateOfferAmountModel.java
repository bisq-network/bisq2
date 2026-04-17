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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount;

import bisq.account.payment_method.PaymentMethod;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.common.view.Model;
import bisq.offer.price.spec.PriceSpec;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
public class MuSigCreateOfferAmountModel implements Model {
    private final BooleanProperty useRangeAmount = new SimpleBooleanProperty();

    private final ObjectProperty<PriceSpec> priceSpec = new SimpleObjectProperty<>();
    private final ObservableList<PaymentMethod<?>> paymentMethods = FXCollections.observableArrayList();

    private final StringProperty amountLimitInfo = new SimpleStringProperty();
    private final StringProperty amountLimitInfoOverlayInfo = new SimpleStringProperty();
    private final BooleanProperty shouldShowAmountLimitInfo = new SimpleBooleanProperty();
    private final BooleanProperty shouldShowHowToBuildReputationButton = new SimpleBooleanProperty();
    private final BooleanProperty shouldShowWarningIcon = new SimpleBooleanProperty();
    private final BooleanProperty learnMoreVisible = new SimpleBooleanProperty();
    @Setter
    private String amountLimitInfoLink;
    @Setter
    private String linkToWikiText;
    @Setter
    private Optional<Monetary> baseSideAmount = Optional.empty();
    @Setter
    private Monetary reputationBasedMaxAmount;
    @Setter
    private long myReputationScore;
    private final BooleanProperty isOverlayVisible = new SimpleBooleanProperty();
    private final StringProperty priceTooltip = new SimpleStringProperty();
    private final ObjectProperty<PriceQuote> priceQuote = new SimpleObjectProperty<>();
    private final StringProperty errorMessage = new SimpleStringProperty();

    public void reset() {
        priceSpec.set(null);
        paymentMethods.clear();
        amountLimitInfo.set(null);
        amountLimitInfoOverlayInfo.set(null);
        shouldShowAmountLimitInfo.set(false);
        shouldShowHowToBuildReputationButton.set(false);
        shouldShowWarningIcon.set(false);
        learnMoreVisible.set(false);
        amountLimitInfoLink = null;
        linkToWikiText = null;
        baseSideAmount = Optional.empty();
        isOverlayVisible.set(false);
        priceTooltip.set(null);
        priceQuote.set(null);
        errorMessage.set(null);
    }
}
