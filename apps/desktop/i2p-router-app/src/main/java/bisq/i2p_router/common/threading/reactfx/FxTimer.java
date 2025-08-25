package bisq.i2p_router.common.threading.reactfx;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import lombok.Getter;

/**
 * Provides factory methods for timers that are manipulated from and execute
 * their action on the JavaFX application thread.
 * <p>
 * Taken from:
 * <a href="https://github.com/TomasMikula/ReactFX/blob/537fffdbb2958a77dfbca08b712bb2192862e960/reactfx/src/main/java/org/reactfx/util/FxTimer.java">...</a>
 */
public class FxTimer {
    private final Duration actionTime;
    private final Timeline timeline;
    private final Runnable action;
    private long seq = 0;
    @Getter
    private long counter = 0;

    public FxTimer(long actionTime, long period, Runnable action, int cycles) {
        this.actionTime = Duration.millis(actionTime);
        this.timeline = new Timeline();
        this.action = action;

        timeline.getKeyFrames().add(new KeyFrame(this.actionTime)); // used as placeholder
        if (period != actionTime) {
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(period)));
        }

        timeline.setCycleCount(cycles);
    }

    public void restart() {
        stop();
        long expected = seq;
        timeline.getKeyFrames().set(0, new KeyFrame(actionTime, ae -> {
            if (seq == expected) {
                action.run();
                counter++;
            }
        }));
        timeline.play();
    }

    public void stop() {
        timeline.stop();
        ++seq;
    }
}
