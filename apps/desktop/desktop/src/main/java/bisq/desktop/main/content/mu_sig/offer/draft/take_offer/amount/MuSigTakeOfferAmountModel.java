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

package bisq.desktop.main.content.mu_sig.offer.draft.take_offer.amount;

import bisq.desktop.common.view.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
public class MuSigTakeOfferAmountModel implements Model {
    @Setter
    private String headline;
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
    private final BooleanProperty isOverlayVisible = new SimpleBooleanProperty();
    private final StringProperty priceTooltip = new SimpleStringProperty();
    private final StringProperty errorMessage = new SimpleStringProperty();

    void reset() {
        headline = null;
        amountLimitInfo.set(null);
        amountLimitInfoOverlayInfo.set(null);
        shouldShowAmountLimitInfo.set(false);
        shouldShowHowToBuildReputationButton.set(false);
        shouldShowWarningIcon.set(false);
        learnMoreVisible.set(false);
        amountLimitInfoLink = null;
        linkToWikiText = null;
        isOverlayVisible.set(false);
        priceTooltip.set(null);
        errorMessage.set(null);
    }
}
