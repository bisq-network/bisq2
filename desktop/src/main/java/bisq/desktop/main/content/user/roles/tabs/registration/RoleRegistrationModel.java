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

package bisq.desktop.main.content.user.roles.tabs.registration;

import bisq.desktop.common.view.Model;
import bisq.user.identity.UserIdentity;
import bisq.user.role.RoleType;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class RoleRegistrationModel implements Model {
    private final RoleType roleType;
    private final ObjectProperty<UserIdentity> selectedChatUserIdentity = new SimpleObjectProperty<>();
    private final StringProperty profileId = new SimpleStringProperty();
    private final StringProperty bondUserName = new SimpleStringProperty();
    private final BooleanProperty requestRegistrationButtonDisabled = new SimpleBooleanProperty();

    public RoleRegistrationModel(RoleType roleType) {
        this.roleType = roleType;
    }
}
