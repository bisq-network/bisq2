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

package bisq.desktop.main.content.reputation.ranking;

import bisq.desktop.common.view.Model;
import bisq.desktop.components.table.RichTableView;
import bisq.user.reputation.ReputationSource;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ToggleGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
@Getter
public class ReputationRankingModel implements Model {
    private final ObservableList<ReputationRankingView.ListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<ReputationRankingView.ListItem> filteredList = new FilteredList<>(listItems);
    private final SortedList<ReputationRankingView.ListItem> sortedList = new SortedList<>(filteredList);
    private final BooleanProperty scoreChangeTrigger = new SimpleBooleanProperty();
    private final StringProperty filteredValueTitle = new SimpleStringProperty();
    private final BooleanProperty valueColumnVisible = new SimpleBooleanProperty();
    private final ObjectProperty<ReputationSource> selectedReputationSource = new SimpleObjectProperty<>();
    private final List<RichTableView.FilterMenuItem<ReputationRankingView.ListItem>> filterItems = new ArrayList<>();
    private final ToggleGroup filterMenuItemToggleGroup = new ToggleGroup();
    @Setter
    private Predicate<ReputationRankingView.ListItem> filterItemPredicate = e -> true;
    @Setter
    private Predicate<ReputationRankingView.ListItem> searchStringPredicate = e -> true;
}
