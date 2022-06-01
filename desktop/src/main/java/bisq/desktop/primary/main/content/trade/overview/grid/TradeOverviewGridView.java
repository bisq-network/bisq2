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
import bisq.desktop.layout.Layout;
import bisq.desktop.primary.main.content.trade.overview.ProtocolListItem;
import bisq.desktop.primary.main.content.trade.overview.TradeOverviewBaseView;
import bisq.i18n.Res;
import bisq.protocol.SwapProtocol;
import de.jensd.fx.fontawesome.AwesomeIcon;
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
        root.setHgap(26);
        root.setVgap(26);
    }

    @Override
    protected void onViewAttached() {
        root.getChildren().clear();
        int index = 0;
        for (ProtocolListItem protocol : model.getSortedItems()) {
            Pane protocolBox = getProtocolBox(protocol);
            if (protocol.getSwapProtocolType() != SwapProtocol.Type.SATOSHI_SQUARE) {
                protocolBox.setOpacity(0.4);
            }
            root.add(protocolBox, index % 2, index >> 1);
            index++;
        }
    }

    @Override
    protected void onViewDetached() {
    }

    private Pane getProtocolBox(ProtocolListItem protocol) {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("bisq-box-2");
        pane.setMinWidth(360);
        GridPane.setHgrow(pane, Priority.ALWAYS);

        VBox box = new VBox();
        pane.getChildren().add(box);

        Label headlineLabel = new Label(protocol.getProtocolsName());
        headlineLabel.setPadding(new Insets(16, VERTICAL_MARGIN, 0, VERTICAL_MARGIN));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        headlineLabel.setGraphic(ImageUtil.getImageViewById(protocol.getIconId()));
        box.getChildren().add(headlineLabel);

        Label basicInfo = new Label(protocol.getBasicInfo());
        basicInfo.getStyleClass().addAll("bisq-text-3", "wrap-text");
        basicInfo.setPadding(new Insets(4, VERTICAL_MARGIN, 0, VERTICAL_MARGIN));
        basicInfo.setAlignment(Pos.TOP_LEFT);
        basicInfo.setMaxWidth(384);
        basicInfo.setMinHeight(60);
        box.getChildren().addAll(basicInfo, Layout.separator());

        GridPane paramsPane = new GridPane();
        paramsPane.setPadding(new Insets(24, VERTICAL_MARGIN, 20, VERTICAL_MARGIN));
        paramsPane.setVgap(12);
        paramsPane.add(
                getParameterPane(Res.get("markets"), protocol.getMarkets()),
                0,
                0
        );

        paramsPane.add(
                getParameterPane(
                        Res.get("trade.protocols.table.header.release"),
                        protocol.getReleaseDate()
                ),
                1,
                0
        );

        paramsPane.add(
                getParameterPane(
                        Res.get("trade.protocols.table.header.security"),
                        protocol.getSwapProtocolType().getSecurity().ordinal(),
                        protocol.getSecurityInfo()
                ),
                0,
                1
        );

        paramsPane.add(
                getParameterPane(
                        Res.get("trade.protocols.table.header.privacy"),
                        protocol.getSwapProtocolType().getPrivacy().ordinal(),
                        protocol.getPrivacyInfo()
                ),
                1,
                1
        );

        paramsPane.add(
                getParameterPane(
                        Res.get("trade.protocols.table.header.convenience"),
                        protocol.getSwapProtocolType().getConvenience().ordinal(),
                        protocol.getConvenienceInfo()
                ),
                2,
                1
        );

        box.getChildren().add(paramsPane);
        box.setCursor(Cursor.HAND);
        box.setOnMouseClicked(e -> controller.onSelect(protocol));

        String title = protocol.getSwapProtocolType() == SwapProtocol.Type.SATOSHI_SQUARE ?
                Res.get("select") :
                Res.get("learnMore");
        Button button = new Button(title);
        button.getStyleClass().setAll("bisq-transparent-button", "bisq-text-3");
        button.setOnAction(e -> controller.onSelect(protocol));
        StackPane.setAlignment(button, Pos.TOP_RIGHT);
        StackPane.setMargin(button, new Insets(20, 14, 0, 0));
        pane.getChildren().add(button);

        return pane;
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
        final HBox hBox = new HBox();
        hBox.setPadding(new Insets(5, 0, 5, 0));
        hBox.setSpacing(5);

        for (int i = 0; i < 3; i++) {
            Label label = Icons.getIcon(AwesomeIcon.STAR, "1.2em");
            label.setMouseTransparent(false);
            label.setOpacity(i <= value ? 1 : 0.2);
            hBox.getChildren().add(label);
        }

        final Tooltip tooltip = new Tooltip();
        tooltip.setMaxWidth(240);
        tooltip.setWrapText(true);
        tooltip.setText(tooltipText);
        Tooltip.install(hBox, tooltip);

        return hBox;
    }
}
