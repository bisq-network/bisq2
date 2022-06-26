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

package bisq.desktop.primary.main.content.settings.reputation.earnReputation;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.Styles;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.desktop.primary.PrimaryStageModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EarnReputationView extends TabView<EarnReputationModel, EarnReputationController> {

    public EarnReputationView(EarnReputationModel model, EarnReputationController controller) {
        super(model, controller);

        root.setMinWidth(PrimaryStageModel.MIN_WIDTH - 20);
        root.setMaxWidth(PrimaryStageModel.MIN_WIDTH - 20);
        root.setMinHeight(PrimaryStageModel.MIN_HEIGHT- 20);
        root.setMaxHeight(PrimaryStageModel.MIN_HEIGHT- 20);
        vBox.setPrefHeight(PrimaryStageModel.MIN_HEIGHT- 20);

        root.setPadding(new Insets(40, 68, 40, 68));
        root.getStyleClass().add("popup-bg");

        Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-white", "bisq-text-grey-9");
        addTab(Res.get("reputation.source.BURNED_BSQ"),
                NavigationTarget.BURN_BSQ,
                styles);
        addTab(Res.get("reputation.source.BSQ_BOND"),
                NavigationTarget.BSQ_BOND,
                styles);

        headlineLabel.setText(Res.get("reputation.earnReputation"));

        // Make tabs left aligned and headline on top
        tabs.getChildren().remove(0, 2); // remove headline and spacer

        headlineLabel.getStyleClass().remove("bisq-content-headline-label");
        headlineLabel.getStyleClass().add("bisq-popup-headline-label");

        ImageView icon = ImageUtil.getImageViewById("onboarding-1-reputation");
        HBox.setMargin(icon, new Insets(0, 0, 0, 0));
        HBox.setMargin(headlineLabel, new Insets(2, 0, 0, 2));
        HBox hBox = new HBox(7, icon, headlineLabel);
        VBox.setMargin(hBox, new Insets(0, 0, 32, 0));
        vBox.getChildren().add(0, hBox);

        line.getStyleClass().remove("bisq-darkest-bg");
        line.getStyleClass().add("bisq-mid-grey");
        selectionMarker.getStyleClass().remove("bisq-green-line");
        selectionMarker.getStyleClass().add("bisq-white-bg");

        StackPane.setMargin(lineAndMarker, new Insets(100, 110, 0, 0));
    }

    @Override
    protected void onViewAttached() {
        line.prefWidthProperty().unbind();
        line.prefWidthProperty().bind(root.widthProperty().subtract(136));
    }

    @Override
    protected void onViewDetached() {
    }
}
