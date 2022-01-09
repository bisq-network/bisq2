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

package bisq.desktop.primary.main.content.createoffer.assetswap.review;

import bisq.common.data.Pair;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.controls.BisqTextField;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ReviewOfferView extends View<VBox, ReviewOfferModel, ReviewOfferController> {
    private GridPane gridPane;
    private TextField askTextField;
    private BisqButton publishButton;

    public ReviewOfferView(ReviewOfferModel model, ReviewOfferController controller) {
        super(new VBox(), model, controller);
    }

    protected void initialize() {
        root.setSpacing(20);
        Label header = new BisqLabel("Review offer");
        gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        int rowIndex = 0;
        askTextField = addRow(rowIndex, "I want (ask):", "").second();

        publishButton = new BisqButton("Publish");
        root.getChildren().addAll(header, gridPane, publishButton);
    }

    private Pair<Label, TextField> addRow(int rowIndex, String key, String value) {
        Label keyLabel = new BisqLabel(key);
        GridPane.setRowIndex(keyLabel, rowIndex);

        TextField valueTextField = new BisqTextField(value);
        GridPane.setRowIndex(valueTextField, rowIndex);
        GridPane.setColumnIndex(valueTextField, 1);

        gridPane.getChildren().addAll(keyLabel, valueTextField);
        return new Pair<>(keyLabel, valueTextField);
    }

    @Override
    protected void onViewAttached() {
        askTextField.textProperty().bindBidirectional(model.formattedAskAmount);
   
    /*    ChangeListener<String> listener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                controller.setAskValue(newValue);
            }
        };
        askTextField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!oldValue && newValue) {
                    askTextField.textProperty().unbind();
                    askTextField.textProperty().addListener(listener);

                } else {
                    askTextField.textProperty().bind(model.formattedAskAmount);
                    askTextField.textProperty().removeListener(listener);
                }
            }
        });
*/

        publishButton.setOnAction(e -> controller.onPublish());
    }

    protected void onViewDetached() {
    }
}
