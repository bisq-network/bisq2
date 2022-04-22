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

package bisq.desktop.primary.main.content.multiSig.createOffer;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import bisq.offer.Offer;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateOfferView extends View<VBox, CreateOfferModel, CreateOfferController> {
    private final static double POPUP_PADDING = 66;
    private final ChangeListener<Offer> offerListener;
    private final Button createOfferButton;
    private final BisqTextArea offerSummary;

    public CreateOfferView(CreateOfferModel model,
                           CreateOfferController controller,
                           Pane marketSelection,
                           Pane direction,
                           Pane amountPrice,
                           Pane protocol,
                           Pane settlement) {
        super(new VBox(), model, controller);

        root.setSpacing(30);
        root.getStyleClass().add("bisq-darkest-bg");
        root.setPadding(new Insets(POPUP_PADDING, POPUP_PADDING, POPUP_PADDING, POPUP_PADDING));
        
        marketSelection.setMinWidth(280);

        Label headlineLabel = new Label(Res.get("createOffer"));
        headlineLabel.getStyleClass().add("bisq-content-headline-label");
        HBox headLineBox = Layout.hBoxWith(Spacer.fillHBox(), headlineLabel, Spacer.fillHBox());

        amountPrice.setPadding(new Insets(0, 0, -5, 0));

        createOfferButton = new Button(Res.get("createOffer.button"));
        createOfferButton.setDefaultButton(true);
        Button cancelButton = new Button(Res.get("cancel"));
        cancelButton.setOnAction(e -> controller.onCancel());

        //todo temp
        offerSummary = new BisqTextArea();
        offerSummary.setVisible(false);

        Button publishButton = new Button(Res.get("publishOffer.button"));
        publishButton.setOnAction(e -> controller.onPublishOffer());
        publishButton.setVisible(false);

        protocol.setMaxWidth(845);
        settlement.setMaxWidth(845);
        offerSummary.setMaxWidth(845);


        root.getChildren().addAll(
                headLineBox,
                marketSelection,
                direction,
                amountPrice,
                protocol,
                settlement,
                Layout.hBoxWith(createOfferButton, cancelButton),
                offerSummary,
                publishButton);

        offerListener = (observable, oldValue, newValue) -> {
            //todo show summary
            offerSummary.setVisible(true);
            offerSummary.setText(newValue.toString());

            publishButton.setVisible(true);
        };
    }

    @Override
    protected void onViewAttached() {
        model.getOfferProperty().addListener(offerListener);
        createOfferButton.setOnAction(e -> controller.onCreateOffer());
        createOfferButton.visibleProperty().bind(model.createOfferButtonVisibleProperty());
        createOfferButton.managedProperty().bind(model.createOfferButtonVisibleProperty());

        offerSummary.clear();
        root.requestFocus();
    }

    @Override
    protected void onViewDetached() {
        model.getOfferProperty().removeListener(offerListener);
        createOfferButton.setOnAction(null);
        createOfferButton.visibleProperty().unbind();
        createOfferButton.managedProperty().unbind();
    }
}
