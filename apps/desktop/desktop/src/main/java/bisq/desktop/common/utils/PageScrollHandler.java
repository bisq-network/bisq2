package bisq.desktop.common.utils;

import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.Subscription;

import java.util.Objects;
import java.util.Optional;

/**
 * PageScrollHandler
 * <p>
 * Utility that installs a key event handler on a JavaFX Control to perform page-wise
 * scrolling when the PAGE_UP and PAGE_DOWN keys are pressed. The handler locates the
 * control's vertical ScrollBar (if present) and adjusts its value by the ScrollBar's
 * block increment, mimicking a page click on the scrollbar trough. The key event is
 * consumed only when a scroll action is performed.
 * <p>
 * Typical usage:
 * - Instantiate with a JavaFX Control.
 * - Call {@link #subscribe()} to enable the handler.
 * - Call {@link #unsubscribe()} to remove the handler.
 * <p>
 * Note: methods that inspect or modify the scene graph (e.g. locating ScrollBars)
 * must be called on the JavaFX Application Thread.
 */
@Slf4j
public class PageScrollHandler implements Subscription {

    private final Control control;
    private final EventHandler<KeyEvent> keyEventHandler;

    public PageScrollHandler(Control control) {
        this.control = Objects.requireNonNull(control);
        this.keyEventHandler = createKeyEventHandler();
    }

    public static Optional<ScrollBar> findScrollBar(Control control, Orientation orientation) {
        return findScrollBar(control, orientation, "VirtualScrollBar")
                .or(() -> findScrollBar(control, orientation, ".scroll-bar"));
    }

    public static Optional<ScrollBar> findScrollBar(Control control, Orientation orientation, String selector) {
        if (control.getSkin() == null) {
            log.warn("Control has no skin; cannot find ScrollBar");
            return Optional.empty();
        }

        return control.lookupAll(selector).stream()
                .filter(node -> node instanceof ScrollBar sb && sb.getOrientation() == orientation)
                .map(ScrollBar.class::cast)
                .findFirst();
    }

    private EventHandler<KeyEvent> createKeyEventHandler() {
        return event -> {
            if (event.getCode() == KeyCode.PAGE_UP && blockDecrement()) {
                event.consume();
            } else if (event.getCode() == KeyCode.PAGE_DOWN && blockIncrement()) {
                event.consume();
            }
        };
    }

    public void subscribe() {
        unsubscribe();
        control.addEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler);
    }

    public void unsubscribe() {
        control.removeEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler);
    }

    protected boolean blockDecrement() {
        return findScrollBar(control, Orientation.VERTICAL)
                .map(this::adjustScrollBarDecrement)
                .orElse(false);
    }

    protected boolean blockIncrement() {
        return findScrollBar(control, Orientation.VERTICAL)
                .map(this::adjustScrollBarIncrement)
                .orElse(false);
    }

    private boolean adjustScrollBarDecrement(ScrollBar vbar) {
        return adjustScrollBar(vbar, -vbar.getBlockIncrement());
    }

    private boolean adjustScrollBarIncrement(ScrollBar vbar) {
        return adjustScrollBar(vbar, vbar.getBlockIncrement());
    }

    private boolean adjustScrollBar(ScrollBar vbar, double increment) {
        double oldValue = vbar.getValue();
        vbar.adjustValue(oldValue + increment);
        double newValue = vbar.getValue();
        log.debug("ScrollBar adjusted: {}; {} -> {}", vbar, oldValue, newValue);
        return oldValue != newValue;
    }
}