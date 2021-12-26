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

package network.misq.desktop;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import network.misq.desktop.common.view.Model;

public class StageModel implements Model {
    final DoubleProperty minWidthProperty = new SimpleDoubleProperty(800);
    final DoubleProperty minHeightProperty = new SimpleDoubleProperty(600);
    final DoubleProperty prefWidthProperty = new SimpleDoubleProperty(1900);
    final DoubleProperty prefHeightProperty = new SimpleDoubleProperty(1000);
    final StringProperty titleProperty = new SimpleStringProperty("");

    public StageModel() {
    }

    public void setTitle(String appName) {
        titleProperty.set(appName);
    }
}
