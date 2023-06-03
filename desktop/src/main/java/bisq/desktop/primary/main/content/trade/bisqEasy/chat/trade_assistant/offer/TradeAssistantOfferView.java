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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.offer;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeAssistantOfferView extends View<VBox, TradeAssistantOfferModel, TradeAssistantOfferController> {
    // private final static int BUTTON_WIDTH = 140;
    private final Button nextButton, openOfferDetailsButton;
    private final Text content;
    private final Label offerInfo;
    private final HBox createOfferHBox;
    private final Label headline;

    private Subscription widthPin;

    public TradeAssistantOfferView(TradeAssistantOfferModel model,
                                   TradeAssistantOfferController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setFillWidth(true);
        root.setAlignment(Pos.TOP_LEFT);

        headline = new Label();
        headline.getStyleClass().add("bisq-text-headline-2");

        content = new Text(Res.get("tradeAssistant.offer.taker.subHeadline"));
        content.getStyleClass().addAll("bisq-text-13", "bisq-line-spacing-01");

        offerInfo = new Label();
        offerInfo.setWrapText(true);
        offerInfo.setId("chat-messages-message");

        openOfferDetailsButton = new Button(Res.get("tradeAssistant.offer.openDetails"));
        openOfferDetailsButton.getStyleClass().add("outlined-button");
        openOfferDetailsButton.setMinWidth(180);

        HBox.setHgrow(offerInfo, Priority.ALWAYS);
        createOfferHBox = new HBox(15, offerInfo, Spacer.fillHBox(), openOfferDetailsButton);
        createOfferHBox.getStyleClass().add("create-offer-message-my-offer");
        createOfferHBox.setAlignment(Pos.CENTER_LEFT);
        createOfferHBox.setPadding(new Insets(15, 26, 15, 15));

        nextButton = new Button(Res.get("tradeAssistant.offer.next"));
        nextButton.setDefaultButton(true);

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(createOfferHBox, new Insets(20, 0, 20, 0));
        root.getChildren().addAll(headline, /*content,*/ createOfferHBox, nextButton);
    }

    @Override
    protected void onViewAttached() {
        headline.textProperty().bind(model.getHeadline());
        offerInfo.textProperty().bind(model.getOfferInfo());
        openOfferDetailsButton.setOnAction(e -> controller.onOpenOfferDetails());
        nextButton.setOnAction(e -> controller.onNext());
        widthPin = EasyBind.subscribe(root.widthProperty(),
                w -> content.setWrappingWidth(w.doubleValue() - 30));
    }

    @Override
    protected void onViewDetached() {
        headline.textProperty().unbind();
        offerInfo.textProperty().unbind();
        openOfferDetailsButton.setOnAction(null);
        nextButton.setOnAction(null);
        widthPin.unsubscribe();
    }
}
