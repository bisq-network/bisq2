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

import bisq.desktop.common.view.Model;
import bisq.user.identity.UserIdentity;
import bisq.user.reputation.ReputationScore;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class UserProfileModel implements Model {
    private final ObjectProperty<UserIdentity> selectedUserIdentity = new SimpleObjectProperty<>();
    private final ObservableList<UserIdentity> userIdentities = FXCollections.observableArrayList();
    private final StringProperty nickName = new SimpleStringProperty();
    private final StringProperty nymId = new SimpleStringProperty();
    private final StringProperty profileId = new SimpleStringProperty();
    private final ObjectProperty<Image> roboHash = new SimpleObjectProperty<>();
    private final StringProperty statement = new SimpleStringProperty("");
    private final StringProperty terms = new SimpleStringProperty("");
    private final StringProperty reputationScoreValue = new SimpleStringProperty();
    private final ObjectProperty<ReputationScore> reputationScore = new SimpleObjectProperty<>();
    private final StringProperty profileAge = new SimpleStringProperty();
    private final BooleanProperty saveButtonDisabled = new SimpleBooleanProperty();
}
