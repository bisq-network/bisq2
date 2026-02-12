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

package bisq.desktop.main.content.user.accounts.crypto_accounts.create.address.form;

import bisq.account.payment_method.DigitalAssetPaymentMethod;
import bisq.common.asset.CryptoAsset;
import bisq.common.asset.CryptoAssetRepository;
import bisq.desktop.common.view.Model;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.TextMinMaxLengthValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

@Getter
public abstract class FormModel implements Model {
    protected final String id;
    protected final DigitalAssetPaymentMethod paymentMethod;
    protected final String currencyCode;
    protected final CryptoAsset cryptoAsset;
    protected final boolean isAutoConfSupported;
    protected final ValidatorBase addressValidator;

    //todo
    protected final NumberValidator autoConfNumConfirmationsValidator = new NumberValidator(1, 10);
    protected final NumberValidator autoConfMaxTradeAmountValidator = new NumberValidator(0.001, 1);
    protected final TextMinMaxLengthValidator autoConfExplorerUrlsValidator = new TextMinMaxLengthValidator(10, 200);

    protected final BooleanProperty runValidation = new SimpleBooleanProperty();
    protected final StringProperty address = new SimpleStringProperty();
    protected final BooleanProperty isInstant = new SimpleBooleanProperty();
    protected final BooleanProperty isAutoConf = new SimpleBooleanProperty();
    protected final StringProperty autoConfNumConfirmations = new SimpleStringProperty();
    protected final StringProperty autoConfMaxTradeAmount = new SimpleStringProperty();
    protected final StringProperty autoConfExplorerUrls = new SimpleStringProperty();

    public FormModel(String id, DigitalAssetPaymentMethod paymentMethod) {
        this.id = id;
        this.paymentMethod = paymentMethod;
        currencyCode = paymentMethod.getCode();

        cryptoAsset = CryptoAssetRepository.findOrCreateCustom(currencyCode);
        isAutoConfSupported = cryptoAsset.isSupportAutoConf();
        //addressValidator = new CryptoAssetValidator(cryptoAsset.getValidation());
        addressValidator = new ValidatorBase();//todo for dev testing we bypass validation

    }

    void reset() {
        runValidation.set(false);
        address.set(null);
        isInstant.set(false);
        isAutoConf.set(false);
        autoConfNumConfirmations.set(null);
        autoConfMaxTradeAmount.set(null);
        autoConfExplorerUrls.set(null);
    }
}