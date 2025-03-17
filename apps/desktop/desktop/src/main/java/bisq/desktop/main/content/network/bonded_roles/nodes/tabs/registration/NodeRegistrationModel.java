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

package bisq.desktop.main.content.network.bonded_roles.nodes.tabs.registration;

import bisq.bonded_roles.BondedRoleType;
import bisq.desktop.main.content.network.bonded_roles.tabs.registration.BondedRolesRegistrationModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class NodeRegistrationModel extends BondedRolesRegistrationModel {
    private final StringProperty addressInfoJson = new SimpleStringProperty();
    private final StringProperty pubKey = new SimpleStringProperty();
    private final StringProperty privKey = new SimpleStringProperty();
    private final BooleanProperty showKeyPair = new SimpleBooleanProperty();
    private final BooleanProperty importButtonVisible = new SimpleBooleanProperty();
    protected final BooleanProperty jsonValid = new SimpleBooleanProperty();

    public NodeRegistrationModel(BondedRoleType bondedRoleType) {
        super(bondedRoleType);
    }
}
