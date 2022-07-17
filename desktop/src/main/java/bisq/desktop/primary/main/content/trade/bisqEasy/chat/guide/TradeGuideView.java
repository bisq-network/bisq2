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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide;

import bisq.desktop.common.utils.Styles;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeGuideView extends TabView<TradeGuideModel, TradeGuideController> {

    private final Button closeButton;

    public TradeGuideView(TradeGuideModel model, TradeGuideController controller) {
        super(model, controller);

        // root.setPadding(new Insets(40, 68, 40, 68));
        root.getStyleClass().addAll("bisq-box-2");
        root.setPadding(new Insets(15, 30, 30, 30));

        Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-logo-green", "bisq-text-grey-9");
        //Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-white", "bisq-text-grey-9");
       
        addTab(Res.get("tradeGuide.tab1"),
                NavigationTarget.TRADE_GUIDE_TAB_1,
                styles);
        addTab(Res.get("tradeGuide.tab2"),
                NavigationTarget.TRADE_GUIDE_TAB_2,
                styles);
        addTab(Res.get("tradeGuide.tab3"),
                NavigationTarget.TRADE_GUIDE_TAB_3,
                styles);

        closeButton = BisqIconButton.createIconButton("close");

        headLine.setText(Res.get("tradeGuide.headline"));

        // Make tabs left aligned and headline on top
        tabs.getChildren().remove(0, 2); // remove headline and spacer

        headLine.getStyleClass().remove("bisq-content-headline-label");
        headLine.getStyleClass().add("bisq-text-17");

        HBox.setMargin(closeButton, new Insets(-1, -15, 0, 0));
        HBox hBox = new HBox(7, headLine, Spacer.fillHBox(), closeButton);

        VBox.setMargin(hBox, new Insets(0, 0, 17, 0));
        vBox.getChildren().add(0, hBox);

        line.getStyleClass().remove("bisq-darkest-bg");
        line.getStyleClass().add("bisq-mid-grey");

        StackPane.setMargin(lineAndMarker, new Insets(85, 30, 0, 0));
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
