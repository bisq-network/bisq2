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
import bisq.identity.Identity;
import bisq.security.pow.ProofOfWork;
import javafx.beans.property.*;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
public class GenerateProfileModel implements Model {
    @Setter
    private Optional<KeyPairAndId> keyPairAndId = Optional.empty();
    @Setter
    private Optional<Identity> pooledIdentity = Optional.empty();
    @Setter
    private Optional<ProofOfWork> proofOfWork = Optional.empty();
    @Setter
    private Optional<byte[]> pubKeyHash = Optional.empty();

    private final StringProperty nickName = new SimpleStringProperty();
    private final StringProperty nym = new SimpleStringProperty();
    private final ObjectProperty<Image> roboHashImage = new SimpleObjectProperty<>();
    private final BooleanProperty reGenerateButtonDisabled = new SimpleBooleanProperty();
    private final BooleanProperty roboHashIconVisible = new SimpleBooleanProperty();
    private final DoubleProperty powProgress = new SimpleDoubleProperty();
    private final BooleanProperty createProfileButtonDisabled = new SimpleBooleanProperty();
    private final DoubleProperty createProfileProgress = new SimpleDoubleProperty();
}