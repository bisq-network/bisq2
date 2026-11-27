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

package bisq.desktop.main.content.wallet.setup_wallet_wizard.protect;

import bisq.common.observable.Observable;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.settings.SettingsService;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class SetupWalletWizardProtectViewTest {
    private SetupWalletWizardProtectController controller;
    private Stage stage;
    private StackPane container;
    private TextInputControl passwordInput;
    private TextInputControl confirmPasswordInput;

    @BeforeEach
    void setUp() throws Exception {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);
        WaitForAsyncUtils.asyncFx(() -> {
            controller = new SetupWalletWizardProtectController(mock(ServiceProvider.class), () -> {
            }, visible -> {
            });
            container = new StackPane(controller.getView().getRoot());
            stage = new Stage();
            stage.setScene(new Scene(container, 800, 600));
            stage.show();
        }).get(10, TimeUnit.SECONDS);
        WaitForAsyncUtils.waitForFxEvents();
        List<TextInputControl> inputs = controller.getView().getRoot().lookupAll(".text-input").stream()
                .map(TextInputControl.class::cast)
                .toList();
        assertThat(inputs).hasSize(2);
        passwordInput = inputs.get(0);
        confirmPasswordInput = inputs.get(1);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (stage != null) {
            WaitForAsyncUtils.asyncFx(() -> stage.close()).get(10, TimeUnit.SECONDS);
            WaitForAsyncUtils.waitForFxEvents();
        }
        Transitions.setSettingsService(null);
    }

    @Test
    void keysDoNotChangeThePasswordFieldsWhileTheInputIsLocked() throws Exception {
        type(passwordInput, "ab");
        type(confirmPasswordInput, "ab");

        for (TextInputControl focusedAtTheLock : List.of(confirmPasswordInput, passwordInput)) {
            onFx(focusedAtTheLock::requestFocus);
            onFx(() -> controller.setInputLocked(true));
            awaitPulse();
            assertThat(passwordInput.isDisabled()).isTrue();
            assertThat(confirmPasswordInput.isDisabled()).isTrue();
            onFx(focusedAtTheLock::requestFocus);
            assertThat(stage.getScene().getFocusOwner()).isNotSameAs(focusedAtTheLock);

            fireAtFocusOwner(typed("c"));
            fireAtFocusOwner(pressed(KeyCode.BACK_SPACE, false));
            fireAtFocusOwner(pressed(KeyCode.DELETE, false));
            fireAtFocusOwner(pressed(KeyCode.Z, true));
            fireAtFocusOwner(pressed(KeyCode.X, true));

            assertThat(passwordInput.getText()).isEqualTo("ab");
            assertThat(confirmPasswordInput.getText()).isEqualTo("ab");
            assertThat(controller.getPassword()).isEqualTo("ab");
            assertThat(controller.getModel().getConfirmPassword().get()).isEqualTo("ab");
            onFx(() -> controller.setInputLocked(false));
        }

        type(passwordInput, "c");
        type(confirmPasswordInput, "c");
        assertThat(controller.getPassword()).isEqualTo("abc");
        assertThat(controller.getModel().getConfirmPassword().get()).isEqualTo("abc");
    }

    @Test
    void aDetachedViewNoLongerFollowsTheLock() {
        onFx(() -> container.getChildren().clear());

        onFx(() -> controller.setInputLocked(true));

        assertThat(passwordInput.isDisabled()).isFalse();
        assertThat(confirmPasswordInput.isDisabled()).isFalse();
    }

    private void type(TextInputControl input, String text) {
        onFx(() -> {
            input.requestFocus();
            input.end();
        });
        text.chars().forEach(character -> fireAtFocusOwner(typed(String.valueOf((char) character))));
    }

    private static KeyEvent typed(String character) {
        return new KeyEvent(KeyEvent.KEY_TYPED, character, "", KeyCode.UNDEFINED, false, false, false, false);
    }

    private static KeyEvent pressed(KeyCode keyCode, boolean withShortcut) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, false, withShortcut, false, false);
    }

    // As in the running app a key event goes to the node which owns the focus
    private void fireAtFocusOwner(KeyEvent keyEvent) {
        onFx(() -> {
            Scene scene = stage.getScene();
            Node focusOwner = scene.getFocusOwner();
            Event.fireEvent(focusOwner != null ? focusOwner : scene, keyEvent);
        });
    }

    // The scene takes the focus from a disabled node at its next pulse
    private void awaitPulse() throws InterruptedException {
        CountDownLatch pulsed = new CountDownLatch(1);
        Runnable listener = pulsed::countDown;
        onFx(() -> {
            stage.getScene().addPostLayoutPulseListener(listener);
            Platform.requestNextPulse();
        });
        assertThat(pulsed.await(10, TimeUnit.SECONDS)).isTrue();
        onFx(() -> stage.getScene().removePostLayoutPulseListener(listener));
    }

    private void onFx(Runnable runnable) {
        try {
            WaitForAsyncUtils.asyncFx(runnable).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        WaitForAsyncUtils.waitForFxEvents();
    }
}
