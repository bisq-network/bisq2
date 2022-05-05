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

package bisq.desktop.primary.main.content.trade.overview;

import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.TabViewChild;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeOverviewGridView extends View<GridPane, TradeOverviewModel, TradeOverviewGridController> implements TabViewChild {

    public TradeOverviewGridView(TradeOverviewModel model, TradeOverviewGridController controller) {
        super(new GridPane(), model, controller);
        getRoot().setHgap(32);
        getRoot().setVgap(32);
    }

    @Override
    protected void onViewAttached() {
        getRoot().getChildren().clear();
        int index = 0;
        for (ProtocolListItem protocol: model.getListItems()) {
            getRoot().add(getProtocolBox(protocol), index % 2, index >> 1);
            index++;
        }
    }

    @Override
    protected void onViewDetached() {
    }

    private VBox getProtocolBox(ProtocolListItem protocol) {
        VBox box = new VBox();
        box.getStyleClass().add("bisq-box-1");
        box.setMinWidth(400);
        GridPane.setHgrow(box, Priority.ALWAYS);

        Label headlineLabel = new Label(protocol.getProtocolsName());
        headlineLabel.setPadding(new Insets(24, 24, 0, 24));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        headlineLabel.setGraphic(ImageUtil.getImageViewById(protocol.getIconId()));
        box.getChildren().add(headlineLabel);

        Label basicInfo = new Label(protocol.getBasicInfo());
        basicInfo.getStyleClass().addAll("bisq-text-4", "wrap-text");
        basicInfo.setPadding(new Insets(12, 24, 0, 24));
        basicInfo.setAlignment(Pos.TOP_LEFT);
        basicInfo.setMaxWidth(420);
        basicInfo.setMinHeight(80);
        box.getChildren().add(basicInfo);

        Region line = new Region();
        line.getStyleClass().addAll("border-bottom");
        box.getChildren().add(line);

        GridPane paramsPane = new GridPane();
        paramsPane.setPadding(new Insets(24));
        paramsPane.setVgap(28);
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

        return box;
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
        VBox box = new VBox(6);
        GridPane.setHgrow(box, Priority.ALWAYS);
        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.getStyleClass().add("bisq-text-5");
        box.getChildren().addAll(titleLabel, node);
        
        return box;
    }

    private Node getStarsNode(int value, String tooltipText) {
        final HBox hBox = new HBox();
        hBox.setSpacing(5);
        hBox.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < 3; i++) {
            Label label = Icons.getIcon(AwesomeIcon.STAR, "1.3em");
            label.setMouseTransparent(false);
            label.setOpacity(i <= value ? 1 : 0.2);
            hBox.getChildren().add(label);
        }

        final Tooltip tooltip = new Tooltip();
        tooltip.setMaxWidth(300);
        tooltip.setWrapText(true);
        tooltip.setText(tooltipText);
        Tooltip.install(hBox, tooltip);
        
        return hBox;
    }
}
