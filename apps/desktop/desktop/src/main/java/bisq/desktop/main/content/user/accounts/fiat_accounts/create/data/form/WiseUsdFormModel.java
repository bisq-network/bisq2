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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form;

import bisq.common.validation.PaymentAccountValidation;
import bisq.desktop.components.controls.validator.EmailValidator;
import bisq.desktop.components.controls.validator.TextMinMaxLengthValidator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

@Getter
public class WiseUsdFormModel extends FormModel {
    private final BooleanProperty runValidation = new SimpleBooleanProperty();

    private final StringProperty holderName = new SimpleStringProperty();
    private final TextMinMaxLengthValidator holderNameValidator = new TextMinMaxLengthValidator(
            PaymentAccountValidation.HOLDER_NAME_MIN_LENGTH,
            PaymentAccountValidation.HOLDER_NAME_MAX_LENGTH);

    private final StringProperty email = new SimpleStringProperty();
    private final EmailValidator emailValidator = new EmailValidator();

    private final StringProperty beneficiaryAddress = new SimpleStringProperty();
    private final TextMinMaxLengthValidator beneficiaryAddressValidator = new TextMinMaxLengthValidator(
            PaymentAccountValidation.ADDRESS_MIN_LENGTH,
            PaymentAccountValidation.ADDRESS_MAX_LENGTH);

    public WiseUsdFormModel(String id) {
        super(id);
    }
}
