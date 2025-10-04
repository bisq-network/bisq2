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

package bisq.desktop.main.content.user.user_profile;

import bisq.desktop.common.view.Model;
import bisq.desktop.components.controls.validator.TextMaxLengthValidator;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentity;
import bisq.user.reputation.ReputationScore;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static bisq.user.profile.UserProfile.MAX_LENGTH_STATEMENT;
import static bisq.user.profile.UserProfile.MAX_LENGTH_TERMS;

@Slf4j
@Getter
public class UserProfileModel implements Model {
    static final double CAT_HASH_IMAGE_SIZE = 125;

    private final ObjectProperty<UserIdentity> selectedUserIdentity = new SimpleObjectProperty<>();
    private final ObservableList<UserIdentity> userIdentities = FXCollections.observableArrayList();
    private final StringProperty nickName = new SimpleStringProperty();
    private final StringProperty nymId = new SimpleStringProperty();
    private final StringProperty profileId = new SimpleStringProperty();
    private final ObjectProperty<Image> catHashImage = new SimpleObjectProperty<>();
    private final StringProperty statement = new SimpleStringProperty("");
    private final StringProperty terms = new SimpleStringProperty("");
    private final StringProperty reputationScoreValue = new SimpleStringProperty();
    private final ObjectProperty<ReputationScore> reputationScore = new SimpleObjectProperty<>();
    private final StringProperty profileAge = new SimpleStringProperty();
    private final StringProperty livenessState = new SimpleStringProperty();
    private final BooleanProperty useDeleteTooltip = new SimpleBooleanProperty();

    private final TextMaxLengthValidator termsMaxLengthValidator =
            new TextMaxLengthValidator(
                    Res.get("user.userProfile.terms.tooLong", MAX_LENGTH_TERMS),
                    MAX_LENGTH_TERMS);
    private final TextMaxLengthValidator statementMaxLengthValidator =
            new TextMaxLengthValidator(
                    Res.get("user.userProfile.statement.tooLong", MAX_LENGTH_STATEMENT),
                    MAX_LENGTH_STATEMENT);
    private final String statementPrompt = Res.get("user.userProfile.statement.prompt");
    private final String termsPrompt = Res.get("user.userProfile.terms.prompt");
}
