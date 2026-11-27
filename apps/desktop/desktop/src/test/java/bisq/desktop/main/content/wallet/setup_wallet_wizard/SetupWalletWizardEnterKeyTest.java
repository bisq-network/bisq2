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
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.settings.SettingsService;
import bisq.wallet.WalletService;
import javafx.scene.Scene;
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
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

// The backup step hides the navigation buttons while the seed words load. Enter must not move past it then.
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class SetupWalletWizardEnterKeyTest {
    private static final int BACKUP_STEP = 2;

    private MockedStatic<OverlayController> overlayController;
    private SetupWalletWizardController controller;
    private StackPane container;

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);
        ServiceProvider serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getWalletService()).thenReturn(Optional.of(mock(WalletService.class)));
        JavaFxToolkitExtension.runOnFxThread(() -> {
            // The wizard handles keys only while it is shown, so it is attached to a window with the overlay mocked.
            // Static mocks are bound to the thread which creates them, so this one lives on the JavaFX thread.
            overlayController = mockStatic(OverlayController.class);
            OverlayController overlay = mock(OverlayController.class);
            when(overlay.getApplicationRoot()).thenReturn(new StackPane());
            overlayController.when(OverlayController::getInstance).thenReturn(overlay);

            controller = new SetupWalletWizardController(serviceProvider);
            Stage stage = new Stage();
            container = new StackPane();
            stage.setScene(new Scene(container));
            container.getChildren().add(controller.getView().getRoot());
            controller.getModel().getCurrentIndex().set(BACKUP_STEP);
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
    void enterDoesNotLeaveTheBackupStepWhileTheNavigationIsHidden() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            controller.getModel().getNextButtonVisible().set(false);
            controller.onKeyPressed(enter());
        });

        assertThat(controller.getModel().getCurrentIndex().get()).isEqualTo(BACKUP_STEP);
    }

    @Test
    void enterLeavesTheBackupStepOnceTheNavigationShows() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            controller.getModel().getNextButtonVisible().set(true);
            controller.onKeyPressed(enter());
        });

        assertThat(controller.getModel().getCurrentIndex().get()).isEqualTo(BACKUP_STEP + 1);
    }

    // A failed seed request sends the wizard back while the backup step still hides the navigation.
    @Test
    void goingBackShowsTheNavigationAgain() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            controller.getModel().getNextButtonVisible().set(false);
            controller.getModel().getBackButtonVisible().set(false);
            controller.onBack();
        });

        assertThat(controller.getModel().getCurrentIndex().get()).isEqualTo(BACKUP_STEP - 1);
        assertThat(controller.getModel().getNextButtonVisible().get()).isTrue();
        assertThat(controller.getModel().getBackButtonVisible().get()).isTrue();
    }

    private static KeyEvent enter() {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false);
    }
}
