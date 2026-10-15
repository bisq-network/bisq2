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

package bisq.desktop.overlay.unreadable_store_files;

import bisq.desktop.common.view.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;

@Getter
public class UnreadableStoreFilesModel implements Model {
    private final ObservableList<UnreadableStoreFilesView.ListItem> listItems = FXCollections.observableArrayList();
    private final SortedList<UnreadableStoreFilesView.ListItem> sortedList = new SortedList<>(listItems);
    private final StringProperty directory = new SimpleStringProperty();
    private final BooleanProperty directoryExists = new SimpleBooleanProperty();
}
