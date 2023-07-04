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

package bisq.desktop.main.content.trade_apps.bisqEasy.chat.offer_details;

import bisq.desktop.common.view.Model;
import bisq.offer.bisq_easy.BisqEasyOffer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class BisqEasyOfferDetailsModel implements Model {
    @Setter
    private BisqEasyOffer bisqEasyOffer;
    private final StringProperty offerType = new SimpleStringProperty();
    private final StringProperty baseSideAmount = new SimpleStringProperty();
    private final StringProperty quoteSideAmount = new SimpleStringProperty();
    private final StringProperty quoteSideAmountDescription = new SimpleStringProperty("");
    private final StringProperty price = new SimpleStringProperty();
    private final StringProperty priceDescription = new SimpleStringProperty();
    private final StringProperty paymentMethods = new SimpleStringProperty();

    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty date = new SimpleStringProperty();
    private final StringProperty makersTradeTerms = new SimpleStringProperty();
    private final StringProperty requiredTotalReputationScore = new SimpleStringProperty();
    private final BooleanProperty makersTradeTermsVisible = new SimpleBooleanProperty();
    private final BooleanProperty requiredTotalReputationScoreVisible = new SimpleBooleanProperty();
}
