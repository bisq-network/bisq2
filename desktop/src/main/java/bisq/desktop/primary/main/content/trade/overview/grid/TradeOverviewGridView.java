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

package bisq.desktop.primary.main.content.trade.overview.grid;

import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.primary.main.content.trade.overview.ProtocolListItem;
import bisq.desktop.primary.main.content.trade.overview.TradeOverviewBaseView;
import bisq.i18n.Res;
import bisq.protocol.SwapProtocol;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeOverviewGridView extends TradeOverviewBaseView<GridPane, TradeOverviewGridModel, TradeOverviewGridController> {
    private static final int VERTICAL_MARGIN = 30;

    public TradeOverviewGridView(TradeOverviewGridModel model, TradeOverviewGridController controller) {
        super(new GridPane(), model, controller);
        root.setHgap(25);
        root.setVgap(25);
        root.setPadding(new Insets(25, 0, 0, 0));

        int index = 0;
        for (ProtocolListItem protocol : model.getSortedItems()) {
            Pane protocolBox = getProtocolBox(protocol);
            GridPane.setHgrow(protocolBox, Priority.ALWAYS);
            root.add(protocolBox, index % 2, index >> 1);
            index++;
        }
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    private Pane getProtocolBox(ProtocolListItem protocol) {
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(24, VERTICAL_MARGIN, 20, VERTICAL_MARGIN));
        gridPane.setVgap(12);
        gridPane.setHgap(12);
        gridPane.getStyleClass().add("bisq-box-2");
        gridPane.setMinWidth(360);
        gridPane.setCursor(Cursor.HAND);
        gridPane.setOnMouseClicked(e -> controller.onSelect(protocol));

        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        ColumnConstraints col3 = new ColumnConstraints();
        col1.setPercentWidth(100 / 3d);
        col2.setPercentWidth(100 / 3d);
        col3.setPercentWidth(100 / 3d);
        gridPane.getColumnConstraints().addAll(col1, col2, col3);

        int rowIndex = 0;
        Label headlineLabel = new Label(protocol.getProtocolsName());
        headlineLabel.setAlignment(Pos.TOP_LEFT);
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        headlineLabel.setGraphic(ImageUtil.getImageViewById(protocol.getIconId()));
        GridPane.setMargin(headlineLabel, new Insets(-5, 0, 0, 0));
        gridPane.add(headlineLabel, 0, rowIndex, 2, 1);

        if (protocol.getSwapProtocolType() != SwapProtocol.Type.BISQ_EASY) {
            Label label = new Label(Res.get("trade.protocols.comingSoon"));
            label.setOpacity(0.1);
            GridPane.setHalignment(label, HPos.RIGHT);
            GridPane.setMargin(label, new Insets(-8, 0, 0, 0));
            gridPane.add(label, 2, rowIndex);
        }

        Label subTitleLabel = new Label(protocol.getBasicInfo());
        subTitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");
        subTitleLabel.setAlignment(Pos.TOP_LEFT);
        subTitleLabel.setMaxWidth(384);
        subTitleLabel.setMinHeight(40);
        GridPane.setMargin(subTitleLabel, new Insets(-10, 0, 0, 0));
        gridPane.add(subTitleLabel, 0, ++rowIndex, 3, 1);

        Region separator = Layout.separator();
        GridPane.setMargin(separator, new Insets(5, 0, 15, 0));
        gridPane.add(separator, 0, ++rowIndex, 3, 1);

        VBox markets = getParameterPane(Res.get("markets"), protocol.getMarkets());
        gridPane.add(markets, 0, ++rowIndex);

        VBox release = getParameterPane(Res.get("trade.protocols.table.header.release"), protocol.getReleaseDate());
        gridPane.add(release, 1, rowIndex);

        VBox security = getParameterPane(Res.get("trade.protocols.table.header.security"),
                protocol.getSwapProtocolType().getSecurity().ordinal(), protocol.getSecurityInfo()
        );
        gridPane.add(security, 0, ++rowIndex);

        VBox privacy = getParameterPane(Res.get("trade.protocols.table.header.privacy"),
                protocol.getSwapProtocolType().getPrivacy().ordinal(),
                protocol.getPrivacyInfo()
        );
        gridPane.add(privacy, 1, rowIndex);

        VBox convenience = getParameterPane(Res.get("trade.protocols.table.header.convenience"),
                protocol.getSwapProtocolType().getConvenience().ordinal(),
                protocol.getConvenienceInfo()
        );
        gridPane.add(convenience, 2, rowIndex);

        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("medium-large-button");
        button.setOnAction(e -> controller.onSelect(protocol));
        if (protocol.getSwapProtocolType() == SwapProtocol.Type.BISQ_EASY) {
            button.setText(Res.get("select"));
            button.getStyleClass().add("outlined-button");
        } else {
            button.setText(Res.get("learnMore"));
            button.getStyleClass().addAll("outlined-button", "grey-outlined-button");
        }
        GridPane.setMargin(button, new Insets(20, 0, 5, 0));
        gridPane.add(button, 0, ++rowIndex, 3, 1);

        return gridPane;
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
