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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.rules;

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
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BisqEasyGuideRulesView extends View<VBox, BisqEasyGuideRulesModel, BisqEasyGuideRulesController> {
    private final Button backButton, closeButton, confirmButton;
    private final Hyperlink learnMore;
    private final Text content;
    private Subscription widthPin;

    public BisqEasyGuideRulesView(BisqEasyGuideRulesModel model,
                                  BisqEasyGuideRulesController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("tradeGuide.tab3.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        content = new Text(Res.get("tradeGuide.tab3.content"));
        content.getStyleClass().addAll("bisq-text-13", "bisq-line-spacing-01");

        learnMore = new Hyperlink(Res.get("user.reputation.learnMore"));

        backButton = new Button(Res.get("back"));
        closeButton = new Button(Res.get("close"));
        closeButton.setDefaultButton(true);
        confirmButton = new Button(Res.get("tradeGuide.tab3.confirm"));
        confirmButton.setDefaultButton(true);
        HBox buttons = new HBox(20, backButton, closeButton, confirmButton);
        VBox.setVgrow(content, Priority.ALWAYS);
        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(headline, content, learnMore, buttons);
    }

    @Override
    protected void onViewAttached() {
        closeButton.setManaged(model.getTradeRulesConfirmed().get());
        closeButton.setVisible(model.getTradeRulesConfirmed().get());
        confirmButton.setManaged(!model.getTradeRulesConfirmed().get());
        confirmButton.setVisible(!model.getTradeRulesConfirmed().get());

        closeButton.setOnAction(e -> controller.onClose());
        confirmButton.setOnAction(e -> controller.onConfirm());
        backButton.setOnAction(e -> controller.onBack());
        learnMore.setOnAction(e -> controller.onLearnMore());

        widthPin = EasyBind.subscribe(root.widthProperty(),
                w -> content.setWrappingWidth(w.doubleValue() - 30));
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
        confirmButton.setOnAction(null);
        backButton.setOnAction(null);
        learnMore.setOnAction(null);

        widthPin.unsubscribe();
    }
}
