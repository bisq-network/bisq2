package bisq.desktop.components.list_view;

import bisq.desktop.common.threading.UIScheduler;
import javafx.geometry.Orientation;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ListViewUtil {
    // We need to lookup VirtualScrollBar not ScrollBar, as ScrollBar would return any of the parents scrollbars.
    public static Optional<ScrollBar> findScrollbar(ListView<?> listView, Orientation orientation) {
        return listView.lookupAll("VirtualScrollBar").stream()
                .filter(node -> {
                    // VirtualScrollBar is in package `com.sun.javafx.scene.control`
                    // and cause an IllegalAccessError on Apple Silicon macs.
                    // VirtualScrollBar inherits from ScrollBar which is in the 
                    // accessible `javafx.scene.control package`.
                    // We still wrap it with an exception handler as it is not tested yet with 
                    // Apple Silicon macs.
                    // See https://github.com/bisq-network/bisq2/issues/1697
                    try {
                        return node instanceof ScrollBar;
                    } catch (IllegalAccessError e) {
                        log.warn("Access to VirtualScrollBar failed", e);
                        return false;
                    }
                })
                .map(node -> (ScrollBar) node)
                .filter(scrollBar -> scrollBar.getOrientation().equals(orientation))
                .findFirst();
    }

    public static CompletableFuture<Optional<ScrollBar>> findScrollbarAsync(ListView<?> listView, Orientation orientation, long timeout) {
        Optional<ScrollBar> scrollbar = findScrollbar(listView, orientation);
        if (scrollbar.isPresent()) {
            return CompletableFuture.completedFuture(scrollbar);
        }
        CompletableFuture<Optional<ScrollBar>> future = new CompletableFuture<>();
        future.orTimeout(timeout, TimeUnit.MILLISECONDS);

        delayedScrollbarLookup(listView, orientation, future);

        return future;
    }

    private static void delayedScrollbarLookup(ListView<?> listView, Orientation orientation, CompletableFuture<Optional<ScrollBar>> future) {
        UIScheduler.run(() -> {
            Optional<ScrollBar> scrollbar2 = findScrollbar(listView, orientation);
            if (scrollbar2.isPresent()) {
                future.complete(scrollbar2);
            } else if (!future.isDone()) {
                delayedScrollbarLookup(listView, orientation, future);
            }
        }).after(20);
    }
}
