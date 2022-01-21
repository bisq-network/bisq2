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

package bisq.desktop.primary.main.content.swap.create;

import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqLabel;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateOfferView extends View<VBox, CreateOfferModel, CreateOfferController> {


    public CreateOfferView(CreateOfferModel model,
                           CreateOfferController controller,
                           MonetaryInput.MonetaryView ask,
                           MonetaryInput.MonetaryView bid,
                           PriceInput.PriceView price,
                           ProtocolSelection.ProtocolSelectionView protocolSelectionView) {
        super(new VBox(), model, controller);

        //root.getStyleClass().add("content-pane");
        root.setPadding(new Insets(20, 20, 20, 0));

        Label amountHeadline = new BisqLabel("Select currencies, set amount and price");
        amountHeadline.getStyleClass().add("titled-group-bg-label-active");
        
        Label xLabel = new Label();
        Text xIcon = Icons.getIconForLabel(MaterialDesignIcon.CLOSE, "2em", xLabel);
        xIcon.getStyleClass().add("opaque-icon");
        xLabel.getStyleClass().add("opaque-icon-character");


        Label resultLabel = new Label("=");
        resultLabel.getStyleClass().add("opaque-icon-character");


        HBox firstRowHBox = new HBox();
        firstRowHBox.setSpacing(5);
        firstRowHBox.setAlignment(Pos.CENTER_LEFT);
        firstRowHBox.getChildren().addAll(ask.getRoot(), xLabel, price.getRoot(), resultLabel, bid.getRoot());
        VBox.setMargin(firstRowHBox, new Insets(0, 0, 30, 0));


      /*  Button button = new BisqButton("Continue");
        //button.setPadding(new Insets(0, 0, 50, 0));
        button.setOnAction(e -> {
        });*/
        Label protocolSelectionHeadline = new BisqLabel("Select trade protocol");
        protocolSelectionHeadline.getStyleClass().add("titled-group-bg-label-active");
        
        VBox protocolSelectionViewRoot = protocolSelectionView.getRoot();
        
       root.getChildren().addAll(amountHeadline, firstRowHBox, protocolSelectionHeadline, protocolSelectionViewRoot);
    }
}
