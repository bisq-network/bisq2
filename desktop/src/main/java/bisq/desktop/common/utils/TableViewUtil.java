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

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableView;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TableViewUtil {
    public static Optional<ScrollBar> findScrollbar(TableView<?> tableView, Orientation orientation) {
        return tableView.lookupAll(".scroll-bar").stream()
                .filter(node -> node instanceof ScrollBar)
                .map(node -> (ScrollBar) node)
                .filter(scrollBar -> scrollBar.getOrientation().equals(orientation))
                .filter(Node::isVisible)
                .findAny();
    }
}