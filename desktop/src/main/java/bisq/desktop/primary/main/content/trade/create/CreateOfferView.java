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

package bisq.desktop.primary.main.content.trade.create;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import bisq.offer.Offer;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateOfferView extends View<ScrollPane, CreateOfferModel, CreateOfferController> {
    private final ChangeListener<Offer> offerListener;
    private final BisqButton createOfferButton;
    private final BisqTextArea offerSummary;

    public CreateOfferView(CreateOfferModel model,
                           CreateOfferController controller,
                           Pane marketSelection,
                           Pane direction,
                           Pane amountPrice,
                           Pane protocol,
                           Pane settlement) {
        super(new ScrollPane(), model, controller);

        // Place content within a VBox, within a ScrollPane, to show scrollbars if window size is too small
        VBox vBox = new VBox();
        vBox.setSpacing(30);
        root.setContent(vBox);
        root.setFitToWidth(true);
        vBox.setPadding(new Insets(40, 0, 0, 0));
       
        vBox.getStyleClass().add("bisq-content-bg");

        amountPrice.setPadding(new Insets(0, 0, -5, 0));

        createOfferButton = new BisqButton(Res.get("createOffer.button"));
        createOfferButton.getStyleClass().add("action-button");
        BisqButton cancelButton = new BisqButton(Res.get("cancel"));
        cancelButton.setOnAction(e -> controller.onCancel());

        //todo temp
        offerSummary = new BisqTextArea();
        offerSummary.setVisible(false);

        BisqButton publishButton = new BisqButton(Res.get("publishOffer.button"));
        publishButton.setOnAction(e -> controller.onPublishOffer());
        publishButton.setVisible(false);

        protocol.setMaxWidth(845);
        settlement.setMaxWidth(845);
        offerSummary.setMaxWidth(845);
        
        vBox.getChildren().addAll(
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
