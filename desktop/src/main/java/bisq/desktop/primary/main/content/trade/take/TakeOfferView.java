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

package bisq.desktop.primary.main.content.trade.take;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.layout.Layout;
import bisq.desktop.primary.main.content.trade.components.AccountSelection;
import bisq.desktop.primary.main.content.trade.components.AmountPriceGroup;
import bisq.desktop.primary.main.content.trade.components.DirectionSelection;
import bisq.i18n.Res;
import bisq.offer.Offer;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TakeOfferView extends View<VBox, TakeOfferModel, TakeOfferController> {
    private final ChangeListener<Offer> offerListener;
    private final BisqButton takeOfferButton;
    private final BisqLabel protocolLabel;

    public TakeOfferView(TakeOfferModel model,
                         TakeOfferController controller,
                         DirectionSelection.DirectionView directionView,
                         AmountPriceGroup.AmountPriceView amountPriceView,
                         AccountSelection.AccountView accountView) {
        super(new VBox(), model, controller);
        root.setSpacing(30);
        root.setPadding(new Insets(20, 20, 20, 0));

        protocolLabel = new BisqLabel();
        protocolLabel.getStyleClass().add("titled-group-bg-label-active");

        amountPriceView.getRoot().setPadding(new Insets(0, 0, -5, 0));

        takeOfferButton = new BisqButton(Res.offerbook.get("takeOffer.button"));
        takeOfferButton.setOnAction(e -> controller.onTakeOffer());

        BisqButton cancelButton = new BisqButton(Res.common.get("cancel"));
        cancelButton.setOnAction(e -> controller.onCancel());

        //todo temp
        BisqTextArea offerSummary = new BisqTextArea();
        offerSummary.setVisible(false);

        BisqButton publishButton = new BisqButton(Res.offerbook.get("publishOffer.button"));
        publishButton.setOnAction(e -> controller.onReview());
        publishButton.setVisible(false);

        root.getChildren().addAll(
                directionView.getRoot(),
                protocolLabel,
                amountPriceView.getRoot(),
                accountView.getRoot(),
                Layout.hBoxWith(takeOfferButton, cancelButton),
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
        protocolLabel.setText(Res.offerbook.get("takeOffer.protocol", Res.offerbook.get(model.selectedProtocol.name())));
    }

    @Override
    public void onViewDetached() {
    }

}
