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

package bisq.desktop.components.controls.validator;

import bisq.common.network.Address;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkAddressValidation;
import bisq.i18n.Res;
import javafx.scene.control.TextInputControl;

public class HostValidator extends ValidatorBase {
    public HostValidator() {
        super(Res.get("validation.invalidHost"));
    }

    @Override
    protected void eval() {
        TextInputControl textField = (TextInputControl) srcControl.get();
        String host = textField.getText();
        if (StringUtils.isEmpty(host)) {
            setMessage(Res.get("validation.emptyHost"));
            hasErrors.set(true);
            return;
        }

        try {
            Address address = Address.from(host, 80);
            boolean isValidAddress = NetworkAddressValidation.isValid(address);
            if (!isValidAddress) {
                setMessage(Res.get("validation.invalidHost"));
                hasErrors.set(true);
                return;
            }
            hasErrors.set(false);
        } catch (Exception e) {
            setMessage(ExceptionUtil.getMessageOrToString(e));
            hasErrors.set(true);
        }
    }
}
