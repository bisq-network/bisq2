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

package bisq.desktop.main.content.mu_sig.my_offers;

import bisq.desktop.common.view.Model;
import bisq.desktop.main.content.mu_sig.MuSigOfferListItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

@Getter
public class MuSigMyOffersModel implements Model {
    @Setter
    private String numOffers;

    private final Set<String> muSigMyOffersIds = new HashSet<>();
    private final ObservableList<MuSigOfferListItem> muSigMyOffersListItems = FXCollections.observableArrayList();
    private final FilteredList<MuSigOfferListItem> filteredMuSigMyOffersListItems = new FilteredList<>(muSigMyOffersListItems);
    private final SortedList<MuSigOfferListItem> sortedMuSigMyOffersListItems = new SortedList<>(filteredMuSigMyOffersListItems);

    private final Predicate<MuSigOfferListItem> muSigMyOffersListItemsPredicate = item ->
            getMuSigMyOffersFilterPredicate().test(item);
    private final Predicate<MuSigOfferListItem> muSigMyOffersFilterPredicate = item -> true;
}
