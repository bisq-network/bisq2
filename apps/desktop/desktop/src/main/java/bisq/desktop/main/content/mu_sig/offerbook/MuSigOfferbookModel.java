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

package bisq.desktop.main.content.mu_sig.offerbook;

import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.offer.Direction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

@Slf4j
@Getter
public abstract class MuSigOfferbookModel implements Model {
    private final Direction direction;
    private final ObservableList<Market> markets = FXCollections.observableArrayList();
    private final ObjectProperty<Market> selectedMarket = new SimpleObjectProperty<>();
    private final Comparator<MuSigOfferListItem> priceComparator;
    private final String maker;
    private final String createOfferButtonText;
    private final String takeOfferButtonText;
    private final Set<String> offerIds = new HashSet<>();
    private final ObservableList<MuSigOfferListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<MuSigOfferListItem> filteredList = new FilteredList<>(listItems);
    private final SortedList<MuSigOfferListItem> sortedList = new SortedList<>(filteredList);

    @Setter
    private Predicate<MuSigOfferListItem> searchPredicate = item -> true;
    private final Predicate<MuSigOfferListItem> directionPredicate = item -> item.getOffer().getDirection().mirror().equals(getDirection());
    @Setter
    protected Predicate<MuSigOfferListItem> marketPredicate = item -> true;

    private final StringProperty priceTableHeader = new SimpleStringProperty();

    private final StringProperty amountToReceive = new SimpleStringProperty();
    private final StringProperty amountToSend = new SimpleStringProperty();

    public MuSigOfferbookModel(Direction direction) {
        this.direction = direction;
        priceComparator = direction.isBuy()
                ? Comparator.comparingLong(MuSigOfferListItem::getPriceAsLong)
                : Comparator.comparingLong(MuSigOfferListItem::getPriceAsLong).reversed();

        maker = direction.isSell()
                ? Res.get("muSig.offerbook.table.header.maker.buyer").toUpperCase(Locale.ROOT)
                : Res.get("muSig.offerbook.table.header.maker.seller").toUpperCase(Locale.ROOT);
        createOfferButtonText = direction.isSell()
                ? Res.get("muSig.offerbook.createOffer.sell").toUpperCase(Locale.ROOT)
                : Res.get("muSig.offerbook.createOffer.buy").toUpperCase(Locale.ROOT);

        takeOfferButtonText = direction.isSell()
                ? Res.get("muSig.offerbook.table.cell.intent.sell").toUpperCase(Locale.ROOT)
                : Res.get("muSig.offerbook.table.cell.intent.buy").toUpperCase(Locale.ROOT);
    }
}
