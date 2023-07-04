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

import bisq.desktop.components.controls.skins.BisqPopupSkin;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.Setter;

public class BisqPopup extends PopupControl {
    @Getter
    private final StackPane root = new StackPane();

    @Getter
    @Setter
    protected Node contentNode;

    @Setter
    protected Alignment alignment = Alignment.RIGHT;

    public BisqPopup() {
        super();
        getStyleClass().add("bisq-popup");
        setAutoHide(true);
    }

    public final void show(Node owner) {
        Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
        double anchorX = 0;
        if (alignment == Alignment.RIGHT) {
            setAnchorLocation(AnchorLocation.WINDOW_BOTTOM_RIGHT);
            anchorX = bounds.getMaxX();
        } else if (alignment == Alignment.LEFT) {
            setAnchorLocation(AnchorLocation.WINDOW_BOTTOM_LEFT);
            anchorX = bounds.getMinX();
        }
        super.show(owner, anchorX, bounds.getMinY());
    }

    public enum Alignment {
        LEFT, RIGHT
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new BisqPopupSkin(this);
    }
}
