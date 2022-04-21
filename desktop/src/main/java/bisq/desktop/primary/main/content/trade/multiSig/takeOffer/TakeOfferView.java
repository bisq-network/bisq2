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

package bisq.desktop.primary.main.content.trade.multiSig.takeOffer;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TakeOfferView extends View<VBox, TakeOfferModel, TakeOfferController> {    private final static double MARGIN = 66;
    private final Button takeOfferButton;
    private final Label protocolLabel;

    public TakeOfferView(TakeOfferModel model,
                         TakeOfferController controller,
                         Pane directionSelection,
                         Pane amountPrice,
                         Pane settlement) {
        super(new VBox(), model, controller);

        root.setSpacing(30);
        root.setPadding(new Insets(MARGIN, MARGIN, MARGIN, MARGIN));
        root.getStyleClass().add("bisq-darkest-bg");

        Label headlineLabel = new Label(Res.get("trade.takeOffer"));
        headlineLabel.getStyleClass().add("bisq-content-headline-label");
        HBox headLineBox = Layout.hBoxWith(Spacer.fillHBox(), headlineLabel, Spacer.fillHBox());
        VBox.setMargin(headLineBox, new Insets(-27, 0, 0, 0));

        
        protocolLabel = new Label();
        protocolLabel.getStyleClass().add("titled-group-bg-label-active");

        amountPrice.setPadding(new Insets(0, 0, -5, 0));

        takeOfferButton = new Button(Res.get("takeOffer.button"));
        takeOfferButton.setDefaultButton(true);

        Button cancelButton = new Button(Res.get("cancel"));
        cancelButton.setOnAction(e -> controller.onCancel());

        root.getChildren().addAll(
                headLineBox,
                directionSelection,
                protocolLabel,
                amountPrice,
                settlement,
                Layout.hBoxWith(takeOfferButton, cancelButton));
    }

    @Override
    protected void onViewAttached() {
        protocolLabel.setText(Res.get("takeOffer.protocol", Res.get(model.getSelectedProtocolType().name())));
        takeOfferButton.setOnAction(e -> controller.onTakeOffer());
    }

    @Override
    protected void onViewDetached() {
        takeOfferButton.setOnAction(null);
    }
}
