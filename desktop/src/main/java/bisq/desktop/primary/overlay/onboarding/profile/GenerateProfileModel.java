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

package bisq.desktop.primary.overlay.onboarding.profile;

import bisq.desktop.common.view.Model;
import javafx.beans.property.*;
import javafx.scene.image.Image;
import lombok.Getter;

@Getter
public class GenerateProfileModel implements Model {
    private final ObjectProperty<TempIdentity> tempIdentity = new SimpleObjectProperty<>();
    private final ObjectProperty<Image> roboHashImage = new SimpleObjectProperty<>();
    private final StringProperty nymId = new SimpleStringProperty();
    private final BooleanProperty reGenerateButtonDisabled = new SimpleBooleanProperty();
    private final BooleanProperty roboHashIconVisible = new SimpleBooleanProperty();
    private final DoubleProperty powProgress = new SimpleDoubleProperty();
    private final StringProperty nickName = new SimpleStringProperty();
    private final BooleanProperty createProfileButtonDisabled = new SimpleBooleanProperty();
    private final DoubleProperty createProfileProgress = new SimpleDoubleProperty();
}