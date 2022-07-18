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

import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqText extends Text {
    private ChangeListener<Parent> parentListener;

    public BisqText() {
        init();
    }

    public BisqText(String text) {
        super(text);
        init();
    }

    public BisqText(double x, double y, String text) {
        super(x, y, text);
        init();
    }

    public void init() {
        parentListener = (observable, oldValue, newValue) -> {
            log.error("Parent " + newValue);
            if (newValue instanceof Region) {
                //parentProperty().removeListener(parentListener);
                //  parentListener = null;
                onParentAsRegionAvailable((Region) newValue);
            }
        };
        parentProperty().addListener(parentListener);
    }

    private void onParentAsRegionAvailable(Region region) {
        ChangeListener<Number> widthListener = (observable, oldValue, newValue) -> {
            setWrappingWidth(newValue.doubleValue() - 30);
        };
        // region.widthProperty().addListener(new WeakReference<>(widthListener).get());
        region.widthProperty().addListener(widthListener);
    }
}