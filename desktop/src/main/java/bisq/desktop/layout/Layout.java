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

package bisq.desktop.layout;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

public class Layout {
    public static final Insets PADDING = new Insets(20);
    public static final double SPACING = 20;

    public static void pinToAnchorPane(Node node, int top, int right, int bottom, int left) {
        AnchorPane.setTopAnchor(node, (double) top);
        AnchorPane.setRightAnchor(node, (double) right);
        AnchorPane.setBottomAnchor(node, (double) bottom);
        AnchorPane.setLeftAnchor(node, (double) left);
    }
}