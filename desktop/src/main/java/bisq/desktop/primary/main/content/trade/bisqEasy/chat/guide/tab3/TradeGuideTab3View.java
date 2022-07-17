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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.tab3;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeGuideTab3View extends View<VBox, TradeGuideTab3Model, TradeGuideTab3Controller> {
    private final Button  closeButton, backButton;
    private final Hyperlink learnMore;

    public TradeGuideTab3View(TradeGuideTab3Model model,
                              TradeGuideTab3Controller controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("tradeGuide.tab3.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        Label content = new Label(Res.get("tradeGuide.tab3.content"));
        content.getStyleClass().addAll("bisq-text-13", "wrap-text");

        closeButton = new Button(Res.get("close"));
        learnMore = new Hyperlink(Res.get("reputation.learnMore"));
        backButton = new Button(Res.get("back"));

        HBox buttons = new HBox(20, backButton, closeButton);

        VBox.setVgrow(content, Priority.ALWAYS);
        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(headline, content, learnMore, buttons
        );
    }

    @Override
    protected void onViewAttached() {
        closeButton.setOnAction(e -> controller.onClose());
        backButton.setOnAction(e -> controller.onBack());
        learnMore.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
        backButton.setOnAction(null);
        learnMore.setOnAction(null);
    }
}
