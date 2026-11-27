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


package bisq.desktop.main.content.wallet.setup_wallet_wizard;

import bisq.common.observable.Observable;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.settings.SettingsService;
import bisq.wallet.WalletService;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

// The system close of the overlay window must close the wizard like its close button, and the first dismissal must
// win, so that a wallet open answered or a close requested while the overlay hides does not move the wizard on.
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class SetupWalletWizardWindowCloseTest {
    private final AtomicInteger overlayCloseHandlerCalls = new AtomicInteger();
    private MockedStatic<OverlayController> overlayController;
    private SetupWalletWizardController controller;
    private Stage stage;
    private StackPane container;

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);
        ServiceProvider serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getWalletService()).thenReturn(Optional.of(mock(WalletService.class)));
        JavaFxToolkitExtension.runOnFxThread(() -> {
            // Static mocks are bound to the thread which creates them, so this one lives on the JavaFX thread
            overlayController = mockStatic(OverlayController.class);
            OverlayController overlay = mock(OverlayController.class);
            when(overlay.getApplicationRoot()).thenReturn(new StackPane());
            overlayController.when(OverlayController::getInstance).thenReturn(overlay);

            controller = new SetupWalletWizardController(serviceProvider);
            stage = new Stage();
            container = new StackPane();
            stage.setScene(new Scene(container));
            // Like the overlay window, which hides itself on a close request
            stage.setOnCloseRequest(event -> overlayCloseHandlerCalls.incrementAndGet());
            container.getChildren().add(controller.getView().getRoot());
        });
    }

    @AfterEach
    void tearDown() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            container.getChildren().clear();
            overlayController.close();
        });
        Transitions.setSettingsService(null);
    }

    @Test
    void closeRequestOfTheWindowClosesTheWizard() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            requestWindowClose();

            overlayController.verify(OverlayController::hide);
        });

        assertThat(overlayCloseHandlerCalls).hasValue(0);
    }

    @Test
    void closeRequestWhileTheWizardHidesIsIgnored() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            requestWindowClose();
            requestWindowClose();

            overlayController.verify(OverlayController::hide, times(1));
        });

        assertThat(overlayCloseHandlerCalls).hasValue(0);
    }

    // The finish of the wizard closes the overlay with Wallet as the target. A close which arrives while the overlay
    // hides must not replace that target with Main.
    @Test
    void closeRequestsDuringTheFinishKeepItsTarget() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            controller.closeAndNavigateTo(NavigationTarget.WALLET);
            requestWindowClose();
            controller.onKeyPressed(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));

            overlayController.verify(() -> OverlayController.hide(any(Runnable.class)));
            overlayController.verify(OverlayController::hide, never());
        });

        assertThat(overlayCloseHandlerCalls).hasValue(0);
    }

    // A finish which arrives after a close must not replace the Main target.
    @Test
    void finishAfterACloseChangesNothing() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            requestWindowClose();
            controller.closeAndNavigateTo(NavigationTarget.WALLET);

            overlayController.verify(OverlayController::hide, times(1));
            overlayController.verify(() -> OverlayController.hide(any(Runnable.class)), never());
        });
    }

    @Test
    void nextWhileTheWizardHidesChangesNothing() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            requestWindowClose();
            controller.onNext();
        });

        assertThat(controller.getModel().getCurrentIndex().get()).isEqualTo(0);
    }

    @Test
    void backWhileTheWizardHidesChangesNothing() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            controller.onNext();
            requestWindowClose();
            controller.onBack();
        });

        assertThat(controller.getModel().getCurrentIndex().get()).isEqualTo(1);
    }

    private void requestWindowClose() {
        Event.fireEvent(stage, new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }
}
