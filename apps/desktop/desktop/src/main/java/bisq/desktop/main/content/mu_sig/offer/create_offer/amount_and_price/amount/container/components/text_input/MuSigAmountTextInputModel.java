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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.container.components.text_input;

import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TextFormatter;
import lombok.Getter;

@Getter
public class MuSigAmountTextInputModel implements Model {
    private final TextFormatter<Monetary> textFormatter;
    private final boolean isFixedAmount;
    private final boolean isLeftSideRangeAmount;

    private final ObjectProperty<Monetary> amount = new SimpleObjectProperty<>();
    private final StringProperty code = new SimpleStringProperty();
    private final BooleanProperty focusedProperty = new SimpleBooleanProperty();
    private final StringProperty inputText = new SimpleStringProperty();
    private final IntegerProperty sumOfNumChars = new SimpleIntegerProperty();
    private final DoubleProperty amountFieldWidth = new SimpleDoubleProperty();
    private final DoubleProperty dashFieldWidth = new SimpleDoubleProperty();

    MuSigAmountTextInputModel(TextFormatter<Monetary> textFormatter, boolean isFixedAmount, boolean isLeftSideRangeAmount) {
        this.textFormatter = textFormatter;
        this.isFixedAmount = isFixedAmount;
        this.isLeftSideRangeAmount = isLeftSideRangeAmount;
    }
}
