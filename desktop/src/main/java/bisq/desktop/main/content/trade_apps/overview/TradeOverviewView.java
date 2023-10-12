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

package bisq.desktop.main.content.trade_apps.overview;

import bisq.desktop.common.Icons;
import bisq.desktop.common.Layout;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.HPos;
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
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeOverviewView extends View<GridPane, TradeOverviewModel, TradeOverviewController> {
    private static final int VERTICAL_MARGIN = 30;
    private static final int VERTICAL_GAP = 25;
    private static final int HORIZONTAL_GAP = 25;

    public TradeOverviewView(TradeOverviewModel model, TradeOverviewController controller) {
        super(new GridPane(), model, controller);

        root.setHgap(HORIZONTAL_GAP);
        root.setVgap(VERTICAL_GAP);
        root.setPadding(new Insets(20, 40, 40, 40));

        GridPaneUtil.setGridPaneTwoColumnsConstraints(root);
        root.getColumnConstraints().get(0).setMinWidth(450);
        root.getColumnConstraints().get(1).setMinWidth(450);

        Label headline = new Label(Res.get("tradeApps.overview.headline"));
        headline.setWrapText(true);
        headline.getStyleClass().add("trade-protocols-overview-headline");
        headline.setMinHeight(55);
        root.add(headline, 0, 0, 2, 1);

        Label subHeadline = new Label(Res.get("tradeApps.overview.subHeadline"));
        subHeadline.setWrapText(true);
        subHeadline.getStyleClass().add("trade-protocols-overview-sub-headline");
        GridPane.setMargin(subHeadline, new Insets(-10, 0, 0, 0));
        root.add(subHeadline, 0, 1, 2, 1);

        Insets protocolsInset = new Insets(0, 0, 0, 0);
        GridPane mainProtocolsPane = GridPaneUtil.getGridPane(25, 25, protocolsInset);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(mainProtocolsPane, 6);
        root.add(mainProtocolsPane, 0, 2, 2, 1);

        int rowCount = mainProtocolsPane.getRowCount();
        int index = 0;
        for (ProtocolListItem protocol : model.getMainProtocols()) {
            int columnIndex = index % 2;
            rowCount = mainProtocolsPane.getRowCount();
            if (columnIndex == 1) {
                //Right box, go back to last line on the row count.
                rowCount = mainProtocolsPane.getRowCount() - 6;
            }
            getProtocolBox(mainProtocolsPane, protocol, columnIndex * 3, rowCount);
            index++;
        }

        Label more = new Label(Res.get("tradeApps.overview.more"));
        more.setWrapText(true);
        more.getStyleClass().add("trade-protocols-overview-sub-headline");
        GridPane.setMargin(more, new Insets(20, 0, 10, 0));
        root.add(more, 0, root.getRowCount(), 2, 1);

        GridPane moreProtocolsPane = GridPaneUtil.getGridPane(25, 25, protocolsInset);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(moreProtocolsPane, 6);
        root.add(moreProtocolsPane, 0, root.getRowCount(), 2, 1);

        rowCount = moreProtocolsPane.getRowCount();
        index = 0;
        for (ProtocolListItem protocol : model.getMoreProtocols()) {
            int columnIndex = index % 2;
            rowCount = moreProtocolsPane.getRowCount();
            if (columnIndex == 1) {
                //Right box, go back to last line on the row count.
                rowCount = moreProtocolsPane.getRowCount() - 6;
            }
            getProtocolBox(moreProtocolsPane, protocol, columnIndex * 3, rowCount);
            index++;
        }
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    private void getProtocolBox(GridPane gridPane,
                                ProtocolListItem protocolListItem,
                                int columnIndex,
                                int rowCount) {
        int rowIndex = rowCount;

        Pane group = new Pane();
        group.getStyleClass().add("bisq-box-2");
        group.setCursor(Cursor.HAND);
        group.setOnMouseClicked(e -> controller.onSelect(protocolListItem));
        gridPane.add(group, columnIndex, rowIndex, 3, 6);

        Label headlineLabel = new Label(protocolListItem.getProtocolsName());
        headlineLabel.setAlignment(Pos.TOP_LEFT);
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        headlineLabel.setGraphic(ImageUtil.getImageViewById(protocolListItem.getIconId()));
        headlineLabel.setGraphicTextGap(10);
        GridPane.setMargin(headlineLabel, new Insets(20, 30, 0, 30));
        gridPane.add(headlineLabel, columnIndex, rowIndex, 2, 1);

        if (protocolListItem.getTradeAppsAttributesType() != TradeAppsAttributes.Type.BISQ_EASY) {
            Label label = new Label(Res.get("tradeApps.comingSoon"));
            label.setOpacity(0.2);
            GridPane.setHalignment(label, HPos.RIGHT);
            GridPane.setMargin(label, new Insets(11, VERTICAL_MARGIN, 0, 0));
            gridPane.add(label, columnIndex + 2, rowIndex);
        }

        TextFlow subTitleLabel = GridPaneUtil.getInfoLabel(protocolListItem.getBasicInfo(),
                "bisq-text-3");
        subTitleLabel.setLineSpacing(1d);
        subTitleLabel.setMinHeight(40);
        GridPane.setMargin(subTitleLabel, new Insets(-17, VERTICAL_MARGIN, 0, VERTICAL_MARGIN));
        gridPane.add(subTitleLabel, columnIndex, ++rowIndex, 3, 1);

        Region separator = Layout.hLine();
        GridPane.setMargin(separator, new Insets(-14, VERTICAL_MARGIN, 0, VERTICAL_MARGIN));
        gridPane.add(separator, columnIndex, ++rowIndex, 3, 1);

        VBox markets = getParameterPane(Res.get("tradeApps.markets"),
                protocolListItem.getMarkets());
        GridPane.setMargin(markets, new Insets(-4, 0, 0, VERTICAL_MARGIN));
        gridPane.add(markets, columnIndex, ++rowIndex);

        if (!protocolListItem.getReleaseDate().isEmpty()) {
            VBox release = getParameterPane(Res.get("tradeApps.release"),
                    protocolListItem.getReleaseDate());
            GridPane.setMargin(release, new Insets(-4, VERTICAL_MARGIN, 0, 6));
            gridPane.add(release, columnIndex + 1, rowIndex);
        }

        VBox security = getParameterPane(Res.get("tradeApps.security"),
                protocolListItem.getTradeAppsAttributesType().getSecurity().ordinal(),
                protocolListItem.getSecurityInfo()
        );
        gridPane.add(security, columnIndex, ++rowIndex);
        GridPane.setMargin(security, new Insets(-13, VERTICAL_MARGIN, 0, VERTICAL_MARGIN));

        VBox privacy = getParameterPane(Res.get("tradeApps.privacy"),
                protocolListItem.getTradeAppsAttributesType().getPrivacy().ordinal(),
                protocolListItem.getPrivacyInfo()
        );
        gridPane.add(privacy, columnIndex + 1, rowIndex);
        GridPane.setMargin(privacy, new Insets(-13, VERTICAL_MARGIN, 0, 6));

        VBox convenience = getParameterPane(Res.get("tradeApps.convenience"),
                protocolListItem.getTradeAppsAttributesType().getConvenience().ordinal(),
                protocolListItem.getConvenienceInfo()
        );
        gridPane.add(convenience, columnIndex + 2, rowIndex);
        GridPane.setMargin(convenience, new Insets(-13, 0, 0, -19));

        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("medium-large-button");
        button.setOnAction(e -> controller.onSelect(protocolListItem));
        if (protocolListItem.getTradeAppsAttributesType() == TradeAppsAttributes.Type.BISQ_EASY) {
            button.setText(Res.get("tradeApps.select"));
            button.setDefaultButton(true);
        } else {
            button.setText(Res.get("action.learnMore"));
            button.getStyleClass().addAll("outlined-button", "grey-outlined-button");
        }
        GridPane.setMargin(button, new Insets(7, VERTICAL_MARGIN, HORIZONTAL_GAP, VERTICAL_MARGIN));
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
