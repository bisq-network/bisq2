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
import javafx.collections.ObservableMap;
import javafx.scene.Node;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

    private final Observable<Boolean> useAnimations = new Observable<>(true);
    private VBox content;
    private Region firstStep;
    private Region secondStep;
    private Stage stage;

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class, RETURNS_DEEP_STUBS);
        when(settingsService.getUseAnimations()).thenReturn(useAnimations);
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
    void stepFadedInAgainDuringItsSlideOutStaysInContent() throws InterruptedException {
        runOnFxThread(() -> {
            // Next
            showStep(secondStep, firstStep, false);
            // The wizard is closed and opened again before the slide-out of the first step has finished. Its first
            // view change has no old view, so it fades the step in (TradeWizardView and the other wizards).
            content.getChildren().setAll(firstStep);
            Transitions.fadeIn(firstStep);
        });
        Thread.sleep(ANIMATIONS_DONE_MS);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(content.getChildren()).containsExactly(firstStep);
        assertThat(firstStep.getOpacity()).isEqualTo(1);
    }

    @Test
    void stepFadedInAgainDuringItsSlideOutIsMovedBackToItsPosition() throws InterruptedException {
        AtomicReference<Double> translateXAfterFadeIn = new AtomicReference<>();
        runOnFxThread(() -> {
            showStep(secondStep, firstStep, false);
            // As if the slide-out had already moved the first step part of the way
            firstStep.setTranslateX(-firstStep.getWidth() / 2);
            content.getChildren().setAll(firstStep);
            Transitions.fadeIn(firstStep);
            translateXAfterFadeIn.set(firstStep.getTranslateX());
        });
        Thread.sleep(ANIMATIONS_DONE_MS);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(translateXAfterFadeIn.get()).isEqualTo(0);
        assertThat(firstStep.getTranslateX()).isEqualTo(0);
        assertThat(firstStep.getProperties()).isEmpty();
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

    @Test
    void stepIsRemovedImmediatelyAfterSlideOutToTheLeftWithoutAnimations() {
        assertStepIsRemovedImmediatelyWithoutAnimations(false);
    }

    @Test
    void stepIsRemovedImmediatelyAfterSlideOutToTheRightWithoutAnimations() {
        assertStepIsRemovedImmediatelyWithoutAnimations(true);
    }

    private void assertStepIsRemovedImmediatelyWithoutAnimations(boolean slideOutRight) {
        useAnimations.set(false);
        AtomicReference<List<Node>> childrenAfterTransition = new AtomicReference<>();
        AtomicReference<ObservableMap<Object, Object>> propertiesOfStepOut = new AtomicReference<>();
        runOnFxThread(() -> {
            // Like OnboardingView and TacView, keep the old step in the pane while it slides out
            content.getChildren().add(secondStep);
            if (slideOutRight) {
                Transitions.transitRightOut(secondStep, firstStep);
            } else {
                Transitions.transitLeftOut(secondStep, firstStep);
            }
            // Read in the same FX task, so that no later FX event can have removed the step
            childrenAfterTransition.set(List.copyOf(content.getChildren()));
            propertiesOfStepOut.set(firstStep.getProperties());
        });

        assertThat(childrenAfterTransition.get()).containsExactly(secondStep);
        // No slide-out timeline is stored, so a later transition of the step has nothing to stop
        assertThat(propertiesOfStepOut.get()).isEmpty();
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
