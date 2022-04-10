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

import javafx.scene.Node;
import javafx.scene.control.Button;


public class BisqButton extends Button {

    public BisqButton() {
        super();
    }

    public BisqButton(String text) {
        super(text);
    }

    public BisqButton(String text, Node graphic) {
        super(text, graphic);
    }

 /*   @Override
    protected Skin<?> createDefaultSkin() {
        return new AutoTooltipButtonSkin(this);
    }*/

    public void setFixWidth(double value) {
        setMinWidth(value);
        setMaxWidth(value);
    }

    public void setActionButton(boolean value) {
        super.setDefaultButton(value);
        getStyleClass().add("action-button");
    }
/*
    private static class AutoTooltipButtonSkin extends JFXButtonSkin {
        public AutoTooltipButtonSkin(JFXButton button) {
            super(button);
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            super.layoutChildren(x, y, w, h);
            showTooltipIfTruncated(this, getSkinnable());
        }
    }*/
}
