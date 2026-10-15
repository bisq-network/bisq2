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

import bisq.common.encoding.BitcoinURIScheme;
import bisq.common.util.MathUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.qr.QrCodeDisplay;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.presentation.formatters.DefaultNumberFormatter;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class WalletAddressQrCodeController implements Controller {
    @Getter
    private final WalletAddressQrCodeView view;
    private final WalletAddressQrCodeModel model;
    private Subscription amountPin, isAmountValidPin, labelPin;

    public WalletAddressQrCodeController(ServiceProvider serviceProvider) {
        model = new WalletAddressQrCodeModel(170);
        view = new WalletAddressQrCodeView(model, this);
    }

    public void setReceiveAddress(String receiveAddress) {
        if (receiveAddress != null) {
            model.setReceiveAddress(receiveAddress);
        }
    }

    public void reset() {
        model.reset();
    }

    public boolean validate() {
        return true;
    }

    @Override
    public void onActivate() {
        amountPin = EasyBind.subscribe(model.getAmount(), amount -> UIThread.run(this::updateBtcUriAndQrCode));
        isAmountValidPin = EasyBind.subscribe(model.getIsAmountValid(), isAmountValid -> UIThread.run(() -> {
            parseAmount();
            updateBtcUriAndQrCode();
        }));
        labelPin = EasyBind.subscribe(model.getLabel(), label -> UIThread.run(this::updateBtcUriAndQrCode));
    }

    @Override
    public void onDeactivate() {
        amountPin.unsubscribe();
        isAmountValidPin.unsubscribe();
        labelPin.unsubscribe();
    }

    void onCopyToClipboard() {
        if (model.getBtcUri() != null) {
            ClipboardUtil.copyToClipboard(model.getBtcUri().get());
        }
    }

    private void parseAmount() {
        String amount = model.getAmount().get();
        if (amount != null && !StringUtils.isEmpty(amount) && MathUtils.isValidDouble(amount)) {
            amount = DefaultNumberFormatter.reformat(amount);
            amount = StringUtils.removeAllWhitespaces(amount);
            model.getAmount().set(amount);
        }
    }

    private void updateBtcUriAndQrCode() {
        if (model.getReceiveAddress() != null) {
            String address = model.getReceiveAddress();
            Optional<String> amount = Optional.empty();
            if (model.getIsAmountValid() != null && model.getIsAmountValid().get()) {
                amount = Optional.ofNullable(model.getAmount())
                        .map(StringProperty::get)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty());
            }
            Optional<String> label = Optional.ofNullable(model.getLabel())
                    .map(StringProperty::get)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty());
            String btcUri = BitcoinURIScheme.buildBitcoinUri(address, amount, label);
            Image image = QrCodeDisplay.toImage(btcUri, model.getQrCodeSize());
            model.getBtcUri().set(btcUri);
            model.getQrCodeImage().set(image);
        }
    }
}
