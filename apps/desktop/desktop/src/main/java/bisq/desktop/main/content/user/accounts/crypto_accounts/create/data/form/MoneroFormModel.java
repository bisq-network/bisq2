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

package bisq.desktop.main.content.user.accounts.crypto_accounts.create.data.form;

import bisq.account.payment_method.DigitalAssetPaymentMethod;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.TextMinMaxLengthValidator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

@Getter
public class MoneroFormModel extends FormModel {
    //todo
    protected final TextMinMaxLengthValidator mainAddressValidator = new TextMinMaxLengthValidator(10, 200);
    protected final TextMinMaxLengthValidator privateViewKeyValidator = new TextMinMaxLengthValidator(10, 200);
    protected final NumberValidator accountIndexValidator = new NumberValidator(0, 100000);
    protected final NumberValidator initialSubAddressIndexValidator = new NumberValidator(0, 100000);

    private final BooleanProperty useSubAddresses = new SimpleBooleanProperty();
    protected final StringProperty mainAddress = new SimpleStringProperty();
    protected final StringProperty privateViewKey = new SimpleStringProperty();
    protected final StringProperty subAddress = new SimpleStringProperty();
    protected final StringProperty accountIndex = new SimpleStringProperty();
    protected final StringProperty initialSubAddressIndex = new SimpleStringProperty();

    public MoneroFormModel(String id, DigitalAssetPaymentMethod paymentMethod) {
        super(id, paymentMethod);
    }

    public void reset() {
        super.reset();
        useSubAddresses.set(false);
        mainAddress.set(null);
        privateViewKey.set(null);
        subAddress.set(null);
        accountIndex.set(null);
        initialSubAddressIndex.set(null);
    }
}
