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

import bisq.common.validation.LightningInvoiceValidation;
import bisq.i18n.Res;
import javafx.scene.control.TextInputControl;

public class LightningInvoiceValidator extends ValidatorBase {

    public LightningInvoiceValidator(String message) {
        super(message);
    }

    public LightningInvoiceValidator() {
        super(Res.get("validation.invalidLightningInvoice"));
    }

    @Override
    protected void eval() {
        TextInputControl textField = (TextInputControl) srcControl.get();
        String invoice = textField.getText();
        if (invoice != null && !invoice.isEmpty()) {
            hasErrors.set(!LightningInvoiceValidation.isValid(invoice));
        } else {
            hasErrors.set(false);
        }
    }
}
