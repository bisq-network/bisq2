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


package bisq.desktop.testutil;

import bisq.i18n.Res;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeoutException;

/**
 * Starts the JavaFX toolkit for tests which need the JavaFX application thread but no user input.
 * <p>
 * Unlike TestFX's {@code ApplicationExtension}, this extension creates no {@code FxRobot}. Such tests
 * therefore never send native mouse or keyboard events to the desktop session and do not need the
 * macOS Accessibility permission. Tests which drive input use {@link DesktopUiTest} instead.
 * <p>
 * The toolkit is started through {@link FxToolkit} so that these tests can share a JVM with tests which
 * use {@code ApplicationExtension}; starting it with {@code Platform.startup} would make TestFX's later
 * launch fail.
 */
public class JavaFxToolkitExtension implements BeforeAllCallback {
    private static final long FX_THREAD_TIMEOUT_MS = 10_000;

    @Override
    public void beforeAll(ExtensionContext context) throws TimeoutException {
        FxToolkit.registerPrimaryStage();
        Res.setAndApplyLanguageTag("en");
    }

    /**
     * Runs the task on the JavaFX application thread, waits for it and for the events it queued,
     * and rethrows an exception thrown by the task.
     */
    public static void runOnFxThread(Runnable task) {
        WaitForAsyncUtils.waitForAsyncFx(FX_THREAD_TIMEOUT_MS, task);
        WaitForAsyncUtils.waitForFxEvents();
    }
}
