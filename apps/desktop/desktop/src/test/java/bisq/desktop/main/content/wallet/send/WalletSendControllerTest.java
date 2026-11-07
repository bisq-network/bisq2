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

package bisq.desktop.main.content.wallet.send;

import bisq.common.application.DevMode;
import bisq.common.observable.Observable;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.components.overlay.Overlay;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class WalletSendControllerTest {
    private static final String ADDRESS = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq";
    private static final String TX_ID = "69111c8de670d7a12b8c4db85c67485889b30335cdd3fd7f18924104e88e9fc3";

    private WalletService walletService;
    private CompletableFuture<String> sendResult;
    private WalletSendController controller;
    private Stage stage;
    private StackPane container;

    @BeforeEach
    void setUp() throws Exception {
        DevMode.setDevMode(false);
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);
        Overlay.settingsService = settingsService;
        walletService = mock(WalletService.class);
        when(walletService.isWalletEncrypted()).thenReturn(CompletableFuture.completedFuture(true));
        sendResult = new CompletableFuture<>();
        when(walletService.sendToAddress(any(), any(), anyLong())).thenReturn(sendResult);
        ServiceProvider serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getWalletService()).thenReturn(Optional.of(walletService));

        WaitForAsyncUtils.asyncFx(() -> {
            controller = new WalletSendController(serviceProvider);
            // The container stands in for the app's root: it stays in the scene while the overlay content comes and goes
            container = new StackPane(controller.getView().getRoot());
            stage = new Stage();
            stage.setScene(new Scene(container));
            stage.show();
            Overlay.primaryStageOwner = container;
        }).get(10, TimeUnit.SECONDS);
        WaitForAsyncUtils.waitForFxEvents();
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
    void aSecondClickWhileTheSendIsInFlightSendsNothing() {
        fillForm("  " + ADDRESS + " ", "0.5", "Wallet-Pa55!");

        onFx(() -> controller.onSend());
        onFx(() -> controller.onSend());

        verify(walletService, times(1)).sendToAddress(eq(Optional.of("Wallet-Pa55!")), eq(ADDRESS), eq(50_000_000L));
        assertThat(controller.getModel().getSendInProgress().get()).isTrue();
    }

    @Test
    void anAcceptedSendClearsTheFormAndShowsTheTxId() {
        fillForm(ADDRESS, "0.5", "Wallet-Pa55!");
        onFx(() -> controller.onSend());

        onFx(() -> sendResult.complete(TX_ID));

        WalletSendModel model = controller.getModel();
        assertThat(model.getSendInProgress().get()).isFalse();
        assertThat(model.getSentTxId().get()).isEqualTo(TX_ID);
        assertThat(model.getAddress().get()).isEmpty();
        assertThat(model.getAmount().get()).isEmpty();
        assertThat(model.getPassword().get()).isEmpty();

        // A send is ignored while the result is shown
        onFx(() -> controller.onSend());
        verify(walletService, times(1)).sendToAddress(any(), any(), anyLong());
    }

    // The overlay caches the controller, so a reopened screen must not show the previous send
    @Test
    void aReopenedScreenStartsEmpty() {
        fillForm(ADDRESS, "0.5", "Wallet-Pa55!");
        onFx(() -> controller.onSend());
        onFx(() -> sendResult.complete(TX_ID));

        // The overlay detaches the view by taking its root out of the scene and attaches it again on the next open
        onFx(() -> container.getChildren().clear());
        onFx(() -> container.getChildren().setAll(controller.getView().getRoot()));

        WalletSendModel model = controller.getModel();
        assertThat(model.getSentTxId().get()).isNull();
        assertThat(model.getAddress().get()).isEmpty();
        assertThat(model.getPassword().get()).isEmpty();

        fillForm(ADDRESS, "0.5", "Wallet-Pa55!");
        onFx(() -> controller.onSend());
        verify(walletService, times(2)).sendToAddress(any(), any(), anyLong());
    }

    // The user closed the overlay while the daemon worked; the result still has to reach them
    @Test
    void aResultArrivingAfterTheScreenWasLeftIsShownAsAPopup() {
        fillForm(ADDRESS, "0.5", "Wallet-Pa55!");
        onFx(() -> controller.onSend());
        onFx(() -> container.getChildren().clear());
        int windowsBefore = Window.getWindows().size();

        onFx(() -> sendResult.complete(TX_ID));

        assertThat(Window.getWindows()).hasSize(windowsBefore + 1);
        assertThat(controller.getModel().getSendInProgress().get()).isFalse();
        assertThat(controller.getModel().getSentTxId().get()).isNull();
    }

    @Test
    void aFailedSendShowsTheWalletErrorAndFreesTheForm() {
        fillForm(ADDRESS, "0.5", "Wallet-Pa55!");
        onFx(() -> controller.onSend());
        int windowsBefore = Window.getWindows().size();

        onFx(() -> sendResult.completeExceptionally(
                new WalletException(WalletException.Reason.WRONG_PASSWORD, "denied", Optional.empty(), null)));

        assertThat(Window.getWindows()).hasSize(windowsBefore + 1);
        WalletSendModel model = controller.getModel();
        assertThat(model.getSendInProgress().get()).isFalse();
        assertThat(model.getSentTxId().get()).isNull();
        assertThat(model.getAddress().get()).isEqualTo(ADDRESS);
        assertThat(model.getPassword().get()).isEqualTo("Wallet-Pa55!");
    }

    @Test
    void invalidInputNeverReachesTheDaemon() {
        fillForm("bc1-not-an-address", "0.5", "Wallet-Pa55!");
        onFx(() -> controller.onSend());

        fillForm(ADDRESS, "0", "Wallet-Pa55!");
        onFx(() -> controller.onSend());

        verify(walletService, never()).sendToAddress(any(), any(), anyLong());
        assertThat(controller.getModel().getSendInProgress().get()).isFalse();
    }

    private void fillForm(String address, String amount, String password) {
        onFx(() -> {
            controller.getModel().getAddress().set(address);
            controller.getModel().getAmount().set(amount);
            controller.getModel().getPassword().set(password);
        });
    }

    private static void onFx(Runnable task) {
        JavaFxToolkitExtension.runOnFxThread(task);
    }
}
