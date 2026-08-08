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

package bisq.desktop.main.content.wallet.receive.qrcode;

import bisq.desktop.common.view.Model;
import bisq.desktop.components.controls.validator.BitcoinUriAmountValidator;
import bisq.desktop.components.controls.validator.TextMaxLengthValidator;
import bisq.i18n.Res;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class WalletAddressQrCodeModel implements Model {
    private static final int LABEL_MAX_LENGTH = 20;
    private final int qrCodeSize;

    @Setter
    private String receiveAddress;
    private final ObjectProperty<Image> qrCodeImage = new SimpleObjectProperty<>();
    private final StringProperty btcUri = new SimpleStringProperty();
    private final StringProperty amount = new SimpleStringProperty();
    private final BooleanProperty isAmountValid = new SimpleBooleanProperty(false);
    private final StringProperty label = new SimpleStringProperty();

    private final BitcoinUriAmountValidator bitcoinUriAmountValidator = new BitcoinUriAmountValidator();
    private final TextMaxLengthValidator labelMaxLengthValidator =
            new TextMaxLengthValidator(Res.get("wallet.receive.name.maxLength", LABEL_MAX_LENGTH), LABEL_MAX_LENGTH);

    public WalletAddressQrCodeModel(int qrCodeSize) {
        this.qrCodeSize = qrCodeSize;
    }

    void reset() {
        receiveAddress = null;
        qrCodeImage.set(null);
        btcUri.setValue(null);
        amount.setValue(null);
        isAmountValid.setValue(false);
        label.setValue(null);
    }
}
