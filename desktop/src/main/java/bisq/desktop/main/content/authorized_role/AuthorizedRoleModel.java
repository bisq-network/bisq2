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

package bisq.desktop.main.content.authorized_role;

import bisq.bonded_roles.BondedRoleType;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.List;

@Getter
public class AuthorizedRoleModel extends TabModel {
    private final List<BondedRoleType> bondedRoleTypes;
    private final ObservableList<BondedRoleType> authorizedBondedRoles = FXCollections.observableArrayList();

    public AuthorizedRoleModel(List<BondedRoleType> bondedRoleTypes) {
        this.bondedRoleTypes = bondedRoleTypes;
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return authorizedBondedRoles.isEmpty() ?
                NavigationTarget.NONE :
                NavigationTarget.valueOf(authorizedBondedRoles.get(0).name());
    }
}
