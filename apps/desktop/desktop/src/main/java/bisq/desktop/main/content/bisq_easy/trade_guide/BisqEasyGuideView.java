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

package bisq.desktop.main.content.bisq_easy.trade_guide;

import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.common.Styles;
import bisq.desktop.common.view.TabView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyGuideView extends TabView<BisqEasyGuideModel, BisqEasyGuideController> {
    private Button closeIconButton;

    public BisqEasyGuideView(BisqEasyGuideModel model, BisqEasyGuideController controller) {
        super(model, controller);

        root.setPadding(new Insets(15, 30, 30, 30));
        VBox.setMargin(scrollPane, new Insets(10, 0, 0, 0));

        Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-green", "bisq-text-grey-9");
        addTab(Res.get("bisqEasy.tradeGuide.welcome"),
                NavigationTarget.BISQ_EASY_GUIDE_WELCOME,
                styles);
        addTab(Res.get("bisqEasy.tradeGuide.security"),
                NavigationTarget.BISQ_EASY_GUIDE_SECURITY,
                styles);
        addTab(Res.get("bisqEasy.tradeGuide.process"),
                NavigationTarget.BISQ_EASY_GUIDE_PROCESS,
                styles);
        addTab(Res.get("bisqEasy.tradeGuide.rules"),
                NavigationTarget.BISQ_EASY_GUIDE_RULES,
                styles);
    }

    @Override
    protected void onViewAttached() {
        line.prefWidthProperty().unbind();
        double paddings = root.getPadding().getLeft() + root.getPadding().getRight();
        line.prefWidthProperty().bind(root.widthProperty().subtract(paddings));

        closeIconButton.setOnAction(e -> controller.onClose());

        root.setPrefWidth(OverlayModel.WIDTH);
        double height = model.isTradeRulesConfirmed()
                ? OverlayModel.HEIGHT + 10
                : OverlayModel.HEIGHT + 60;
        root.setPrefHeight(height);
    }

    @Override
    protected void onViewDetached() {
        line.prefWidthProperty().unbind();

        closeIconButton.setOnAction(null);
    }

    @Override
    protected void setupTopBox() {
        headline = new Label();
        headline.setText(Res.get("bisqEasy.tradeGuide.tabs.headline"));
        headline.getStyleClass().add("tab-view");

        closeIconButton = BisqIconButton.createIconButton("close");

        HBox.setMargin(closeIconButton, new Insets(-1, -15, 0, 0));
        HBox.setMargin(headline, new Insets(0, 0, 0, -2));
        HBox hBox = new HBox(headline, Spacer.fillHBox(), closeIconButton);

        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        tabs.setMinHeight(35);

        topBox = new VBox();
        VBox.setMargin(hBox, new Insets(0, 0, 17, 0));
        topBox.getChildren().addAll(hBox, tabs);
    }
}
