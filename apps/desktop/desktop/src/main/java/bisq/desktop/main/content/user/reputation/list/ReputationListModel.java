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

package bisq.desktop.main.content.user.reputation.list;

import bisq.desktop.common.view.Model;
import bisq.desktop.components.table.StandardTable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ToggleGroup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class ReputationListModel implements Model {
    private final ObservableList<ReputationListView.ListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<ReputationListView.ListItem> filteredList = new FilteredList<>(listItems);
    private final SortedList<ReputationListView.ListItem> sortedList = new SortedList<>(filteredList);
    private final StringProperty userProfileIdOfScoreUpdate = new SimpleStringProperty();
    private final StringProperty filteredValueTitle = new SimpleStringProperty();
    private final BooleanProperty valueColumnVisible = new SimpleBooleanProperty();
    private final List<StandardTable.FilterMenuItem<ReputationListView.ListItem>> filterItems = new ArrayList<>();
    private final ToggleGroup filterMenuItemToggleGroup = new ToggleGroup();
}
