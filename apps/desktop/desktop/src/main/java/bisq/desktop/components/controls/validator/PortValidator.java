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

import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkPortValidation;
import bisq.i18n.Res;
import javafx.scene.control.TextInputControl;

public class PortValidator extends ValidatorBase {
    public PortValidator() {
        super(Res.get("validation.invalidPort"));
    }

    @Override
    protected void eval() {
        TextInputControl textField = (TextInputControl) srcControl.get();
        String portValue = textField.getText();
        if (StringUtils.isEmpty(portValue)) {
            setMessage("No port is set");
            hasErrors.set(true);
            return;
        }

        try {
            int port = Integer.parseInt(portValue);
            if (!NetworkPortValidation.isValid(port)) {
                setMessage("Port is invalid");
                hasErrors.set(true);
                return;
            }
            hasErrors.set(false);
        } catch (NumberFormatException e) {
            setMessage("Input is not a number");
            hasErrors.set(true);
        } catch (Exception e) {
            setMessage(ExceptionUtil.getMessageOrToString(e));
            hasErrors.set(true);
        }
    }
}
