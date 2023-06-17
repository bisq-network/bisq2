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

package bisq.desktop.common.utils;

import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;

import javax.annotation.Nullable;

public class Layout {
    public static final double SPACING = 20;

    public static void pinToAnchorPane(Node node,
                                       @Nullable Integer top,
                                       @Nullable Integer right,
                                       @Nullable Integer bottom,
                                       @Nullable Integer left) {
        if (top != null) AnchorPane.setTopAnchor(node, (double) top);
        if (right != null) AnchorPane.setRightAnchor(node, (double) right);
        if (bottom != null) AnchorPane.setBottomAnchor(node, (double) bottom);
        if (left != null) AnchorPane.setLeftAnchor(node, (double) left);
    }

    public static void pinToAnchorPane(Node node,
                                       @Nullable Double top,
                                       @Nullable Double right,
                                       @Nullable Double bottom,
                                       @Nullable Double left) {
        if (top != null) AnchorPane.setTopAnchor(node, top);
        if (right != null) AnchorPane.setRightAnchor(node, right);
        if (bottom != null) AnchorPane.setBottomAnchor(node, bottom);
        if (left != null) AnchorPane.setLeftAnchor(node, left);
    }


    public static Region hLine() {
        Region separator = new Region();
        separator.setMinHeight(1);
        separator.getStyleClass().add("h-line");
        return separator;
    }

    public static Region vLine() {
        Region separator = new Region();
        separator.setMaxWidth(1);
        separator.getStyleClass().add("v-line");
        return separator;
    }

    public static void addStyleClass(Node node, String className) {
        if (!node.getStyleClass().contains(className)) {
            node.getStyleClass().add(className);
        }
    }

    public static void removeStyleClass(Node node, String className) {
        node.getStyleClass().remove(className);
    }

    public static void toggleStyleClass(Node node, String className, boolean isPresent) {
        if (isPresent) {
            addStyleClass(node, className);
        } else {
            removeStyleClass(node, className);
        }
    }

    public static void chooseStyleClass(Node node, String firstClass, String secondClass, boolean firstClassSelected) {
        toggleStyleClass(node, firstClass, firstClassSelected);
        toggleStyleClass(node, secondClass, !firstClassSelected);
    }
}