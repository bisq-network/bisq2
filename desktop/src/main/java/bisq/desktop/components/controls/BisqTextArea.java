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

import bisq.desktop.common.threading.UIThread;
import com.jfoenix.controls.JFXTextArea;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Skin;

import javax.annotation.Nullable;

public class BisqTextArea extends JFXTextArea {
    @Nullable
    private SimpleDoubleProperty adjustedHeight;
    @Nullable
    private ChangeListener<Number> scrollTopListener;

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JFXTextAreaSkinBisqStyle(this);
    }

    // TextArea does not support adjustment of it is height when text requires more lines.
    // It shows a scrollbar instead. This hack below somehow fixes that, but not perfect. 
    // Waiting for a component implementation which works as expected and replace it then.
    // As this is temporary we don't handle listener cleanups...
    public void autoAdjustHeight(double rowHeight) {
        double extra = 11;
        adjustedHeight = new SimpleDoubleProperty(extra + rowHeight);
        prefHeightProperty().bindBidirectional(adjustedHeight);
        minHeightProperty().bindBidirectional(adjustedHeight);
        if (scrollTopListener == null) {
            scrollTopListener = (observable, oldValue, newValue) -> {
                if (newValue.doubleValue() >= rowHeight) {
                    UIThread.runLater(() -> adjustedHeight.set(adjustedHeight.get() + newValue.doubleValue()));
                }
            };
            scrollTopProperty().addListener(scrollTopListener);
            UIThread.runLater(() -> adjustedHeight.set(adjustedHeight.get() + getScrollTop()));
        }
    }

    public void resetAutoAdjustedHeight() {
        if (adjustedHeight != null) {
            adjustedHeight.set(getMinHeight());
        }
    }

    public void releaseResources() {
        if (scrollTopListener != null) {
            prefHeightProperty().unbindBidirectional(adjustedHeight);
            minHeightProperty().unbindBidirectional(adjustedHeight);
            scrollTopProperty().removeListener(scrollTopListener);
            scrollTopListener = null;
        }
    }
}
