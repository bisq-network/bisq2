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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.negotiation;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeAssistantNegotiationView extends View<VBox, TradeAssistantNegotiationModel, TradeAssistantNegotiationController> {
    private final Button nextButton, backButton;
    private final Hyperlink openTradeGuide;
    private final Text content;
    private Subscription widthPin;

    public TradeAssistantNegotiationView(TradeAssistantNegotiationModel model,
                                         TradeAssistantNegotiationController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("tradeAssistant.negotiation.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        content = new Text(Res.get("tradeAssistant.negotiation.content"));
        content.getStyleClass().addAll("bisq-text-13", "bisq-line-spacing-01");

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);
        backButton = new Button(Res.get("action.back"));
        HBox buttons = new HBox(10, backButton, nextButton);

        openTradeGuide = new Hyperlink(Res.get("tradeAssistant.openTradeGuide"));

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(openTradeGuide, new Insets(0, 0, 10, 0));
        root.getChildren().addAll(headline, content, openTradeGuide, buttons);
    }

    @Override
    protected void onViewAttached() {
        backButton.setOnAction(e -> controller.onBack());
        nextButton.setOnAction(e -> controller.onNext());
        openTradeGuide.setOnAction(e -> controller.onOpenTradeGuide());

        widthPin = EasyBind.subscribe(root.widthProperty(),
                w -> content.setWrappingWidth(w.doubleValue() - 30));
    }

    @Override
    protected void onViewDetached() {
        backButton.setOnAction(null);
        nextButton.setOnAction(null);
        openTradeGuide.setOnAction(null);

        widthPin.unsubscribe();
    }
}
