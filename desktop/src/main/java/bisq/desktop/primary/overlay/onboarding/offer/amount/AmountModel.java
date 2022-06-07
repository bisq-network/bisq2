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

package bisq.desktop.primary.overlay.onboarding.offer.amount;

import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.desktop.common.view.Model;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.model.StyleSpans;

import java.util.Collection;

@Slf4j
@Getter
public class AmountModel implements Model {
    static final int MAX_INPUT_TERMS = 500;
    private final ObservableList<String> customTags = FXCollections.observableArrayList();
    @Setter
    private Market selectedMarket;
    @Setter
    private Monetary baseSideAmount;
    @Setter
    private Monetary quoteSideAmount;
    @Setter
    private Quote fixPrice;

    private final ObservableList<String> selectedPaymentMethods = FXCollections.observableArrayList();

    private final BooleanProperty createOfferButtonVisibleProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty isInvalidTradeIntent = new SimpleBooleanProperty();
    private final StringProperty offerPreview = new SimpleStringProperty();
    private final StringProperty terms = new SimpleStringProperty();
    private final BooleanProperty termsEditable = new SimpleBooleanProperty(true);
    private final String userName;
    ObjectProperty<StyleSpans<Collection<String>>> styleSpans = new SimpleObjectProperty<>();

    public AmountModel(String userName) {
        this.userName = userName;
    }
}
