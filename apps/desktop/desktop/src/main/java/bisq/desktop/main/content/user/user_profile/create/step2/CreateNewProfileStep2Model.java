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

package bisq.desktop.main.content.user.user_profile.create.step2;

import bisq.desktop.common.view.Model;
import bisq.security.pow.ProofOfWork;
import javafx.beans.property.*;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;

import java.security.KeyPair;

@Getter
public class CreateNewProfileStep2Model implements Model {
    static final double CAT_HASH_IMAGE_SIZE = 128;
    @Setter
    private KeyPair keyPair;
    @Setter
    private byte[] pubKeyHash;
    @Setter
    private ProofOfWork proofOfWork;
    private final StringProperty nickName = new SimpleStringProperty();
    private final StringProperty nym = new SimpleStringProperty();
    private final StringProperty terms = new SimpleStringProperty();
    private final StringProperty statement = new SimpleStringProperty();
    private final ObjectProperty<Image> catHashImage = new SimpleObjectProperty<>();
    private final BooleanProperty createProfileButtonDisabled = new SimpleBooleanProperty();
    private final DoubleProperty createProfileProgress = new SimpleDoubleProperty();
    private final BooleanProperty isEditable = new SimpleBooleanProperty();
    private final BooleanProperty saveButtonDisabled = new SimpleBooleanProperty();
}