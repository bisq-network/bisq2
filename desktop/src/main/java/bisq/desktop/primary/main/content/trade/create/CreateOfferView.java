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
import bisq.desktop.primary.main.content.trade.components.*;
import bisq.i18n.Res;
import bisq.offer.Offer;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateOfferView extends View<VBox, CreateOfferModel, CreateOfferController> {
    private final ChangeListener<Offer> offerListener;
    private final BisqButton createOfferButton;

    public CreateOfferView(CreateOfferModel model,
                           CreateOfferController controller,
                           MarketSelection.MarketSelectionView marketSelectionView,
                           DirectionSelection.DirectionView directionView,
                           AmountPriceGroup.AmountPriceView amountPriceView,
                           ProtocolSelection.ProtocolView protocolView,
                           AccountSelection.AccountView accountView) {
        super(new VBox(), model, controller);
        root.setSpacing(30);
        root.setPadding(new Insets(20, 20, 20, 0));

        amountPriceView.getRoot().setPadding(new Insets(0, 0, -5, 0));

        createOfferButton = new BisqButton(Res.offerbook.get("createOffer.button"));
        createOfferButton.setOnAction(e -> controller.onCreateOffer());

        //todo temp
        BisqTextArea offerSummary = new BisqTextArea();
        offerSummary.setVisible(false);

        BisqButton publishButton = new BisqButton(Res.offerbook.get("publishOffer.button"));
        publishButton.setOnAction(e -> controller.onPublishOffer());
        publishButton.setVisible(false);

        root.getChildren().addAll(
                marketSelectionView.getRoot(),
                directionView.getRoot(),
                amountPriceView.getRoot(),
                protocolView.getRoot(),
                accountView.getRoot(),
                createOfferButton,
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
    public void onViewAttached() {
        model.getOfferProperty().addListener(offerListener);
        createOfferButton.visibleProperty().bind(model.createOfferButtonVisibleProperty());
        createOfferButton.managedProperty().bind(model.createOfferButtonVisibleProperty());
    }

    @Override
    public void onViewDetached() {
        model.getOfferProperty().removeListener(offerListener);
        createOfferButton.visibleProperty().unbind();
        createOfferButton.managedProperty().unbind();
    }
}
