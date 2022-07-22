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

import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class MaterialTextArea extends MaterialTextField {
    private double height = 100;

    public MaterialTextArea() {
        this(null, null, null);
    }

    public MaterialTextArea(String description) {
        this(description, null, null);
    }

    public MaterialTextArea(String description, String prompt) {
        this(description, prompt, null);
    }

    public MaterialTextArea(@Nullable String description, @Nullable String prompt, @Nullable String help) {
        super(description, prompt, help);

        field.setMinHeight(height);
        field.setMaxHeight(height);
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
        return 18 + 5;
    }

    public void setFixedHeight(double height) {
        this.height = height;
        field.setMinHeight(height - 32);
        field.setMaxHeight(height - 32);
        doLayout();
    }
}