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

package bisq.desktop.main.content.bisq_easy.trade_wizard.direction;

import bisq.common.data.Pair;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardView;
import bisq.i18n.Res;
import bisq.offer.Direction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeWizardDirectionView extends View<StackPane, TradeWizardDirectionModel, TradeWizardDirectionController> {
    private final Button buyButton, sellButton;
    private final VBox reputationInfo;
    private final VBox content;
    private Subscription directionSubscription, showReputationInfoPin;
    private Button withoutReputationButton, backToBuyButton;
    private Button gainReputationButton;

    public TradeWizardDirectionView(TradeWizardDirectionModel model, TradeWizardDirectionController controller) {
        super(new StackPane(), model, controller);

        root.setAlignment(Pos.CENTER);

        content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("bisqEasy.createOffer.direction.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("bisqEasy.createOffer.direction.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        Pair<VBox, Button> buyPair = getBoxPair(Res.get("bisqEasy.createOffer.direction.buy"),
                Res.get("bisqEasy.createOffer.direction.buy.info"), "card-toggle-button");
        VBox buyBox = buyPair.getFirst();
        buyButton = buyPair.getSecond();

        Pair<VBox, Button> sellPair = getBoxPair(Res.get("bisqEasy.createOffer.direction.sell"),
                Res.get("bisqEasy.createOffer.direction.sell.info"), "card-toggle-button");
        VBox sellBox = sellPair.getFirst();
        sellButton = sellPair.getSecond();

        HBox directionBox = new HBox(25, buyBox, sellBox);
        directionBox.setAlignment(Pos.CENTER);

        VBox.setMargin(directionBox, new Insets(10, 0, 0, 0));
        content.getChildren().addAll(Spacer.fillVBox(), headLineLabel, subtitleLabel, directionBox, Spacer.fillVBox());

        reputationInfo = new VBox(20);
        setupReputationInfo();

        StackPane.setMargin(reputationInfo, new Insets(-TradeWizardView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, reputationInfo);
    }

    @Override
    protected void onViewAttached() {
        buyButton.disableProperty().bind(model.getBuyButtonDisabled());
        buyButton.setOnAction(evt -> controller.onSelectDirection(Direction.BUY));
        sellButton.setOnAction(evt -> controller.onSelectDirection(Direction.SELL));
        gainReputationButton.setOnAction(evt -> controller.onGainReputation());
        withoutReputationButton.setOnAction(evt -> controller.onTradeWithoutReputation());
        backToBuyButton.setOnAction(evt -> controller.onCloseReputationInfo());

        directionSubscription = EasyBind.subscribe(model.getDirection(), direction -> {
            if (direction != null) {
                buyButton.setDefaultButton(direction == Direction.BUY);
                sellButton.setDefaultButton(direction == Direction.SELL);
            }
        });

        showReputationInfoPin = EasyBind.subscribe(model.getShowReputationInfo(),
                showReputationInfo -> {
                    if (showReputationInfo) {
                        reputationInfo.setVisible(true);
                        reputationInfo.setOpacity(1);
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(reputationInfo, 450);
                    } else {
                        Transitions.removeEffect(content);
                        if (reputationInfo.isVisible()) {
                            Transitions.fadeOut(reputationInfo, Transitions.DEFAULT_DURATION / 2,
                                    () -> reputationInfo.setVisible(false));
                        }
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        if (model.getShowReputationInfo().get()) {
            Transitions.removeEffect(content);
        }
        buyButton.disableProperty().unbind();

        buyButton.setOnAction(null);
        sellButton.setOnAction(null);
        gainReputationButton.setOnAction(null);
        withoutReputationButton.setOnAction(null);
        backToBuyButton.setOnAction(null);

        directionSubscription.unsubscribe();
        showReputationInfoPin.unsubscribe();
    }

    private Pair<VBox, Button> getBoxPair(String title, String info, String style) {
        Button button = new Button(title);
        button.getStyleClass().addAll(style, "bisq-easy-trade-wizard-direction-button");
        button.setAlignment(Pos.CENTER);
        int width = 235;
        button.setMinWidth(width);
        button.setMinHeight(112);

        Label infoLabel = new Label(info);
        infoLabel.getStyleClass().add("bisq-text-3");
        infoLabel.setMaxWidth(width);
        infoLabel.setWrapText(true);
        infoLabel.setTextAlignment(TextAlignment.CENTER);
        infoLabel.setAlignment(Pos.CENTER);

        VBox vBox = new VBox(8, button, infoLabel);
        vBox.setAlignment(Pos.CENTER);

        return new Pair<>(vBox, button);
    }

    private void setupReputationInfo() {
        double width = 700;
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.getStyleClass().setAll("trade-wizard-feedback-bg");
        contentBox.setPadding(new Insets(30));
        contentBox.setMaxWidth(width);

        // We don't use setManaged as the transition would not work as expected if set to false
        reputationInfo.setVisible(false);
        reputationInfo.setAlignment(Pos.TOP_CENTER);
        Label headLineLabel = new Label(Res.get("bisqEasy.createOffer.direction.feedback.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");
        headLineLabel.setTextAlignment(TextAlignment.CENTER);
        headLineLabel.setAlignment(Pos.CENTER);
        headLineLabel.setMaxWidth(width - 60);

        Label subtitleLabel1 = new Label(Res.get("bisqEasy.createOffer.direction.feedback.subTitle1"));
        subtitleLabel1.setMaxWidth(width - 60);
        subtitleLabel1.getStyleClass().addAll("bisq-text-21", "wrap-text");

        gainReputationButton = new Button(Res.get("bisqEasy.createOffer.direction.feedback.gainReputation"));
        gainReputationButton.getStyleClass().add("outlined-button");

        Label subtitleLabel2 = new Label(Res.get("bisqEasy.createOffer.direction.feedback.subTitle2"));
        subtitleLabel2.setMaxWidth(width - 60);
        subtitleLabel2.getStyleClass().addAll("bisq-text-21", "wrap-text");

        withoutReputationButton = new Button(Res.get("bisqEasy.createOffer.direction.feedback.tradeWithoutReputation"));
        backToBuyButton = new Button(Res.get("bisqEasy.createOffer.direction.feedback.backToBuy"));

        HBox buttons = new HBox(7, backToBuyButton, withoutReputationButton);
        buttons.setAlignment(Pos.CENTER);

        VBox.setMargin(gainReputationButton, new Insets(10, 0, 20, 0));
        VBox.setMargin(buttons, new Insets(30, 0, 0, 0));
        contentBox.getChildren().addAll(headLineLabel,
                subtitleLabel1,
                gainReputationButton,
                subtitleLabel2,
                buttons);
        reputationInfo.getChildren().addAll(contentBox, Spacer.fillVBox());
    }
}
