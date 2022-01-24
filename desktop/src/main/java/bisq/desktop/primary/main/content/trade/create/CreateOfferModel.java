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

import bisq.desktop.common.view.Model;
import bisq.desktop.primary.main.content.trade.create.components.OfferPreparationModel;
import bisq.offer.Offer;
import bisq.account.protocol.SwapProtocolType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class CreateOfferModel implements Model {
    @Delegate
    private final OfferPreparationModel offerPreparationModel;

    @Getter
    private final ObjectProperty<Offer> offer = new SimpleObjectProperty<>();
    @Getter
    private final BooleanProperty createOfferButtonVisible = new SimpleBooleanProperty(true);
    private final ChangeListener<SwapProtocolType> selectedProtocolTypListener;

    public CreateOfferModel(OfferPreparationModel offerPreparationModel) {
        this.offerPreparationModel = offerPreparationModel;

        selectedProtocolTypListener = (observable, oldValue, newValue) -> createOfferButtonVisible.set(newValue != null);
    }

    public void onViewAttached() {
        offerPreparationModel.selectedProtocolTypeProperty().addListener(selectedProtocolTypListener);
        createOfferButtonVisible.set(offerPreparationModel.getSelectedProtocolType() != null);
    }

    public void onViewDetached() {
        offerPreparationModel.selectedProtocolTypeProperty().removeListener(selectedProtocolTypListener);
    }
}
