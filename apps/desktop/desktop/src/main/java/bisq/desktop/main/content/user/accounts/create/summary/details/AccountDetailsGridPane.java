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

package bisq.desktop.main.content.user.accounts.create.summary.details;

import bisq.desktop.common.utils.GridPaneUtil;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

public abstract class AccountDetailsGridPane extends GridPane {
    protected static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
    protected static final String VALUE_STYLE = "trade-wizard-review-value";
    protected static final String DETAILS_STYLE = "trade-wizard-review-details";

    int rowIndex = 0;
    public AccountDetailsGridPane() {
        super(10, 10);

        GridPaneUtil.setGridPaneMultiColumnsConstraints(this, 3);
    }

    protected Label addDescriptionAndValue(String description, String value, int rowIndex) {
        addDescriptionLabel(description, rowIndex);
        return addValueLabel(value, rowIndex);
    }

    protected Label addDescriptionLabel(String description, int rowIndex) {
        Label label = new Label(description);
        label.getStyleClass().add(DESCRIPTION_STYLE);
        add(label, 0, rowIndex);
        return label;
    }

    protected Label addValueLabel(String value, int rowIndex) {
        Label label = new Label(value);
        label.getStyleClass().add(VALUE_STYLE);
        add(label, 1, rowIndex, 2, 1);
        return label;
    }

    protected Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: -bisq-border-color-grey");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }
}