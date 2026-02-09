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

package bisq.desktop.main.content.bisq_easy.history;

import bisq.desktop.common.view.Model;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Predicate;

@Getter
public class BisqEasyHistoryModel implements Model {
    private final ObservableList<BisqEasyTradeHistoryListItem> bisqEasyTradeHistoryListItems = FXCollections.observableArrayList();
    private final FilteredList<BisqEasyTradeHistoryListItem> filteredBisqEasyTradeHistoryListItems = new FilteredList<>(bisqEasyTradeHistoryListItems);
    private final SortedList<BisqEasyTradeHistoryListItem> sortedBisqEasyTradeHistoryListItems = new SortedList<>(filteredBisqEasyTradeHistoryListItems);

    private final Predicate<BisqEasyTradeHistoryListItem> bisqEasyTradeHistoryListItemsPredicate = item ->
            getSearchStringPredicate().test(item);
    @Setter
    private Predicate<BisqEasyTradeHistoryListItem> searchStringPredicate = item -> true;
}
