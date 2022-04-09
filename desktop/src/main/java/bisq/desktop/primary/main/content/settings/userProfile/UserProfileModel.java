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

package bisq.desktop.primary.main.content.settings.userProfile;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Model;
import bisq.identity.IdentityService;
import bisq.social.user.profile.UserProfile;
import javafx.beans.property.*;
import javafx.scene.Node;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Slf4j
@Getter
public class UserProfileModel implements Model {
    final IdentityService identityService;
    String keyId;
    final ObjectProperty<Node> roboHashNode = new SimpleObjectProperty<>();
    final ObjectProperty<UserProfile> userProfile = new SimpleObjectProperty<>();
    final StringProperty successText = new SimpleStringProperty();
    KeyPair keyPair;
    final BooleanProperty createUserProfileVisible = new SimpleBooleanProperty();
    final BooleanProperty channelAdminVisible = new SimpleBooleanProperty();

    public UserProfileModel(DefaultApplicationService applicationService) {
        identityService = applicationService.getIdentityService();
    }
}
