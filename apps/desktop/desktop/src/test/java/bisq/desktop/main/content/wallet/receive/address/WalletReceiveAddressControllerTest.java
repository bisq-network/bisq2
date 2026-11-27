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

package bisq.desktop.main.content.wallet.receive.address;

import bisq.common.observable.Observable;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import bisq.wallet.WalletService;
import bisq.wallet.receive_address.ReceiveAddressService;
import bisq.wallet.receive_address.ReceiveAddressEntry;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The daemon hands out an address again once enough addresses are unused, so "create new address" can return an
// address which already has a note.
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class WalletReceiveAddressControllerTest {
    private WalletService walletService;
    private WalletReceiveAddressController controller;
    private Stage stage;

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);
        walletService = mock(WalletService.class);
        when(walletService.getUnusedAddress())
                .thenReturn(CompletableFuture.completedFuture(new ReceiveAddressEntry("bcrt1qfirst")));
    }

    @AfterEach
    void tearDown() {
        if (stage != null) {
            onFx(stage::close);
        }
        Transitions.setSettingsService(null);
    }

    @Test
    void theAddressShownAtOpeningCanGetANote() {
        show();

        assertThat(addressLabel().getText()).isEqualTo("bcrt1qfirst");
        assertThat(noteInput().isEditable()).isTrue();
    }

    @Test
    void theAddressShownAtOpeningKeepsItsNote() {
        when(walletService.getUnusedAddress()).thenReturn(CompletableFuture.completedFuture(
                new ReceiveAddressEntry("bcrt1qfirst", 1L, Optional.of("rent"))));

        show();

        assertThat(noteInput().getText()).isEqualTo("rent");
        assertThat(noteInput().isEditable()).isFalse();
    }

    @Test
    void anAddressHandedOutAgainIsShownWithItsNote() {
        when(walletService.createReceiveAddress()).thenReturn(CompletableFuture.completedFuture(
                new ReceiveAddressEntry("bcrt1qcycled", 1L, Optional.of("rent"))));
        show();

        onFx(controller::onCreateNewReceiveAddress);

        assertThat(addressLabel().getText()).isEqualTo("bcrt1qcycled");
        assertThat(noteInput().getText()).isEqualTo("rent");
        assertThat(noteInput().isEditable()).isFalse();
        assertThat(descriptionLabel().getText()).isEqualTo(Res.get("wallet.receive.unusedAddress"));
    }

    @Test
    void anAddressWithoutANoteCanGetOne() {
        when(walletService.createReceiveAddress())
                .thenReturn(CompletableFuture.completedFuture(new ReceiveAddressEntry("bcrt1qnext")));
        show();

        onFx(controller::onCreateNewReceiveAddress);

        assertThat(addressLabel().getText()).isEqualTo("bcrt1qnext");
        assertThat(descriptionLabel().getText()).isEqualTo(Res.get("wallet.receive.unusedAddress"));
        assertThat(noteInput().getText()).isNullOrEmpty();
        assertThat(noteInput().isEditable()).isTrue();
    }

    @Test
    void anInvalidNoteLeavesNoErrorOnTheNextAddress() {
        when(walletService.createReceiveAddress()).thenReturn(CompletableFuture.completedFuture(
                new ReceiveAddressEntry("bcrt1qcycled", 1L, Optional.of("rent"))));
        show();
        enterInvalidNote();

        onFx(controller::onCreateNewReceiveAddress);

        assertThat(noteInputBox().isValidProperty().get()).isTrue();
    }

    @Test
    void anInvalidNoteLeavesNoErrorWhenTheSameAddressComesAgain() {
        when(walletService.createReceiveAddress())
                .thenReturn(CompletableFuture.completedFuture(new ReceiveAddressEntry("bcrt1qfirst")));
        show();
        enterInvalidNote();

        onFx(controller::onCreateNewReceiveAddress);

        assertThat(noteInputBox().isValidProperty().get()).isTrue();
    }

    @Test
    void aNoteIsSavedThroughTheSaveButton() {
        when(walletService.updateReceiveAddress(any(), any())).thenReturn(true);
        show();

        onFx(() -> noteInput().setText("rent"));
        assertThat(saveButton().isVisible()).isTrue();
        onFx(() -> saveButton().fire());

        verify(walletService).updateReceiveAddress(argThat(entry -> entry.getAddress().equals("bcrt1qfirst")),
                eq(Optional.of("rent")));
        assertThat(noteInput().isEditable()).isFalse();
    }

    // The daemon can hand out the opening address again. Its reply can wait for the JavaFX thread while the user saves a
    // note, and still carries the entry without that note.
    @Test
    void aNoteSavedWhileAReplyWaitsStaysAfterTheReplyIsShown() {
        Map<String, ReceiveAddressEntry> store = new HashMap<>();
        store.put("bcrt1qfirst", new ReceiveAddressEntry("bcrt1qfirst"));
        when(walletService.findReceiveAddressEntry(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.<String>getArgument(0))));
        when(walletService.updateReceiveAddress(any(), any())).thenAnswer(invocation -> {
            ReceiveAddressEntry entry = invocation.getArgument(0);
            Optional<String> note = invocation.getArgument(1);
            ReceiveAddressEntry stored = store.get(entry.getAddress());
            if (stored.getNote().isPresent()) {
                return false;
            }
            store.put(entry.getAddress(), new ReceiveAddressEntry(entry.getAddress(), stored.getCreatedAt(), note));
            return true;
        });
        CompletableFuture<ReceiveAddressEntry> waitingReply = new CompletableFuture<>();
        when(walletService.createReceiveAddress()).thenReturn(waitingReply);
        show();
        onFx(controller::onCreateNewReceiveAddress);

        onFx(() -> noteInput().setText("rent"));
        onFx(() -> saveButton().fire());
        waitingReply.complete(new ReceiveAddressEntry("bcrt1qfirst"));
        onFx(() -> {
        });

        assertThat(addressLabel().getText()).isEqualTo("bcrt1qfirst");
        assertThat(noteInput().getText()).isEqualTo("rent");
        assertThat(noteInput().isEditable()).isFalse();
    }

    private void enterInvalidNote() {
        onFx(() -> {
            noteInput().setText("x".repeat(ReceiveAddressService.RECEIVE_ADDRESS_ENTRY_NOTE_MAX_LENGTH + 1));
            noteInputBox().validate();
        });
        assertThat(noteInputBox().isValidProperty().get()).isFalse();
    }

    private AddressNoteInputBox noteInputBox() {
        return controller.getView().getRoot().lookupAll("*").stream()
                .filter(AddressNoteInputBox.class::isInstance)
                .map(AddressNoteInputBox.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private void show() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getWalletService()).thenReturn(Optional.of(walletService));
        onFx(() -> {
            controller = new WalletReceiveAddressController(serviceProvider, visible -> {
            }, () -> {
            });
            stage = new Stage();
            stage.setScene(new Scene(controller.getView().getRoot(), 800, 600));
            stage.show();
        });
    }

    private Button saveButton() {
        HBox noteRow = (HBox) noteInputBox().getParent();
        return (Button) noteRow.getChildren().get(1);
    }

    private Label addressLabel() {
        return (Label) controller.getView().getRoot().lookup(".address-bar .label");
    }

    private Label descriptionLabel() {
        return (Label) controller.getView().getRoot().lookup(".address-description");
    }

    private TextInputControl noteInput() {
        return (TextInputControl) controller.getView().getRoot().lookup(".text-input");
    }

    private static void onFx(Runnable task) {
        JavaFxToolkitExtension.runOnFxThread(task);
    }
}
