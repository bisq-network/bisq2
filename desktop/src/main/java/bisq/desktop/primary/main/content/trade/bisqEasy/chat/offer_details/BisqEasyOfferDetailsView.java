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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.offer_details;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.primary.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyOfferDetailsView extends View<VBox, BisqEasyOfferDetailsModel, BisqEasyOfferDetailsController> {
    private final Button closeButton;
    private final MaterialTextField id, offerType, date, paymentMethods, baseSideAmount, quoteSideAmount,
            price, requiredTotalReputationScore;
    private final MaterialTextArea makersTradeTerms;

    public BisqEasyOfferDetailsView(BisqEasyOfferDetailsModel model,
                                    BisqEasyOfferDetailsController controller) {
        super(new VBox(), model, controller);

        root.setPadding(new Insets(30));
        root.setMinWidth(OverlayModel.WIDTH);
        root.setMinHeight(OverlayModel.HEIGHT);
        root.setSpacing(20);
        root.setFillWidth(true);
        root.setAlignment(Pos.TOP_LEFT);

        Label mainHeadline = new Label(Res.get("bisqEasy.offerDetails.headline"));
        mainHeadline.getStyleClass().add("bisq-text-headline-2");

        VBox mainFields = new VBox(20);

        offerType = getField(Res.get("bisqEasy.offerDetails.offerType"));
        mainFields.getChildren().add(offerType);

        quoteSideAmount = getField("");
        baseSideAmount = getField(Res.get("bisqEasy.offerDetails.baseSideAmount"));
        price = getField(Res.get(""));
        HBox.setHgrow(quoteSideAmount, Priority.ALWAYS);
        HBox.setHgrow(price, Priority.ALWAYS);
        HBox.setHgrow(baseSideAmount, Priority.ALWAYS);
        HBox amountsAndPriceBox = new HBox(20, baseSideAmount, price, quoteSideAmount);
        mainFields.getChildren().add(amountsAndPriceBox);

        paymentMethods = getField(Res.get("bisqEasy.offerDetails.paymentMethods"));
        mainFields.getChildren().add(paymentMethods);

        Label detailHeadline = new Label(Res.get("bisqEasy.offerDetails.details"));
        detailHeadline.getStyleClass().add("bisq-text-headline-2");

        VBox detailFields = new VBox(20);

        id = getField(Res.get("bisqEasy.offerDetails.id"));
        date = getField(Res.get("bisqEasy.offerDetails.date"));

        HBox.setHgrow(id, Priority.ALWAYS);
        HBox.setHgrow(date, Priority.ALWAYS);
        HBox detailsBox = new HBox(20, id, date);
        detailFields.getChildren().add(detailsBox);

        makersTradeTerms = addTextArea(Res.get("bisqEasy.offerDetails.makersTradeTerms"));
        makersTradeTerms.setPrefHeight(30);
        detailFields.getChildren().add(makersTradeTerms);

        requiredTotalReputationScore = getField(Res.get("bisqEasy.offerDetails.requiredTotalReputationScore"));
        detailFields.getChildren().add(requiredTotalReputationScore);

        closeButton = new Button(Res.get("close"));
        closeButton.setDefaultButton(true);
        HBox buttonBox = new HBox(/*Spacer.fillHBox(),*/ closeButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(detailHeadline, new Insets(20, 0, 0, 0));
        root.getChildren().addAll(mainHeadline, mainFields, detailHeadline, detailFields, buttonBox);
    }

    @Override
    protected void onViewAttached() {
        id.textProperty().bind(model.getId());
        offerType.textProperty().bind(model.getOfferType());
        date.textProperty().bind(model.getDate());
        paymentMethods.textProperty().bind(model.getPaymentMethods());
        baseSideAmount.textProperty().bind(model.getBaseSideAmount());
        quoteSideAmount.textProperty().bind(model.getQuoteSideAmount());
        quoteSideAmount.descriptionProperty().bind(model.getQuoteSideAmountDescription());
        price.textProperty().bind(model.getPrice());
        price.descriptionProperty().bind(model.getPriceDescription());
        makersTradeTerms.textProperty().bind(model.getMakersTradeTerms());
        makersTradeTerms.visibleProperty().bind(model.getMakersTradeTermsVisible());
        makersTradeTerms.managedProperty().bind(model.getMakersTradeTermsVisible());
        requiredTotalReputationScore.textProperty().bind(model.getRequiredTotalReputationScore());
        requiredTotalReputationScore.visibleProperty().bind(model.getRequiredTotalReputationScoreVisible());
        requiredTotalReputationScore.managedProperty().bind(model.getRequiredTotalReputationScoreVisible());

        closeButton.setOnAction(e -> controller.onClose());

        UIThread.runOnNextRenderFrame(closeButton::requestFocus);
    }

    @Override
    protected void onViewDetached() {
        id.textProperty().unbind();
        offerType.textProperty().unbind();
        date.textProperty().unbind();
        paymentMethods.textProperty().unbind();
        baseSideAmount.textProperty().unbind();
        quoteSideAmount.textProperty().unbind();
        quoteSideAmount.descriptionProperty().unbind();
        price.textProperty().unbind();
        price.descriptionProperty().unbind();
        makersTradeTerms.textProperty().unbind();
        makersTradeTerms.visibleProperty().unbind();
        makersTradeTerms.managedProperty().unbind();
        requiredTotalReputationScore.textProperty().unbind();
        requiredTotalReputationScore.visibleProperty().unbind();
        requiredTotalReputationScore.managedProperty().unbind();

        closeButton.setOnAction(null);
    }


    private MaterialTextField getField(String description) {
        MaterialTextField field = new MaterialTextField(description, null);
        field.setEditable(false);
        field.setFocusTraversable(false);
        return field;
    }

    private MaterialTextArea addTextArea(String description) {
        MaterialTextArea field = new MaterialTextArea(description, null);
        field.setEditable(false);
        field.setFocusTraversable(false);
        field.setFixedHeight(2 * 56 + 20); // MaterialTextField has height 56
        return field;
    }
}
