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

package bisq.desktop.common;

import bisq.common.observable.Observable;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.settings.SettingsService;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
 * The wizards (trade wizard, take offer, payment account creation, wallet wizards) replace the step view in their
 * content pane and slide the old step view out with {@link Transitions#transitLeftOut} or
 * {@link Transitions#transitRightOut}. A user who clicks Next and then Back before the slide-out has finished gets
 * the old step view back while it still slides out.
 */
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class TransitionsTransitOutTest {
    // Longer than the slide-in (150 ms delay + 600 ms) and the slide-out (150 ms)
    private static final long ANIMATIONS_DONE_MS = 1000;

    private VBox content;
    private Region firstStep;
    private Region secondStep;
    private Stage stage;

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class, RETURNS_DEEP_STUBS);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(true));
        Transitions.setSettingsService(settingsService);

        runOnFxThread(() -> {
            firstStep = new StackPane();
            secondStep = new StackPane();
            content = new VBox(firstStep);
            stage = new Stage();
            stage.setScene(new Scene(new StackPane(content), 400, 300));
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
    void stepShownAgainDuringItsSlideOutStaysInContent() throws InterruptedException {
        runOnFxThread(() -> {
            // Next
            showStep(secondStep, firstStep, false);
            // Back, before the slide-out of the first step has finished
            showStep(firstStep, secondStep, true);
        });
        Thread.sleep(ANIMATIONS_DONE_MS);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(content.getChildren()).containsExactly(firstStep);
        assertThat(firstStep.getOpacity()).isEqualTo(1);
    }

    @Test
    void stepShownAgainDuringItsSlideOutToTheRightStaysInContent() throws InterruptedException {
        runOnFxThread(() -> {
            showStep(secondStep, firstStep, false);
        });
        Thread.sleep(ANIMATIONS_DONE_MS);

        runOnFxThread(() -> {
            // Back
            showStep(firstStep, secondStep, true);
            // Next, before the slide-out of the second step has finished
            showStep(secondStep, firstStep, false);
        });
        Thread.sleep(ANIMATIONS_DONE_MS);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(content.getChildren()).containsExactly(secondStep);
        assertThat(secondStep.getOpacity()).isEqualTo(1);
    }

    @Test
    void stepWhichIsNotShownAgainIsRemovedAfterItsSlideOut() throws InterruptedException {
        runOnFxThread(() -> {
            // Like OnboardingView and TacView, keep the old step in the pane while it slides out
            content.getChildren().add(secondStep);
            Transitions.transitLeftOut(secondStep, firstStep);
        });
        Thread.sleep(ANIMATIONS_DONE_MS);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(content.getChildren()).containsExactly(secondStep);
    }

    // Same as the view change listener of TradeWizardView
    private void showStep(Region stepIn, Region stepOut, boolean animateRightOut) {
        content.getChildren().setAll(stepIn);
        if (animateRightOut) {
            Transitions.transitRightOut(stepIn, stepOut);
        } else {
            Transitions.transitLeftOut(stepIn, stepOut);
        }
    }
}
