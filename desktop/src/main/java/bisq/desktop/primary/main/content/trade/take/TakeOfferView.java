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
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TakeOfferView extends View<VBox, TakeOfferModel, TakeOfferController> {
    private final BisqButton takeOfferButton;
    private final BisqLabel protocolLabel;

    public TakeOfferView(TakeOfferModel model,
                         TakeOfferController controller,
                         Pane directionSelection,
                         Pane amountPrice,
                         Pane settlement) {
        super(new VBox(), model, controller);
        root.setSpacing(30);
        root.setPadding(new Insets(20, 20, 20, 0));

        protocolLabel = new BisqLabel();
        protocolLabel.getStyleClass().add("titled-group-bg-label-active");

        amountPrice.setPadding(new Insets(0, 0, -5, 0));

        takeOfferButton = new BisqButton(Res.get("takeOffer.button"));
        takeOfferButton.getStyleClass().add("action-button");

        BisqButton cancelButton = new BisqButton(Res.get("cancel"));
        cancelButton.setOnAction(e -> controller.onCancel());

        root.getChildren().addAll(
                directionSelection,
                protocolLabel,
                amountPrice,
                settlement,
                Layout.hBoxWith(takeOfferButton, cancelButton));
    }

    @Override
    public void onViewAttached() {
        protocolLabel.setText(Res.get("takeOffer.protocol", Res.get(model.getSelectedProtocolType().name())));
        takeOfferButton.setOnAction(e -> controller.onTakeOffer());
    }

    @Override
    public void onViewDetached() {
        takeOfferButton.setOnAction(null);
    }
}
