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

package bisq.desktop.main.content.support.resources;

import bisq.desktop.common.view.Model;
import bisq.desktop.components.controls.validator.DirectoryPathValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.i18n.Res;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ResourcesModel implements Model {
    private final StringProperty backupLocation = new SimpleStringProperty();
    private final BooleanProperty backupButtonDefault = new SimpleBooleanProperty();
    private final BooleanProperty backupButtonDisabled = new SimpleBooleanProperty();
    private final ValidatorBase directoryPathValidator = new DirectoryPathValidator(
            Res.get("support.resources.backup.location.invalid"));

    private final ObservableList<ResourcesView.BackupSnapshotStoreItem> backupSnapshotStoreItems = FXCollections.observableArrayList();
    private final SortedList<ResourcesView.BackupSnapshotStoreItem> sortedBackupSnapshotStoreItems = new SortedList<>(backupSnapshotStoreItems);
}
