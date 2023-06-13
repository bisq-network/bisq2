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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.details;

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
public class TradeDetailsView extends View<VBox, TradeDetailsModel, TradeDetailsController> {
    private final Button backButton, openOfferDetailsButton, openUserProfileButton;
    private final Hyperlink openTradeGuide;
    private final MaterialTextField amount, paymentMethods;
    private final Label headline;
    private final VBox vBox;
    private final Text offerTitle;
    private Subscription widthPin;

    public TradeDetailsView(TradeDetailsModel model,
                            TradeDetailsController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setFillWidth(true);
        root.setAlignment(Pos.TOP_LEFT);

        headline = new Label();
        headline.getStyleClass().add("bisq-text-9");

        offerTitle = new Text();
        offerTitle.getStyleClass().addAll("bisq-text-9");

        amount = getField(Res.get("bisqEasy.assistant.tradeDetails.amount"));
        paymentMethods = getField(Res.get("bisqEasy.assistant.tradeDetails.paymentMethods"));

        openOfferDetailsButton = new AutoSizeButton(Res.get("bisqEasy.assistant.tradeDetails.openDetails"));
        openOfferDetailsButton.getStyleClass().add("outlined-button");

        openUserProfileButton = new AutoSizeButton();
        openUserProfileButton.getStyleClass().add("outlined-button");

        HBox buttons = new HBox(20, openOfferDetailsButton, openUserProfileButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox.setMargin(offerTitle, new Insets(0, 0, 5, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        vBox = new VBox(10, offerTitle, amount, paymentMethods, buttons);
        vBox.getStyleClass().add("bisq-content-bg");

        vBox.setAlignment(Pos.CENTER_LEFT);
        vBox.setPadding(new Insets(20));

        // todo
        openTradeGuide = new Hyperlink(Res.get("bisqEasy.assistant.header.openTradeGuide"));

        backButton = new Button(Res.get("action.back"));
        backButton.setDefaultButton(true);

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(vBox, new Insets(0, 0, 10, 0));
        root.getChildren().addAll(headline, vBox, backButton);
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
        backButton.setOnAction(e -> controller.onBack());
        openTradeGuide.setOnAction(e -> controller.onOpenTradeGuide());
        widthPin = EasyBind.subscribe(vBox.widthProperty(), w -> {
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
        backButton.setOnAction(null);
        openTradeGuide.setOnAction(null);
        widthPin.unsubscribe();
    }

    private MaterialTextField getField(String description) {
        MaterialTextField field = new MaterialTextField(description, null);
        field.setEditable(false);
        return field;
    }
}
