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

import bisq.desktop.common.view.Model;
import bisq.desktop.components.controls.validator.TextMaxLengthValidator;
import bisq.i18n.Res;
import bisq.wallet.receive_address.ReceiveAddressEntry;
import bisq.wallet.receive_address.ReceiveAddressService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class WalletReceiveAddressModel implements Model {
    private final ObjectProperty<ReceiveAddressEntry> receiveAddressEntry = new SimpleObjectProperty<>();
    private final StringProperty receiveAddress = new SimpleStringProperty();
    private final StringProperty receiveAddressNote = new SimpleStringProperty();
    private final BooleanProperty isNewAddress = new SimpleBooleanProperty();
    private final BooleanProperty isAddressNoteEditable = new SimpleBooleanProperty();
    private final BooleanProperty shouldShowAddressNote = new SimpleBooleanProperty();
    private final StringProperty addressTextFieldDescription =  new SimpleStringProperty();

    private final TextMaxLengthValidator addressNoteMaxLengthValidator =
            new TextMaxLengthValidator(
                    Res.get("wallet.receive.note.maxLength",
                            ReceiveAddressService.RECEIVE_ADDRESS_ENTRY_NOTE_MAX_LENGTH),
                    ReceiveAddressService.RECEIVE_ADDRESS_ENTRY_NOTE_MAX_LENGTH);

    public WalletReceiveAddressModel() {
    }

    void reset() {
        receiveAddressEntry.set(null);
        receiveAddress.set(null);
        receiveAddressNote.set(null);
        isNewAddress.set(false);
        isAddressNoteEditable.set(false);
        shouldShowAddressNote.set(false);
        addressTextFieldDescription.set(null);
    }
}
