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
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeGuideView extends TabView<TradeGuideModel, TradeGuideController> {
    private Button collapseButton, expandButton;
    private HBox hBox;
    private Subscription isCollapsedPin;

    public TradeGuideView(TradeGuideModel model, TradeGuideController controller) {
        super(model, controller);

        root.getStyleClass().addAll("bisq-box-2");
        root.setPadding(new Insets(15, 30, 30, 30));
        VBox.setMargin(contentPane, new Insets(10, 0, 0, 0));

        Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-logo-green", "bisq-text-grey-9");
        addTab(Res.get("tradeGuide.tab1"),
                NavigationTarget.TRADE_GUIDE_TAB_1,
                styles);
        addTab(Res.get("tradeGuide.tab2"),
                NavigationTarget.TRADE_GUIDE_TAB_2,
                styles);
        addTab(Res.get("tradeGuide.tab3"),
                NavigationTarget.TRADE_GUIDE_TAB_3,
                styles);
    }

    @Override
    protected void onViewAttached() {
        line.prefWidthProperty().unbind();
        double paddings = root.getPadding().getLeft() + root.getPadding().getRight();
        line.prefWidthProperty().bind(root.widthProperty().subtract(paddings));

        collapseButton.setOnAction(e -> controller.onCollapse());
        expandButton.setOnAction(e -> controller.onExpand());

        isCollapsedPin = EasyBind.subscribe(model.getIsCollapsed(), isCollapsed -> {
            collapseButton.setManaged(!isCollapsed);
            collapseButton.setVisible(!isCollapsed);
            expandButton.setManaged(isCollapsed);
            expandButton.setVisible(isCollapsed);

            if (isCollapsed) {
                VBox.setMargin(hBox, new Insets(0, 0, -17, 0));
            } else {
                VBox.setMargin(hBox, new Insets(0, 0, 17, 0));
            }

            tabs.setManaged(!isCollapsed);
            tabs.setVisible(!isCollapsed);
            contentPane.setManaged(!isCollapsed);
            contentPane.setVisible(!isCollapsed);
            lineAndMarker.setManaged(!isCollapsed);
            lineAndMarker.setVisible(!isCollapsed);
        });
    }

    @Override
    protected void onViewDetached() {
        line.prefWidthProperty().unbind();
        collapseButton.setOnAction(null);
        expandButton.setOnAction(null);
        isCollapsedPin.unsubscribe();
    }

    @Override
    protected void setupTopBox() {
        headLine = new Label();
        headLine.setText(Res.get("tradeGuide.headline"));
        headLine.getStyleClass().add("bisq-text-17");

        collapseButton = BisqIconButton.createIconButton("collapse");
        expandButton = BisqIconButton.createIconButton("expand");

        HBox.setMargin(collapseButton, new Insets(-1, -15, 0, 0));
        HBox.setMargin(expandButton, new Insets(-1, -15, 0, 0));
        HBox.setMargin(headLine, new Insets(0, 0, 0, -2));
        hBox = new HBox(headLine, Spacer.fillHBox(), collapseButton, expandButton);

        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        tabs.setMinHeight(35);

        topBox = new VBox();
        VBox.setMargin(hBox, new Insets(0, 0, 17, 0));
        topBox.getChildren().addAll(hBox, tabs);
    }

    @Override
    protected void setupLineAndMarker() {
        super.setupLineAndMarker();

        line.getStyleClass().remove("bisq-dark-bg");
        line.getStyleClass().add("bisq-mid-grey");
    }
}
