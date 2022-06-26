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

package bisq.desktop.primary.main.content.settings.reputation.burn;

import bisq.desktop.common.utils.Styles;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.primary.PrimaryStageModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BurnBsqView extends TabView<BurnBsqModel, BurnBsqController> {

    private final Button closeButton;

    public BurnBsqView(BurnBsqModel model, BurnBsqController controller) {
        super(model, controller);

        double width = PrimaryStageModel.MIN_WIDTH - 20;
        root.setMinWidth(width);
        root.setMaxWidth(width);
        double height = PrimaryStageModel.MIN_HEIGHT - 40;
        root.setMinHeight(height);
        root.setMaxHeight(height);
        vBox.setPrefHeight(height);

        root.setPadding(new Insets(40, 68, 40, 68));
        root.getStyleClass().add("popup-bg");

        Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-logo-green", "bisq-text-grey-9");
        //Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-white", "bisq-text-grey-9");
        addTab(Res.get("reputation.burnedBsq.tab1"),
                NavigationTarget.BURN_BSQ_TAB_1,
                styles);
        addTab(Res.get("reputation.burnedBsq.tab2"),
                NavigationTarget.BURN_BSQ_TAB_2,
                styles);
        addTab(Res.get("reputation.burnedBsq.tab3"),
                NavigationTarget.BURN_BSQ_TAB_3,
                styles);

        closeButton = BisqIconButton.createIconButton("close");


        headLine.setText(Res.get("reputation.burnBsq"));

        // Make tabs left aligned and headline on top
        tabs.getChildren().remove(0, 2); // remove headline and spacer

        headLine.getStyleClass().remove("bisq-content-headline-label");
        headLine.getStyleClass().add("bisq-text-15");

        HBox.setMargin(closeButton, new Insets(-1, -15, 0, 0));
        HBox hBox = new HBox(7, headLine, Spacer.fillHBox(), closeButton);

        VBox.setMargin(hBox, new Insets(0, 0, 32, 0));
        vBox.getChildren().add(0, hBox);

        line.getStyleClass().remove("bisq-darkest-bg");
        line.getStyleClass().add("bisq-mid-grey");
       /* selectionMarker.getStyleClass().remove("bisq-green-line");
        selectionMarker.getStyleClass().add("bisq-white-bg");*/

        StackPane.setMargin(lineAndMarker, new Insets(100, 110, 0, 0));
    }

    @Override
    protected void onViewAttached() {
        line.prefWidthProperty().unbind();
        line.prefWidthProperty().bind(root.widthProperty().subtract(136));
        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
    }
}
