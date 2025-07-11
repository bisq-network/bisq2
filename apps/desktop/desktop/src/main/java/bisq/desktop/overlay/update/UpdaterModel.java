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

package bisq.desktop.overlay.update;

import bisq.desktop.common.view.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
public class UpdaterModel implements Model {
    private final BooleanProperty downloadStarted = new SimpleBooleanProperty();
    private final BooleanProperty isLauncherUpdate = new SimpleBooleanProperty();
    private final BooleanProperty downloadAndVerifyCompleted = new SimpleBooleanProperty();
    private final BooleanProperty ignoreVersion = new SimpleBooleanProperty();
    private final BooleanProperty ignoreVersionSwitchVisible = new SimpleBooleanProperty();
    private final StringProperty headline = new SimpleStringProperty();
    private final StringProperty version = new SimpleStringProperty();
    private final StringProperty releaseNotes = new SimpleStringProperty();
    private final StringProperty furtherInfo = new SimpleStringProperty();
    private final StringProperty shutDownButtonText = new SimpleStringProperty();
    private final StringProperty verificationInfo = new SimpleStringProperty();
    private final StringProperty downloadUrl = new SimpleStringProperty();

    private final ObservableList<UpdaterView.ListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<UpdaterView.ListItem> filteredList = new FilteredList<>(listItems);
    private final SortedList<UpdaterView.ListItem> sortedList = new SortedList<>(filteredList);

    @Setter
    private boolean requireVersionForTrading;
    @Setter
    private Optional<String> minRequiredVersionForTrading = Optional.empty();
}