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

package bisq.desktop.components.controls;

import com.jfoenix.controls.JFXTextField;
import javafx.geometry.Insets;
import javafx.scene.control.Skin;

public class BisqTextField extends JFXTextField {

    public BisqTextField(String value) {
        super(value);
        setLabelFloat(true);
        createEnoughSpaceForPromptText();
    }

    public BisqTextField() {
        super();
        setLabelFloat(true);
        createEnoughSpaceForPromptText();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JFXTextFieldSkinBisqStyle<>(this, 0);
    }

    public void hide() {
        setVisible(false);
        setManaged(false);
    }

    public void show() {
        setVisible(true);
        setManaged(true);
    }

    private void createEnoughSpaceForPromptText() {
        setPadding(new Insets(20, 0, 0, 0));
    }
}
