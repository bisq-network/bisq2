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

package bisq.desktop.main.content.contacts_list;

import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.user.reputation.ReputationSource;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

import java.util.function.Predicate;

@Slf4j
@Getter
public class ContactsListModel implements Model {
    private final String tableInfoTitle = Res.get("contactsList.table.tableInfo.title");
    private final String tableInfoContent = Res.get("contactsList.table.tableInfo.content");

    private final ObservableList<ContactsListView.ListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<ContactsListView.ListItem> filteredList = new FilteredList<>(listItems);
    private final SortedList<ContactsListView.ListItem> sortedList = new SortedList<>(filteredList);
    private final BooleanProperty scoreChangeTrigger = new SimpleBooleanProperty();
    private final StringProperty filteredValueTitle = new SimpleStringProperty();
    private final BooleanProperty valueColumnVisible = new SimpleBooleanProperty();
    private final ObjectProperty<ReputationSource> selectedReputationSource = new SimpleObjectProperty<>();
    @Setter
    private Predicate<ContactsListView.ListItem> filterItemPredicate = e -> true;
    @Setter
    private Predicate<ContactsListView.ListItem> searchStringPredicate = e -> true;
    @Setter
    private boolean shouldShowLearnMorePopup;
}
