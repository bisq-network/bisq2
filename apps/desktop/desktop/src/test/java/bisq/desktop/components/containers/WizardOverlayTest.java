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

package bisq.desktop.components.containers;

import bisq.common.observable.Observable;
import bisq.desktop.common.Transitions;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.settings.SettingsService;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.testfx.util.WaitForAsyncUtils;

import static bisq.desktop.testutil.JavaFxToolkitExtension.runOnFxThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A wizard step can hide its overlay and show it again before the hide animations have finished. The review step of
 * the trade wizard does this when it is shown again after an offer was created, and the user publishes the next offer
 * at once (#5213).
 */
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class WizardOverlayTest {
    // Longer than the hide animations (300 ms)
    private static final long ANIMATIONS_DONE_MS = 1000;

    private StackPane content;
    private WizardOverlay overlay;
    private Stage stage;

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class, RETURNS_DEEP_STUBS);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(true));
        Transitions.setSettingsService(settingsService);

        runOnFxThread(() -> {
            content = new StackPane();
            StackPane root = new StackPane(content);
            overlay = new WizardOverlay(root).headline("headline").build();
            overlay.setVisible(false);
            root.getChildren().add(overlay);
            stage = new Stage();
            stage.setScene(new Scene(root, 400, 300));
            stage.show();
        });
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // Let running animations finish while the animation settings are still set
        Thread.sleep(ANIMATIONS_DONE_MS);
        runOnFxThread(() -> stage.close());
        Transitions.setSettingsService(null);
    }

    @Test
    void overlayShownAgainDuringHideAnimationStaysVisible() throws InterruptedException {
        runOnFxThread(() -> overlay.updateOverlayVisibility(content, true, keyEvent -> {
        }));
        Thread.sleep(ANIMATIONS_DONE_MS);

        runOnFxThread(() -> {
            overlay.updateOverlayVisibility(content, false, null);
            overlay.updateOverlayVisibility(content, true, keyEvent -> {
            });
        });
        Thread.sleep(ANIMATIONS_DONE_MS);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(overlay.isVisible()).isTrue();
        assertThat(overlay.getOpacity()).isEqualTo(1);
        assertThat(content.getEffect()).isInstanceOf(GaussianBlur.class);
    }
}
