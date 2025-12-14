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

package bisq.desktop.main.content.dashboard.bisq_easy;

import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

public class BisqEasyDashboardColumns {
    private final Controller controller;

    public BisqEasyDashboardColumns(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    public GridPane getViewRoot() {
        return controller.getView().getRoot();
    }

    @Slf4j
    public static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private Pin isNotificationVisiblePin;
        private boolean allowUpdateOffersOnline;

        public Controller(ServiceProvider serviceProvider) {
            Model model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        private void onBuildReputation() {
            Navigation.navigateTo(NavigationTarget.BUILD_REPUTATION);
        }

        private void onOpenTradeOverview() {
            Navigation.navigateTo(NavigationTarget.TRADE_PROTOCOLS);
        }

        private void onOpenBisqEasy() {
            Navigation.navigateTo(NavigationTarget.BISQ_EASY);
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<GridPane, Model, Controller> {
        private static final int PADDING = 20;

        private final Button tradeProtocols, buildReputation;

        public View(Model model, Controller controller) {
            super(new GridPane(), model, controller);

            root.setHgap(PADDING);
            root.setVgap(PADDING);
            GridPaneUtil.setGridPaneTwoColumnsConstraints(root);

            VBox firstBox = getBigWidgetBox();
            VBox.setMargin(firstBox, new Insets(0, 0, 0, 0));
            VBox.setVgrow(firstBox, Priority.NEVER);
            root.add(firstBox, 0, 0, 2, 1);

            Insets gridPaneInsets = new Insets(0, 0, -7.5, 0);
            GridPane subGridPane = GridPaneUtil.getTwoColumnsGridPane(PADDING, 15, gridPaneInsets);

            root.add(subGridPane, 0, 1, 2, 1);

            String groupPaneStyleClass = "bisq-box-1";
            String headlineLabelStyleClass = "bisq-text-headline-2";
            String infoLabelStyleClass = "bisq-text-3";
            String buttonStyleClass = "large-button";
            Insets groupInsets = new Insets(36, 48, 44, 48);
            Insets headlineInsets = new Insets(36, 48, 0, 48);
            Insets infoInsets = new Insets(0, 48, 0, 48);
            Insets buttonInsets = new Insets(10, 48, 44, 48);

            tradeProtocols = new Button(Res.get("dashboard.second.button"));
            GridPaneUtil.fillColumn(subGridPane,
                    0,
                    tradeProtocols,
                    buttonStyleClass,
                    buttonInsets,
                    Res.get("dashboard.second.headline"),
                    headlineLabelStyleClass,
                    "trade-green",
                    16d,
                    headlineInsets,
                    Res.get("dashboard.second.content"),
                    infoLabelStyleClass,
                    infoInsets,
                    0d,
                    groupPaneStyleClass,
                    groupInsets);

            buildReputation = new Button(Res.get("dashboard.third.button"));
            GridPaneUtil.fillColumn(subGridPane,
                    1,
                    buildReputation,
                    buttonStyleClass,
                    buttonInsets,
                    Res.get("dashboard.third.headline"),
                    headlineLabelStyleClass,
                    "reputation-ribbon",
                    16d,
                    headlineInsets,
                    Res.get("dashboard.third.content"),
                    infoLabelStyleClass,
                    infoInsets,
                    0d,
                    groupPaneStyleClass,
                    groupInsets);
        }

        @Override
        protected void onViewAttached() {
            tradeProtocols.setOnAction(e -> controller.onOpenTradeOverview());
            buildReputation.setOnAction(e -> controller.onBuildReputation());
        }

        @Override
        protected void onViewDetached() {
            tradeProtocols.setOnAction(null);
            buildReputation.setOnAction(null);
        }

        private VBox getBigWidgetBox() {
            Label headlineLabel = GridPaneUtil.getHeadline(Res.get("dashboard.main.headline"),
                    "bisq-text-headline-4",
                    null,
                    0d);

            Button button = new Button(Res.get("dashboard.main.button"));
            button.setDefaultButton(true);
            button.getStyleClass().add("super-large-button");
            button.setOnAction(e -> controller.onOpenBisqEasy());
            button.setMaxWidth(Double.MAX_VALUE);

            VBox.setMargin(headlineLabel, new Insets(0, 0, 10, 0));
            VBox.setMargin(button, new Insets(20, 0, 0, 0));
            String iconTxtStyle = "bisq-easy-onboarding-big-box-bullet-point";
            VBox vBox = new VBox(15,
                    headlineLabel,
                    GridPaneUtil.getIconAndText(iconTxtStyle,
                            Res.get("dashboard.main.content1"),
                            "trading-in-circle-green"),
                    GridPaneUtil.getIconAndText(iconTxtStyle,
                            Res.get("dashboard.main.content2"),
                            "chat-in-circle-green"),
                    GridPaneUtil.getIconAndText(iconTxtStyle,
                            Res.get("dashboard.main.content3"),
                            "reputation-in-circle-green"),
                    button);
            vBox.getStyleClass().add("bisq-box-2");
            vBox.setPadding(new Insets(30, 48, 44, 48));
            return vBox;
        }
    }
}
