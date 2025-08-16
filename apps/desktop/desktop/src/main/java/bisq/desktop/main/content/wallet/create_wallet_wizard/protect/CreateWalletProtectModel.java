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

package bisq.desktop.main.content.wallet.create_wallet_wizard.protect;

import bisq.desktop.common.view.Model;
import bisq.desktop.components.controls.validator.TextMinMaxLengthValidator;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class CreateWalletProtectModel implements Model {
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 100;

    private final StringProperty password = new SimpleStringProperty("");
    private final TextMinMaxLengthValidator passwordValidator = new TextMinMaxLengthValidator(PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH);

    private final StringProperty confirmPassword = new SimpleStringProperty("");
    private final TextMinMaxLengthValidator confirmPasswordValidator = new TextMinMaxLengthValidator(PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH);
}