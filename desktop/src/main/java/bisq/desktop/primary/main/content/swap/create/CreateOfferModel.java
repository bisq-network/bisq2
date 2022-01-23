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

import bisq.offer.Direction;
import bisq.desktop.common.view.Model;
import bisq.desktop.primary.main.content.swap.create.components.OfferPreparationModel;
import bisq.offer.Offer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class CreateOfferModel implements Model {
    @Delegate
    private final OfferPreparationModel offerPreparationModel;

    @Getter
    private final ObjectProperty<Offer> offer = new SimpleObjectProperty<>();

    public CreateOfferModel(OfferPreparationModel offerPreparationModel) {
        this.offerPreparationModel = offerPreparationModel;
    }

    public void onViewAttached() {
        offerPreparationModel.setDirection(Direction.BUY);
    }

    public void onViewDetached() {
    }
}
