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

package network.misq.desktop.main.content.createoffer.assetswap.review;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import network.misq.desktop.common.view.Model;

public class ReviewOfferModel implements Model {
    final StringProperty formattedAskAmount = new SimpleStringProperty();
    long askAmount;

    public void setAskValue(String value) {
        formattedAskAmount.set(value);
        askAmount = Long.parseLong(value) * 10000000;
    }

    public ReviewOfferModel() {
        formattedAskAmount.addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                askAmount = Long.parseLong(newValue) * 10000000;
                formattedAskAmount.set(String.valueOf(askAmount / 10000000));
            }
        });
    }
}
