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

package bisq.desktop.primary;

import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import javafx.beans.property.*;
import javafx.scene.Parent;
import lombok.Getter;

@Getter
public class PrimaryStageModel implements Model {
    private final DoubleProperty minWidthProperty = new SimpleDoubleProperty(800);
    private final DoubleProperty minHeightProperty = new SimpleDoubleProperty(600);
    private final DoubleProperty prefWidthProperty = new SimpleDoubleProperty(1100);
    private final DoubleProperty prefHeightProperty = new SimpleDoubleProperty(1300);
    private final StringProperty titleProperty = new SimpleStringProperty("");

    @Getter
    protected final ObjectProperty<View<? extends Parent, ? extends Model, ? extends Controller>> view = new SimpleObjectProperty<>();

    @Getter
    protected NavigationTarget navigationTarget;

    public PrimaryStageModel() {
    }

    public void select(View<? extends Parent, ? extends Model, ? extends Controller> view) {
        this.view.set(view);
    }

    public void setTitle(String appName) {
        titleProperty.set(appName);
    }
}
