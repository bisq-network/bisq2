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

import bisq.desktop.skins.BisqPopupSkin;
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

    public BisqPopup() {
        super();
        root.getStyleClass().add("bisq-popup");
        setAutoHide(true);

        setOnShown(evt -> {
            Bounds bounds = root.getBoundsInParent();
            setAnchorX(getAnchorX() + bounds.getMinX() - contentNode.prefWidth(-1));
            setAnchorY(getAnchorY() - bounds.getMinY() - bounds.getMaxY());
        });
    }

    public final void show(Node owner) {
        Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
        super.show(owner, bounds.getMaxX(), bounds.getMinY() - 4);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new BisqPopupSkin(this);
    }
}
