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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components;

import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.View;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

import static bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.MuSigAmountLayoutConstants.WIDTH;

@Slf4j
public class MuSigAmountComponentsView extends View<VBox, MuSigAmountComponentsModel, MuSigAmountComponentsController> {
    private static final int HEIGHT = 127;

    private final Label description;
    private final VBox fixAmount;
    private final VBox rangeAmount;
    private final Region selectionLine;
    private final Set<Subscription> subscriptions = new HashSet<>();

    public MuSigAmountComponentsView(MuSigAmountComponentsModel model,
                                     MuSigAmountComponentsController controller,
                                     VBox fixAmount,
                                     VBox rangeAmount) {
        super(new VBox(10), model, controller);
        this.fixAmount = fixAmount;
        this.rangeAmount = rangeAmount;

        root.setAlignment(Pos.TOP_CENTER);

        description = new Label();
        description.setMouseTransparent(true);
        description.setPadding(new Insets(8, 0, 0, 20));

        Region greyLine = new Region();
        greyLine.setStyle("-fx-background-color: -bisq-mid-grey-20");
        greyLine.setPrefHeight(1);
        greyLine.setPrefWidth(WIDTH);
        greyLine.setLayoutY(HEIGHT - 1);
        greyLine.setMouseTransparent(true);

        selectionLine = new Region();
        selectionLine.getStyleClass().add("material-text-field-selection-line");
        selectionLine.setPrefHeight(2);
        selectionLine.setPrefWidth(0);
        selectionLine.setLayoutY(HEIGHT - 2);
        selectionLine.setMouseTransparent(true);

        Pane inputAndDisplayWithLine = new Pane(description, fixAmount, rangeAmount, greyLine, selectionLine);
        inputAndDisplayWithLine.setMinWidth(WIDTH);
        inputAndDisplayWithLine.setMaxWidth(WIDTH);
        inputAndDisplayWithLine.setMinHeight(HEIGHT);
        inputAndDisplayWithLine.setMaxHeight(HEIGHT);
        inputAndDisplayWithLine.getStyleClass().add("amount-components");

        fixAmount.setLayoutY(27);
        rangeAmount.setLayoutY(27);

        root.getChildren().addAll(inputAndDisplayWithLine);
    }

    @Override
    protected void onViewAttached() {
        description.textProperty().bind(model.getDescription());

        fixAmount.visibleProperty().bind(model.getUseRangeAmount().not());
        fixAmount.managedProperty().bind(model.getUseRangeAmount().not());
        rangeAmount.visibleProperty().bind(model.getUseRangeAmount());
        rangeAmount.managedProperty().bind(model.getUseRangeAmount());

        subscriptions.add(EasyBind.subscribe(model.getIsTextInputFocused(),
                isFocused -> {
                    description.getStyleClass().clear();
                    if (isFocused) {
                        selectionLine.setPrefWidth(0);
                        selectionLine.setOpacity(1);
                        Transitions.animatePrefWidth(selectionLine, WIDTH);
                        description.getStyleClass().add("description-focused");
                    } else {
                        Transitions.fadeOut(selectionLine, 200);
                        description.getStyleClass().add("description");
                    }
                }));
    }

    @Override
    protected void onViewDetached() {
        description.textProperty().unbind();

        fixAmount.visibleProperty().unbind();
        fixAmount.managedProperty().unbind();
        rangeAmount.visibleProperty().unbind();
        rangeAmount.managedProperty().unbind();

        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }
}
