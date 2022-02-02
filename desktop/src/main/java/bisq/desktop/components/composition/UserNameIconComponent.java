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

package bisq.desktop.components.composition;

import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.robohash.RoboHash;
import bisq.identity.Identity;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

public class UserNameIconComponent extends HBox {
    private final BisqLabel label = new BisqLabel();

    public UserNameIconComponent() {
        setSpacing(0);
        label.setAlignment(Pos.BOTTOM_CENTER);
        label.getStyleClass().add("headline-label");
        HBox.setMargin(label, new Insets(28, 0, 0, 0));
        getChildren().add(label);
    }

    public UserNameIconComponent(Identity identity) {
        this();
        setIdentity(identity);
    }

    public void setIdentity(Identity identity) {
        label.setText(identity.domainId());
        Node icon = RoboHash.getSmall(identity.pubKeyHash());
        getChildren().add(0, icon);
    }
}