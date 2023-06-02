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
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeAssistantOfferView extends View<VBox, TradeAssistantOfferModel, TradeAssistantOfferController> {
    private final Button nextButton, openOfferDetailsButton;
    private final Hyperlink learnMore;
    private final Text content;
    private final MaterialTextField id, direction, date, market, paymentMethods, baseSideAmount, quoteSideAmount,
            pricePremiumAsPercentage, pricePremiumInBaseCurrency, pricePremiumInQuoteCurrency, makersTradeTerms,
            requiredTotalReputationScore, minAmountAsPercentage;

    private Subscription widthPin;
    private final VBox formVBox;

    public TradeAssistantOfferView(TradeAssistantOfferModel model,
                                   TradeAssistantOfferController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setFillWidth(true);
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("tradeAssistant.offer.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        content = new Text(Res.get("tradeAssistant.offer.content"));
        content.getStyleClass().addAll("bisq-text-13", "bisq-line-spacing-01");

        nextButton = new Button(Res.get("next"));
        openOfferDetailsButton = new Button(Res.get("tradeAssistant.offer.openDetails"));
        nextButton.setDefaultButton(true);
        // HBox buttons = new HBox(10, openOfferDetailsButton, nextButton);

        learnMore = new Hyperlink(Res.get("learnMore"));

        formVBox = new VBox(20);

        id = addField("id");
        direction = addField("direction");
        date = addField("date");
        market = addField("market");
        paymentMethods = addField("paymentMethods");
        baseSideAmount = addField("baseSideAmount");
        quoteSideAmount = addField("quoteSideAmount");
        pricePremiumAsPercentage = addField("pricePremiumAsPercentage");
        pricePremiumInBaseCurrency = addField("pricePremiumInBaseCurrency");
        pricePremiumInQuoteCurrency = addField("pricePremiumInQuoteCurrency");
        makersTradeTerms = addField("makersTradeTerms");
        requiredTotalReputationScore = addField("requiredTotalReputationScore");
        minAmountAsPercentage = addField("minAmountAsPercentage");

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(learnMore, new Insets(0, 0, 10, 0));
        root.getChildren().addAll(headline, formVBox, content, learnMore, openOfferDetailsButton, nextButton);
    }

    @Override
    protected void onViewAttached() {
        id.textProperty().bind(model.getId());
        direction.textProperty().bind(model.getDirection());
        date.textProperty().bind(model.getDate());
        market.textProperty().bind(model.getMarket());
        paymentMethods.textProperty().bind(model.getPaymentMethods());
        baseSideAmount.textProperty().bind(model.getBaseSideAmount());
        quoteSideAmount.textProperty().bind(model.getQuoteSideAmount());
        pricePremiumAsPercentage.textProperty().bind(model.getPricePremiumAsPercentage());
        pricePremiumInBaseCurrency.textProperty().bind(model.getPricePremiumInBaseCurrency());
        pricePremiumInQuoteCurrency.textProperty().bind(model.getPricePremiumInQuoteCurrency());
        makersTradeTerms.textProperty().bind(model.getMakersTradeTerms());
        requiredTotalReputationScore.textProperty().bind(model.getRequiredTotalReputationScore());
        minAmountAsPercentage.textProperty().bind(model.getMinAmountAsPercentage());

        openOfferDetailsButton.setOnAction(e -> controller.onOpenOfferDetails());
        nextButton.setOnAction(e -> controller.onNext());
        learnMore.setOnAction(e -> controller.onLearnMore());
        widthPin = EasyBind.subscribe(root.widthProperty(),
                w -> content.setWrappingWidth(w.doubleValue() - 30));
    }

    @Override
    protected void onViewDetached() {
        id.textProperty().unbind();
        direction.textProperty().unbind();
        date.textProperty().unbind();
        market.textProperty().unbind();
        paymentMethods.textProperty().unbind();
        baseSideAmount.textProperty().unbind();
        quoteSideAmount.textProperty().unbind();
        pricePremiumAsPercentage.textProperty().unbind();
        pricePremiumInBaseCurrency.textProperty().unbind();
        pricePremiumInQuoteCurrency.textProperty().unbind();
        makersTradeTerms.textProperty().unbind();
        requiredTotalReputationScore.textProperty().unbind();
        minAmountAsPercentage.textProperty().unbind();

        openOfferDetailsButton.setOnAction(null);
        nextButton.setOnAction(null);
        learnMore.setOnAction(null);
        widthPin.unsubscribe();
    }


    private MaterialTextField addField(String key) {
        MaterialTextField field = new MaterialTextField(Res.get("tradeAssistant.offer." + key), null);
        field.setEditable(false);
        field.setIconTooltip(Res.get("tradeAssistant.offer." + key + ".tooltip"));
        // formVBox.getChildren().add(field);
        return field;
    }

    private MaterialTextArea addTextArea(String description, String prompt) {
        MaterialTextArea field = new MaterialTextArea(description, prompt);
        field.setEditable(false);
        field.setFixedHeight(2 * 56 + 20); // MaterialTextField has height 56
        formVBox.getChildren().add(field);
        return field;
    }
}
