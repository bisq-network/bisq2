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

package bisq.desktop_ui_harness_app;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class DesktopUiHarnessStageLocator {
    private final long stageTimeoutMs;
    private final long fxTimeoutMs;

    DesktopUiHarnessStageLocator(long stageTimeoutMs, long fxTimeoutMs) {
        this.stageTimeoutMs = stageTimeoutMs;
        this.fxTimeoutMs = fxTimeoutMs;
    }

    Stage awaitStage() throws Exception {
        long deadline = System.currentTimeMillis() + stageTimeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                Stage stage = callOnFxThread(this::findReadyStage);
                if (stage != null) {
                    return stage;
                }
            } catch (IllegalStateException | TimeoutException ignored) {
                // JavaFX toolkit or event queue not ready yet.
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Timed out waiting for desktop stage.");
    }

    @Nullable
    private Stage findReadyStage() {
        List<Stage> showingStages = Window.getWindows().stream()
                .filter(window -> window instanceof Stage)
                .map(window -> (Stage) window)
                .filter(Stage::isShowing)
                .filter(stage -> stage.getScene() != null)
                .toList();
        if (showingStages.isEmpty()) {
            return null;
        }
        return showingStages.stream()
                .filter(stage -> stage.getOwner() == null)
                .findFirst()
                .orElse(showingStages.getFirst());
    }

    private <T> T callOnFxThread(Callable<T> callable) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return callable.call();
        }
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(fxTimeoutMs, TimeUnit.MILLISECONDS);
    }
}
