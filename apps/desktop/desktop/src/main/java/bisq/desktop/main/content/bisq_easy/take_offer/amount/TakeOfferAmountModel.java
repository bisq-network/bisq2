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

package bisq.desktop.main.content.bisq_easy.take_offer.amount;

import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import bisq.offer.bisq_easy.BisqEasyOffer;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.Setter;

@Getter
public class TakeOfferAmountModel implements Model {
    @Setter
    private BisqEasyOffer bisqEasyOffer;
    private final ObjectProperty<Monetary> takersQuoteSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> takersBaseSideAmount = new SimpleObjectProperty<>();
    private final StringProperty amountLimitInfo = new SimpleStringProperty();
    private final StringProperty amountLimitInfoAmount = new SimpleStringProperty();
    private final StringProperty amountLimitInfoOverlayInfo = new SimpleStringProperty();
    private final BooleanProperty isWarningIconVisible = new SimpleBooleanProperty();
    private final BooleanProperty isAmountHyperLinkDisabled = new SimpleBooleanProperty();
    private final BooleanProperty isAmountLimitInfoOverlayVisible = new SimpleBooleanProperty();
    private final BooleanProperty isAmountLimitInfoVisible = new SimpleBooleanProperty();
    @Setter
    private Monetary sellersReputationBasedQuoteSideAmount;
    @Setter
    private long sellersReputationScore;
    @Setter
    private String amountLimitInfoLink;
    @Setter
    private String linkToWikiText;
    @Setter
    private String headline;
}