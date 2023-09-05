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

package bisq.desktop.overlay.bisq_easy.create_offer.amount;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
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
public class CreateOfferAmountModel implements Model {
    private final BooleanProperty showRangeAmounts = new SimpleBooleanProperty();
    private final BooleanProperty isMinAmountEnabled = new SimpleBooleanProperty();
    private final StringProperty toggleButtonText = new SimpleStringProperty();
    private final ObjectProperty<AmountSpec> amountSpec = new SimpleObjectProperty<>();
    private final BooleanProperty areAmountsValid = new SimpleBooleanProperty(true);
    @Setter
    private Direction direction;
    @Setter
    private Market market = MarketRepository.getDefault();
    @Setter
    private List<FiatPaymentMethod> fiatPaymentMethods = new ArrayList<>();
    @Setter
    private PriceSpec priceSpec = new MarketPriceSpec();
    @Setter
    private String headline;
    @Setter
    private boolean isOpenedFromDashboard;
    @Setter
    private Optional<PriceQuote> bestOffersPrice = Optional.empty();

    public void reset() {
        showRangeAmounts.set(false);
        isMinAmountEnabled.set(false);
        toggleButtonText.set(null);
        amountSpec.set(null);
        direction = null;
        market = MarketRepository.getDefault();
        fiatPaymentMethods = new ArrayList<>();
        priceSpec = new MarketPriceSpec();
        areAmountsValid.set(true);
    }
}