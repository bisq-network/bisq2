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
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.backup.SetupWalletWizardBackupController;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.verify.SetupWalletWizardVerifyController;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.settings.SettingsService;
import bisq.wallet.WalletException;
import bisq.wallet.WalletService;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// The daemon answers on a gRPC thread, so the test thread, which is not the JavaFX thread, completes the request.
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class SetupWalletWizardSeedLoadFailureTest {
    private ServiceProvider serviceProvider;
    private CompletableFuture<List<String>> seedWords;
    private Stage stage;

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);
        Overlay.settingsService = settingsService;
        WalletService walletService = mock(WalletService.class);
        seedWords = new CompletableFuture<>();
        when(walletService.getSeedWords(any())).thenReturn(seedWords);
        serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getWalletService()).thenReturn(Optional.of(walletService));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (stage != null) {
            WaitForAsyncUtils.asyncFx(() -> {
                List.copyOf(Window.getWindows()).forEach(Window::hide);
                stage.close();
            }).get(10, TimeUnit.SECONDS);
            WaitForAsyncUtils.waitForFxEvents();
        }
        Overlay.primaryStageOwner = null;
        Overlay.settingsService = null;
        Transitions.setSettingsService(null);
    }

    @Test
    void theBackupStepShowsAPopupWhenTheSeedWordsCannotBeLoaded() {
        activate(this::backupController);
        int windowsBefore = Window.getWindows().size();

        failSeedWordRequest();

        assertThat(Window.getWindows()).hasSize(windowsBefore + 1);
    }

    @Test
    void theBackupStepShowsNoPopupAfterItWasLeft() {
        Controller controller = activate(this::backupController);
        onFx(controller::onDeactivate);
        int windowsBefore = Window.getWindows().size();

        failSeedWordRequest();

        assertThat(Window.getWindows()).hasSize(windowsBefore);
    }

    @Test
    void theVerifyStepShowsAPopupWhenTheSeedWordsCannotBeLoaded() {
        activate(this::verifyController);
        int windowsBefore = Window.getWindows().size();

        failSeedWordRequest();

        assertThat(Window.getWindows()).hasSize(windowsBefore + 1);
    }

    @Test
    void theVerifyStepShowsNoPopupAfterItWasLeft() {
        Controller controller = activate(this::verifyController);
        onFx(controller::onDeactivate);
        int windowsBefore = Window.getWindows().size();

        failSeedWordRequest();

        assertThat(Window.getWindows()).hasSize(windowsBefore);
    }

    private void failSeedWordRequest() {
        seedWords.completeExceptionally(
                new WalletException(WalletException.Reason.DAEMON_UNAVAILABLE, "unavailable", Optional.empty(), null));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private Controller backupController() {
        return new SetupWalletWizardBackupController(serviceProvider, visible -> {
        }, () -> {
        }, () -> "Wallet-Pa55!");
    }

    private Controller verifyController() {
        return new SetupWalletWizardVerifyController(serviceProvider, visible -> {
        }, target -> {
        }, () -> {
        }, () -> "Wallet-Pa55!");
    }

    // The view activates its controller when it gets a scene. The steps look up the owner of their popups two levels
    // above their own root, as they sit in the wizard.
    private Controller activate(Supplier<Controller> factory) {
        Controller[] controller = new Controller[1];
        onFx(() -> {
            controller[0] = factory.get();
            StackPane container = new StackPane(new StackPane(controller[0].getView().getRoot()));
            Overlay.primaryStageOwner = container;
            stage = new Stage();
            stage.setScene(new Scene(container, 800, 600));
            stage.show();
        });
        return controller[0];
    }

    private static void onFx(Runnable task) {
        JavaFxToolkitExtension.runOnFxThread(task);
    }
}
