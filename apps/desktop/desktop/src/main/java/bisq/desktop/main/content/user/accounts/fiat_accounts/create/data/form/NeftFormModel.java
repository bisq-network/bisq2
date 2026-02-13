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

import bisq.account.accounts.fiat.NeftAccountPayload;
import bisq.desktop.components.controls.validator.TextMinMaxLengthValidator;
import bisq.desktop.components.controls.validator.fiat.IFSCValidator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

@Getter
public class NeftFormModel extends FormModel {
    private final BooleanProperty runValidation = new SimpleBooleanProperty();
    private final StringProperty holderName = new SimpleStringProperty();
    private final StringProperty accountNr = new SimpleStringProperty();
    private final StringProperty ifsc = new SimpleStringProperty();

    private final TextMinMaxLengthValidator holderNameValidator = new TextMinMaxLengthValidator(
            NeftAccountPayload.HOLDER_NAME_MIN_LENGTH,
            NeftAccountPayload.HOLDER_NAME_MAX_LENGTH);
    private final TextMinMaxLengthValidator accountNrValidator = new TextMinMaxLengthValidator(
            NeftAccountPayload.ACCOUNT_NR_MIN_LENGTH,
            NeftAccountPayload.ACCOUNT_NR_MAX_LENGTH);
    private final IFSCValidator ifscValidator = new IFSCValidator();

    public NeftFormModel(String id) {
        super(id);
    }
}
