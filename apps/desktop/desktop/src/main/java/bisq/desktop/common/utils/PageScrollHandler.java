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

import bisq.desktop.components.list_view.ListViewUtil;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAction;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.Subscription;

import java.util.Objects;

/**
 * Handler to enable page-wise keyboard scrolling for a JavaFX ListView Control.
 * <p>
 * Utility that installs a key event handler on a JavaFX ListView to perform page-wise
 * scrolling when the PAGE_UP and PAGE_DOWN keys are pressed. The handler locates the
 * control's vertical ScrollBar (if present) and adjusts its value by the ScrollBar's
 * block increment, mimicking a page click on the scrollbar trough. The key event is
 * consumed only when a scroll action is performed.
 * </p>
 * <p>
 * Note: the handler is registered using {@code addEventFilter}, i.e. it runs in the
 * capturing phase (filter phase) while events travel from the root toward the target node.
 * This allows centralized preprocessing of PageUp/PageDown keys for the ListView before
 * the event reaches the target node.
 * </p>
 * <p>
 * To ensure that input fields keep their normal keyboard behavior, the handler
 * checks with {@link #isInlineEditorActive()} whether a descendant editable
 * {@link TextInputControl} is currently focused. If an editor is active, the filter does
 * not consume the event — the event will continue to the normal target/bubbling phase and
 * can be handled by the editor. If no editor is focused, the filter consumes the event
 * (via {@code event.consume()}) after a scroll occurred.
 * </p>
 * <p>
 * Typical usage:
 * <ul>
 *   <li>Instantiate with a JavaFX ListView Control.</li>
 *   <li>Call {@link #subscribe()} to enable the handler.</li>
 *   <li>Call {@link #unsubscribe()} to remove the handler.</li>
 * </ul>
 * </p>
 * <p>
 * Note: methods that inspect or modify the scene graph (e.g. locating ScrollBars)
 * must be called on the JavaFX Application Thread.
 * </p>
 */
@Slf4j
public class PageScrollHandler implements Subscription {

    private final ListView<?> listView;
    private final EventHandler<KeyEvent> keyEventFilter;

    public PageScrollHandler(ListView<?> listView) {
        this.listView = Objects.requireNonNull(listView);
        this.keyEventFilter = createKeyEventFilter();
    }

    private EventHandler<KeyEvent> createKeyEventFilter() {
        return event -> {
            if (event.getCode() == KeyCode.PAGE_UP) {
                if (!isInlineEditorActive()) {
                    if (scrollPageUp()) {
                        event.consume();
                    }
                }
            } else if (event.getCode() == KeyCode.PAGE_DOWN) {
                if (!isInlineEditorActive()) {
                    if (scrollPageDown()) {
                        event.consume();
                    }
                }
            }
        };
    }

    public void subscribe() {
        unsubscribe();
        listView.addEventFilter(KeyEvent.KEY_PRESSED, keyEventFilter);
    }

    public void unsubscribe() {
        listView.removeEventFilter(KeyEvent.KEY_PRESSED, keyEventFilter);
    }

    public boolean scrollPageUp() {
        return ListViewUtil.findScrollbar(listView, Orientation.VERTICAL)
                .map(this::adjustScrollBarBlockDecrement)
                .orElse(false);
    }

    public boolean scrollPageDown() {
        return ListViewUtil.findScrollbar(listView, Orientation.VERTICAL)
                .map(this::adjustScrollBarBlockIncrement)
                .orElse(false);
    }

    private boolean adjustScrollBarBlockDecrement(ScrollBar vbar) {
        return adjustScrollBar(vbar, AccessibleAction.BLOCK_DECREMENT);
    }

    private boolean adjustScrollBarBlockIncrement(ScrollBar vbar) {
        return adjustScrollBar(vbar, AccessibleAction.BLOCK_INCREMENT);
    }

    private boolean adjustScrollBar(ScrollBar vbar, AccessibleAction action) {
        double oldValue = vbar.getValue();
        vbar.executeAccessibleAction(action);
        return oldValue != vbar.getValue();
    }

    private boolean isInlineEditorActive() {
        Scene scene = listView.getScene();
        if (scene == null) {
            return false;
        }
        Node focusOwner = scene.getFocusOwner();
        return isEditable(focusOwner) && isChildOfList(focusOwner);
    }

    private boolean isEditable(Node focusOwner) {
        return focusOwner instanceof TextInputControl && ((TextInputControl) focusOwner).isEditable();
    }

    private boolean isChildOfList(Node node) {
        while (node != null) {
            if (node == listView) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }
}