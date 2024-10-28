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

package bisq.desktop.components.controls.skins;

import bisq.desktop.components.controls.BisqPopup;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import lombok.Getter;

@Getter
public class BisqPopupSkin implements Skin<BisqPopup> {
    private final BisqPopup skinnable;

    public BisqPopupSkin(final BisqPopup popup) {
        this.skinnable = popup;
        popup.getRoot().getChildren().add(popup.getContentNode());
        Bindings.bindContent(popup.getRoot().getStyleClass(), popup.getStyleClass());
    }

    @Override
    public Node getNode() {
        return skinnable.getRoot();
    }

    @Override
    public void dispose() {
    }
}
