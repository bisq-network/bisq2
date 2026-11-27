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
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.settings.SettingsService;
import bisq.wallet.WalletService;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The wallet keeps the password which the daemon accepted in the password step. The later steps use that password,
// and an open answer which arrives after the wizard was closed changes nothing.
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class SetupWalletWizardPasswordTest {
    private static final int PROTECT_STEP = 1;
    private static final String ACCEPTED_PASSWORD = "Wallet-Pa55!";
    private static final String OTHER_PASSWORD = "Other-Pa55!";

    private final CompletableFuture<Void> openReply = new CompletableFuture<>();
    private MockedStatic<OverlayController> overlayController;
    private WalletService walletService;
    private SetupWalletWizardController controller;
    private StackPane container;

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);
        walletService = mock(WalletService.class);
        when(walletService.openOrCreateWallet(anyString())).thenReturn(openReply);
        when(walletService.getSeedWords(anyString())).thenReturn(new CompletableFuture<>());
        ServiceProvider serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getWalletService()).thenReturn(Optional.of(walletService));
        JavaFxToolkitExtension.runOnFxThread(() -> {
            // Static mocks are bound to the thread which creates them, so this one lives on the JavaFX thread
            overlayController = mockStatic(OverlayController.class);
            OverlayController overlay = mock(OverlayController.class);
            when(overlay.getApplicationRoot()).thenReturn(new StackPane());
            overlayController.when(OverlayController::getInstance).thenReturn(overlay);

            controller = new SetupWalletWizardController(serviceProvider);
            Stage stage = new Stage();
            container = new StackPane();
            stage.setScene(new Scene(container));
            container.getChildren().add(controller.getView().getRoot());
            controller.onNext();
        });
        assertThat(controller.getModel().getCurrentIndex().get()).isEqualTo(PROTECT_STEP);
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
    void laterStepsUseTheAcceptedPasswordAndTheFieldsKeepIt() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            enterPassword(ACCEPTED_PASSWORD);
            controller.onNext();
        });
        openReply.complete(null);
        JavaFxToolkitExtension.runOnFxThread(() -> {
            controller.onBack();
            assertThat(passwordFields()).hasSize(2).allMatch(MaterialTextField::isDisabled);

            enterPassword(OTHER_PASSWORD);
            controller.onNext();
        });

        assertThat(controller.getModel().getCurrentIndex().get()).isEqualTo(PROTECT_STEP + 1);
        verify(walletService, times(1)).openOrCreateWallet(anyString());
        verify(walletService, times(2)).getSeedWords(ACCEPTED_PASSWORD);
        verify(walletService, never()).getSeedWords(OTHER_PASSWORD);
    }

    @Test
    void anOpenAnsweredAfterTheWizardClosedChangesNothing() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            enterPassword(ACCEPTED_PASSWORD);
            controller.onNext();
            controller.onClose();
        });
        openReply.complete(null);
        JavaFxToolkitExtension.runOnFxThread(() -> {
        });

        assertThat(controller.getModel().getCurrentIndex().get()).isEqualTo(PROTECT_STEP);
        verify(walletService, never()).getSeedWords(anyString());
    }

    private void enterPassword(String password) {
        passwordFields().forEach(field -> field.setText(password));
    }

    private List<MaterialTextField> passwordFields() {
        return controller.getView().getRoot().lookupAll("*").stream()
                .filter(MaterialTextField.class::isInstance)
                .map(MaterialTextField.class::cast)
                .toList();
    }
}
