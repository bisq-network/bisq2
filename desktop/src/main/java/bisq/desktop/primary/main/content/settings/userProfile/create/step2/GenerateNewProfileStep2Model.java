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

package bisq.desktop.primary.main.content.settings.userProfile.create.step2;

import bisq.desktop.common.view.Model;
import bisq.desktop.primary.overlay.onboarding.profile.TempIdentity;
import bisq.identity.Identity;
import javafx.beans.property.*;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
public class GenerateNewProfileStep2Model implements Model {
    @Setter
    private Optional<TempIdentity> tempIdentity = Optional.empty();
    @Setter
    private Optional<Identity> pooledIdentity = Optional.empty();
    private final StringProperty nickName = new SimpleStringProperty();
    private final StringProperty profileId = new SimpleStringProperty();
    private final StringProperty terms = new SimpleStringProperty();
    private final StringProperty bio = new SimpleStringProperty();
    private final ObjectProperty<Image> roboHashImage = new SimpleObjectProperty<>();
    private final BooleanProperty createProfileButtonDisabled = new SimpleBooleanProperty();
    private final DoubleProperty createProfileProgress = new SimpleDoubleProperty();
    private final BooleanProperty isEditable = new SimpleBooleanProperty();
}