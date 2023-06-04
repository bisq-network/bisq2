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
import bisq.desktop.components.controls.AutoSizeButton;
import bisq.desktop.components.controls.MaterialTextField;
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
public class TradeAssistantOfferView extends View<VBox, TradeAssistantOfferModel, TradeAssistantOfferController> {
    private final Button nextButton, openOfferDetailsButton, openUserProfileButton;
    private final Hyperlink openTradeGuide;
    private final MaterialTextField amount, paymentMethods;
    private final Label headline;
    private final VBox offerInfoHBox;
    private final Text offerTitle;
    private Subscription widthPin;

    public TradeAssistantOfferView(TradeAssistantOfferModel model,
                                   TradeAssistantOfferController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setFillWidth(true);
        root.setAlignment(Pos.TOP_LEFT);

        headline = new Label();
        headline.getStyleClass().add("bisq-text-9");

        offerTitle = new Text();
        offerTitle.getStyleClass().addAll("bisq-text-9");

        amount = getField(Res.get("tradeAssistant.offer.amount"));
        paymentMethods = getField(Res.get("tradeAssistant.offer.paymentMethods"));

        openOfferDetailsButton = new AutoSizeButton(Res.get("tradeAssistant.offer.openDetails"));
        openOfferDetailsButton.getStyleClass().add("outlined-button");

        openUserProfileButton = new AutoSizeButton();
        openUserProfileButton.getStyleClass().add("outlined-button");

        HBox buttons = new HBox(20, openOfferDetailsButton, openUserProfileButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox.setMargin(offerTitle, new Insets(0, 0, 5, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        offerInfoHBox = new VBox(10, offerTitle, amount, paymentMethods, buttons);
        offerInfoHBox.getStyleClass().add("chat-message-bg-peer-message");
        offerInfoHBox.setAlignment(Pos.CENTER_LEFT);
        offerInfoHBox.setPadding(new Insets(20));

        // todo
        openTradeGuide = new Hyperlink(Res.get("tradeAssistant.openTradeGuide"));

        nextButton = new Button(Res.get("next"));
        nextButton.setDefaultButton(true);

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(offerInfoHBox, new Insets(0, 0, 10, 0));
        root.getChildren().addAll(headline, offerInfoHBox, nextButton);
    }

    @Override
    protected void onViewAttached() {
        headline.textProperty().bind(model.getHeadline());
        offerTitle.textProperty().bind(model.getOfferTitle());
        amount.textProperty().bind(model.getAmount());
        paymentMethods.textProperty().bind(model.getPaymentMethods());
        openUserProfileButton.textProperty().bind(model.getOpenUserProfileButtonLabel());
        openUserProfileButton.setOnAction(e -> controller.onOpenUserProfile());
        openOfferDetailsButton.setOnAction(e -> controller.onOpenOfferDetails());
        nextButton.setOnAction(e -> controller.onNext());
        openTradeGuide.setOnAction(e -> controller.onOpenTradeGuide());
        widthPin = EasyBind.subscribe(offerInfoHBox.widthProperty(), w -> {
            if (w.doubleValue() > 0) {
                amount.setPrefWidth(w.doubleValue() - 30);
                paymentMethods.setPrefWidth(w.doubleValue() - 30);
            }
        });
    }

    @Override
    protected void onViewDetached() {
        headline.textProperty().unbind();
        offerTitle.textProperty().unbind();
        amount.textProperty().unbind();
        paymentMethods.textProperty().unbind();
        openUserProfileButton.textProperty().unbind();
        openUserProfileButton.setOnAction(null);
        openOfferDetailsButton.setOnAction(null);
        nextButton.setOnAction(null);
        openTradeGuide.setOnAction(null);
        widthPin.unsubscribe();
    }

    private MaterialTextField getField(String description) {
        MaterialTextField field = new MaterialTextField(description, null);
        field.setEditable(false);
        return field;
    }
}
