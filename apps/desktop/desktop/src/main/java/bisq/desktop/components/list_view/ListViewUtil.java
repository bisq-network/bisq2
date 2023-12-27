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
    public static Optional<ScrollBar> findScrollbar(ListView<?> listView, Orientation orientation) {
        return listView.lookupAll(".scroll-bar").stream()
                .filter(node -> node instanceof ScrollBar)
                .map(node -> (ScrollBar) node)
                .filter(scrollBar -> scrollBar.getOrientation().equals(orientation))
                .findAny();
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
