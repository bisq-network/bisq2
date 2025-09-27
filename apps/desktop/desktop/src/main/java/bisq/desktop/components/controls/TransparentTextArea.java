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

import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;

import java.util.function.Consumer;

public class TransparentTextArea extends TransparentTextField {
    private static final double DEFAULT_HEIGHT = 128;

    private double height;

    public TransparentTextArea(String description,
                               Consumer<String> onSaveClicked,
                               Runnable onCancelClicked) {
        super(description, true, onSaveClicked, onCancelClicked);

        ((TextArea) textInputControl).setWrapText(true);
        textInputControl.setContextMenu(new ContextMenu());
        setFixedHeight(DEFAULT_HEIGHT);

        getStyleClass().add("transparent-text-area");
    }

    @Override
    protected TextInputControl createTextInputControl() {
        return new TextArea();
    }

    @Override
    protected double getBgHeight() {
        return height;
    }

    @Override
    protected double getFieldLayoutY() {
        return 23; // 18 + 5 = 23;
    }

    public void setFixedHeight(double height) {
        this.height = height;
        textInputControl.setMinHeight(height - 32);
        textInputControl.setMaxHeight(height - 32);
        doLayout();
    }
}
