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

import bisq.desktop.common.view.View;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateOfferView extends View<VBox, CreateOfferModel, CreateOfferController> {


    public CreateOfferView(CreateOfferModel model,
                           CreateOfferController controller,
                           MarketSelection.MarketSelectionView marketSelectionView,
                           DirectionSelection.AmountPriceView directionView,
                           AmountPriceGroup.AmountPriceView amountPriceView,
                           ProtocolSelection.ProtocolView protocolView,
                           SettlementSelection.SettlementView settlementView) {
        super(new VBox(), model, controller);
        root.setSpacing(30);
        root.setPadding(new Insets(20, 20, 20, 0));

        amountPriceView.getRoot().setPadding(new Insets(0, 0, -5, 0));
        root.getChildren().addAll(
                marketSelectionView.getRoot(),
                directionView.getRoot(),
                amountPriceView.getRoot(),
                protocolView.getRoot(),
                settlementView.getRoot());
    }
}
