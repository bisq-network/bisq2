package bisq.desktop.components.list_view;

import bisq.desktop.common.threading.UIScheduler;
import com.sun.javafx.scene.control.VirtualScrollBar;
import javafx.geometry.Orientation;
import javafx.scene.control.ListView;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ListViewUtil {
    // We need to lookup VirtualScrollBar not ScrollBar, as ScrollBar would return any of the parents scrollbars.
    public static Optional<VirtualScrollBar> findScrollbar(ListView<?> listView, Orientation orientation) {
        return listView.lookupAll("VirtualScrollBar").stream()
                .filter(node -> node instanceof VirtualScrollBar)
                .map(node -> (VirtualScrollBar) node)
                .filter(scrollBar -> scrollBar.getOrientation().equals(orientation))
                .findFirst();
    }

    public static CompletableFuture<Optional<VirtualScrollBar>> findScrollbarAsync(ListView<?> listView, Orientation orientation, long timeout) {
        Optional<VirtualScrollBar> scrollbar = findScrollbar(listView, orientation);
        if (scrollbar.isPresent()) {
            return CompletableFuture.completedFuture(scrollbar);
        }
        CompletableFuture<Optional<VirtualScrollBar>> future = new CompletableFuture<>();
        future.orTimeout(timeout, TimeUnit.MILLISECONDS);

        delayedScrollbarLookup(listView, orientation, future);

        return future;
    }

    private static void delayedScrollbarLookup(ListView<?> listView, Orientation orientation, CompletableFuture<Optional<VirtualScrollBar>> future) {
        UIScheduler.run(() -> {
            Optional<VirtualScrollBar> scrollbar2 = findScrollbar(listView, orientation);
            if (scrollbar2.isPresent()) {
                future.complete(scrollbar2);
            } else if (!future.isDone()) {
                delayedScrollbarLookup(listView, orientation, future);
            }
        }).after(20);
    }
}
