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

package bisq.desktop.main.content.user.profile_card.offers;

import bisq.desktop.common.view.Model;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ProfileCardOffersModel implements Model {
    // Extractor on the formatted percentage: it is set together with the sortable
    // percentage, so a market price that resolves a previously empty percentage emits a
    // list-update event and the SortedList re-sorts the row to its new position.
    private final ObservableList<ProfileCardOfferListItem> offerbookListItems =
            FXCollections.observableArrayList(item -> new Observable[]{item.getFormattedPercentagePrice()});
}
