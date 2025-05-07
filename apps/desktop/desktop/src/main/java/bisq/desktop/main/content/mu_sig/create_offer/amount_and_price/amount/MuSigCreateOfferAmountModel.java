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

package bisq.desktop.main.content.mu_sig.create_offer.amount_and_price.amount;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideAmountSpec;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class MuSigCreateOfferAmountModel implements Model {
    @Setter
    private Direction direction;
    @Setter
    private Market market = MarketRepository.getDefault();
    @Setter
    private List<FiatPaymentMethod> fiatPaymentMethods = new ArrayList<>();
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
    private final BooleanProperty isRangeAmountEnabled = new SimpleBooleanProperty();
    private final BooleanProperty isOverlayVisible = new SimpleBooleanProperty();
    private final StringProperty priceTooltip = new SimpleStringProperty();
    private final ObjectProperty<QuoteSideAmountSpec> quoteSideAmountSpec = new SimpleObjectProperty<>();
    private final ObjectProperty<PriceQuote> priceQuote = new SimpleObjectProperty<>();
    private final StringProperty errorMessage = new SimpleStringProperty();

    public void reset() {
        direction = null;
        market = MarketRepository.getDefault();
        fiatPaymentMethods = new ArrayList<>();
        amountLimitInfo.set(null);
        amountLimitInfoOverlayInfo.set(null);
        shouldShowAmountLimitInfo.set(false);
        shouldShowHowToBuildReputationButton.set(false);
        shouldShowWarningIcon.set(false);
        learnMoreVisible.set(false);
        amountLimitInfoLink = null;
        linkToWikiText = null;
        baseSideAmount = Optional.empty();
        isRangeAmountEnabled.set(false);
        isOverlayVisible.set(false);
        priceTooltip.set(null);
        quoteSideAmountSpec.set(null);
        priceQuote.set(null);
        errorMessage.set(null);
    }
}
