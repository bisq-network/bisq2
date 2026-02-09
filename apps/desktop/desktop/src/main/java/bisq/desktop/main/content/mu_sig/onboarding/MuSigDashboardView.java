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

package bisq.desktop.main.content.mu_sig.onboarding;

import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigDashboardView extends View<VBox, MuSigDashboardModel, MuSigDashboardController> {
    private static final int PADDING = 20;
    private final Button tradeGuide, amountLimitsButton;

    public MuSigDashboardView(MuSigDashboardModel model,
                              MuSigDashboardController controller,
                              HBox muSigDashboardTopPanel) {
        super(new VBox(20), model, controller);

        root.setPadding(new Insets(0, 40, 40, 40));
        root.setMinWidth(780);

        int rowIndex = 0;
        GridPane gridPane = GridPaneUtil.getTwoColumnsGridPane(48, 15, new Insets(30, 48, 44, 48));
        gridPane.getStyleClass().add("bisq-easy-onboarding-big-box");
        root.getChildren().add(gridPane);


        Label securityHeadline = GridPaneUtil.getHeadline(Res.get("muSig.dashboard.security.headline"),
                "bisq-easy-onboarding-big-box-headline",
                null,
                0d);
        gridPane.add(securityHeadline, 0, rowIndex, 2, 1);

        String lineStyleClass = "bisq-easy-onboarding-big-box-bullet-point";
        VBox vBox = new VBox(15, Spacer.fillVBox(),
                GridPaneUtil.getIconAndText(lineStyleClass,
                        Res.get("muSig.dashboard.security.content1"),
                        "multi-sig"),
                GridPaneUtil.getIconAndText(lineStyleClass,
                        Res.get("muSig.dashboard.security.content2"),
                        "trading-in-circle-green"),
                GridPaneUtil.getIconAndText(lineStyleClass,
                        Res.get("muSig.dashboard.security.content3"),
                        "reputation-in-circle-green"),
                Spacer.fillVBox());
        gridPane.add(vBox, 0, ++rowIndex);

        tradeGuide = new Button(Res.get("muSig.dashboard.tradeGuide.button"));
        tradeGuide.getStyleClass().add("large-button");
        tradeGuide.setMaxWidth(Double.MAX_VALUE);
        tradeGuide.setDefaultButton(true);

        GridPane.setMargin(tradeGuide, new Insets(10, 0, 0, 0));
        gridPane.add(tradeGuide, 0, ++rowIndex);

        amountLimitsButton = new Button(Res.get("muSig.dashboard.amountLimits.button"));
        amountLimitsButton.getStyleClass().add("large-button");
        amountLimitsButton.setMaxWidth(Double.MAX_VALUE);

        GridPane.setMargin(amountLimitsButton, new Insets(10, 0, 0, 0));
        gridPane.add(amountLimitsButton, 1, rowIndex);
    }

    @Override
    protected void onViewAttached() {
        tradeGuide.setOnAction(e -> controller.onOpenTradeGuide());
        amountLimitsButton.setOnAction(e -> controller.onExploreAmountLimit());
    }

    @Override
    protected void onViewDetached() {
        tradeGuide.setOnAction(null);
        amountLimitsButton.setOnAction(null);
    }
}
