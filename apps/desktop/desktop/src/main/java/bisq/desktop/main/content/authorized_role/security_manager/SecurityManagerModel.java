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

package bisq.desktop.main.content.authorized_role.security_manager;

import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.desktop.common.view.Model;
import bisq.desktop.main.content.authorized_role.security_manager.SecurityManagerView.AlertListItem;
import bisq.desktop.main.content.authorized_role.security_manager.SecurityManagerView.BondedRoleListItem;
import bisq.desktop.main.content.authorized_role.security_manager.SecurityManagerView.DifficultyAdjustmentListItem;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class SecurityManagerModel implements Model {
    private final DoubleProperty difficultyAdjustmentFactor = new SimpleDoubleProperty();
    private final BooleanProperty difficultyAdjustmentFactorButtonDisabled = new SimpleBooleanProperty();
    private final ObservableList<DifficultyAdjustmentListItem> difficultyAdjustmentListItems = FXCollections.observableArrayList();

    private final ObjectProperty<AlertType> selectedAlertType = new SimpleObjectProperty<>();
    private final ObservableList<AlertType> alertTypes = FXCollections.observableArrayList();
    private final ObjectProperty<BondedRoleListItem> selectedBondedRoleListItem = new SimpleObjectProperty<>();
    private final ObservableList<BondedRoleListItem> bondedRoleListItems = FXCollections.observableArrayList();
    private final SortedList<BondedRoleListItem> bondedRoleSortedList = new SortedList<>(bondedRoleListItems);
    private final StringProperty actionButtonText = new SimpleStringProperty();
    private final BooleanProperty actionButtonDisabled = new SimpleBooleanProperty();
    private final StringProperty headline = new SimpleStringProperty();
    private final StringProperty message = new SimpleStringProperty();
    private final StringProperty minVersion = new SimpleStringProperty();
    private final BooleanProperty haltTrading = new SimpleBooleanProperty();
    private final BooleanProperty requireVersionForTrading = new SimpleBooleanProperty();
    private final BooleanProperty alertsVisible = new SimpleBooleanProperty();
    private final BooleanProperty bondedRoleSelectionVisible = new SimpleBooleanProperty();
    private final BooleanProperty bannedAccountDataVisible = new SimpleBooleanProperty();
    private final StringProperty bannedAccountData = new SimpleStringProperty();

    private final ObservableList<AlertListItem> alertListItems = FXCollections.observableArrayList();
    private final SortedList<AlertListItem> sortedAlertListItems = new SortedList<>(alertListItems);

    public SecurityManagerModel() {
    }
}
