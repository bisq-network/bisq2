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

package bisq.desktop.components.containers;


import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import lombok.extern.slf4j.Slf4j;

// TODO (low prio) remove as not used beside in Overlay where is is likely also not used...
@Slf4j
public class BisqGridPane extends GridPane {
    public BisqGridPane() {
        setVgap(25);
        setHgap(5);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(150);
        ColumnConstraints col2 = new ColumnConstraints();
        getColumnConstraints().addAll(col1, col2);
    }
}