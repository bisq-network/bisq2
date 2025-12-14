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

package bisq.desktop.main.content.dashboard.musig;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Icons;
import bisq.desktop.common.Layout;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

public class MusigDashboardColumns {
    private final Controller controller;

    public MusigDashboardColumns(ServiceProvider serviceProvider) {
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
            ProtocolListItem musig = new ProtocolListItem(TradeAppsAttributes.Type.MU_SIG,
                    NavigationTarget.MU_SIG,
                    TradeProtocolType.MU_SIG,
                    "10 000 USD"
            );
            ProtocolListItem bisqEasy = new ProtocolListItem(TradeAppsAttributes.Type.BISQ_EASY,
                    NavigationTarget.BISQ_EASY,
                    TradeProtocolType.BISQ_EASY,
                    "600 USD"
            );
            Model model = new Model(musig, bisqEasy);
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

        void onSelect(ProtocolListItem protocolListItem) {
            Navigation.navigateTo(protocolListItem.getNavigationTarget());
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ProtocolListItem musig;
        private final ProtocolListItem bisqEasy;

        public Model(ProtocolListItem musig, ProtocolListItem bisqEasy) {
            this.musig = musig;
            this.bisqEasy = bisqEasy;
        }
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<GridPane, Model, Controller> {
        private static final int HORIZONTAL_MARGIN = 30;
        private static final int VERTICAL_GAP = 25;
        private final Button musigButton, bisqEasyButton;

        public View(Model model, Controller controller) {
            super(new GridPane(), model, controller);

            root.setVgap(VERTICAL_GAP);
            root.getStyleClass().add("bisq-common-bg");

            GridPaneUtil.setGridPaneTwoColumnsConstraints(root);
            int minWidth = 450;
            root.getColumnConstraints().get(0).setMinWidth(minWidth);
            root.getColumnConstraints().get(1).setMinWidth(minWidth);

            Label headline = new Label(Res.get("dashboard.protocols.headline"));
            headline.getStyleClass().add("trade-protocols-overview-headline");
            GridPane.setMargin(headline, new Insets(-20, 0, 0, 0));
            root.add(headline, 0, 0, 2, 1);

            WrappingText subHeadline = new WrappingText(Res.get("dashboard.protocols.subHeadline"), "trade-protocols-overview-sub-headline");
            GridPane.setMargin(subHeadline, new Insets(-20, 0, 10, 0));
            root.add(subHeadline, 0, 1, 2, 1);

            GridPane protocolsPane = GridPaneUtil.getGridPane(25, 25, new Insets(0, 0, 0, 0));
            GridPaneUtil.setGridPaneMultiColumnsConstraints(protocolsPane, 6);

            musigButton = new Button(Res.get("dashboard.protocols.musig.button"));
            addProtocolBox(protocolsPane, model.getMusig(), musigButton, 0);

            bisqEasyButton = new Button(Res.get("dashboard.protocols.bisqEasy.button"));
            addProtocolBox(protocolsPane, model.getBisqEasy(), bisqEasyButton, 3);

            root.add(protocolsPane, 0, 2, 2, 1);
        }

        @Override
        protected void onViewAttached() {
            musigButton.setOnAction(e -> controller.onSelect(model.getMusig()));
            bisqEasyButton.setOnAction(e -> controller.onSelect(model.getBisqEasy()));
        }

        @Override
        protected void onViewDetached() {
            musigButton.setOnAction(null);
            bisqEasyButton.setOnAction(null);
        }

        private void addProtocolBox(GridPane gridPane,
                                    ProtocolListItem protocolListItem,
                                    Button button,
                                    int columnIndex) {
            int rowIndex = 0;

            Pane group = new Pane();
            group.getStyleClass().add("bisq-card-bg");

            group.setCursor(Cursor.HAND);
            group.setOnMouseClicked(e -> controller.onSelect(protocolListItem));
            gridPane.add(group, columnIndex, rowIndex, 3, 6);

            Label headlineLabel = new Label(protocolListItem.getProtocolsName());
            headlineLabel.setAlignment(Pos.TOP_LEFT);
            headlineLabel.getStyleClass().add("bisq-text-headline-6");
            headlineLabel.setGraphic(ImageUtil.getImageViewById(protocolListItem.getIconId()));
            headlineLabel.setGraphicTextGap(10);
            GridPane.setMargin(headlineLabel, new Insets(20, 30, 0, 30));
            gridPane.add(headlineLabel, columnIndex, rowIndex, 2, 1);

            WrappingText subTitleLabel = new WrappingText(protocolListItem.getBasicInfo(), "bisq-text-3");
            subTitleLabel.setLineSpacing(1d);
            GridPane.setMargin(subTitleLabel, new Insets(-17, HORIZONTAL_MARGIN, 0, HORIZONTAL_MARGIN));
            gridPane.add(subTitleLabel, columnIndex, ++rowIndex, 3, 1);

            Region separator = Layout.hLine();
            GridPane.setMargin(separator, new Insets(-14, HORIZONTAL_MARGIN, 0, HORIZONTAL_MARGIN));
            gridPane.add(separator, columnIndex, ++rowIndex, 3, 1);

            VBox markets = getParameterPane(Res.get("tradeApps.markets"), protocolListItem.getMarkets());
            GridPane.setMargin(markets, new Insets(-4, 0, 0, HORIZONTAL_MARGIN));
            gridPane.add(markets, columnIndex, ++rowIndex);

            VBox maxTradeLimit = getParameterPane(Res.get("tradeApps.maxTradeLimit"), protocolListItem.getMaxTradeLimit());
            GridPane.setMargin(maxTradeLimit, new Insets(-4, HORIZONTAL_MARGIN, 0, 6));
            gridPane.add(maxTradeLimit, columnIndex + 1, rowIndex);

            VBox security = getParameterPane(Res.get("tradeApps.security"),
                    protocolListItem.getTradeAppsAttributesType().getSecurity().ordinal(),
                    protocolListItem.getSecurityInfo());
            gridPane.add(security, columnIndex, ++rowIndex);
            GridPane.setMargin(security, new Insets(-13, HORIZONTAL_MARGIN, 0, HORIZONTAL_MARGIN));

            VBox privacy = getParameterPane(Res.get("tradeApps.privacy"),
                    protocolListItem.getTradeAppsAttributesType().getPrivacy().ordinal(),
                    protocolListItem.getPrivacyInfo());
            gridPane.add(privacy, columnIndex + 1, rowIndex);
            GridPane.setMargin(privacy, new Insets(-13, HORIZONTAL_MARGIN, 0, 6));

            VBox easeOfUse = getParameterPane(Res.get("tradeApps.easeOfUse"),
                    protocolListItem.getTradeAppsAttributesType().getConvenience().ordinal(),
                    protocolListItem.getConvenienceInfo());
            gridPane.add(easeOfUse, columnIndex + 2, rowIndex);
            GridPane.setMargin(easeOfUse, new Insets(-13, 0, 0, -19));

            button.setDefaultButton(true);
            button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add("large-button");

            GridPane.setMargin(button, new Insets(7, HORIZONTAL_MARGIN, 30, HORIZONTAL_MARGIN));
            gridPane.add(button, columnIndex, ++rowIndex, 3, 1);
        }

        private VBox getParameterPane(String title, String info) {
            Label infoLabel = new Label(info);
            infoLabel.getStyleClass().add("bisq-text-1");
            return getParameterPane(title, infoLabel);
        }

        private VBox getParameterPane(String title, int value, String tooltipText) {
            return getParameterPane(title, getStarsNode(value, tooltipText));
        }

        private VBox getParameterPane(String title, Node node) {
            VBox box = new VBox();
            GridPane.setHgrow(box, Priority.ALWAYS);
            Label titleLabel = new Label(title.toUpperCase());
            titleLabel.getStyleClass().add("bisq-text-4");
            box.getChildren().addAll(titleLabel, node);
            return box;
        }

        private Node getStarsNode(int value, String tooltipText) {
            HBox hBox = new HBox();
            hBox.setPadding(new Insets(5, 0, 5, 0));
            hBox.setSpacing(5);

            for (int i = 0; i < 3; i++) {
                Label label = Icons.getIcon(AwesomeIcon.STAR, "1.2em");
                label.setMouseTransparent(false);
                label.setOpacity(i <= value ? 1 : 0.2);
                hBox.getChildren().add(label);
            }

            Tooltip tooltip = new BisqTooltip();
            tooltip.setMaxWidth(240);
            tooltip.setWrapText(true);
            tooltip.setText(tooltipText);
            Tooltip.install(hBox, tooltip);

            return hBox;
        }
    }
}
