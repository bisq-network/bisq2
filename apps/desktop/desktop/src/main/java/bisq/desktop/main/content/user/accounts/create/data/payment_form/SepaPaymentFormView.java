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

package bisq.desktop.main.content.user.accounts.create.data.payment_form;

import bisq.common.locale.Country;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextField;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import java.util.List;

public class SepaPaymentFormView extends PaymentFormView<SepaPaymentFormModel, SepaPaymentFormController> {
    private MaterialTextField holderNameField;
    private MaterialTextField ibanField;
    private MaterialTextField bicField;
    private AutoCompleteComboBox<Country> countryBox;

    private Label acceptedCountriesLabel;
    private CheckBox acceptAllCountriesCheckBox;
    private FlowPane specificCountriesContainer;
    private List<CheckBox> countryCheckBoxes;
    private Label countrySelectionHint;

    private Label holderNameErrorLabel;
    private Label ibanErrorLabel;
    private Label bicErrorLabel;
    private Label countryErrorLabel;
    private Label acceptedCountriesErrorLabel;
    private List<Country> sepaCountries;

    public SepaPaymentFormView(SepaPaymentFormModel model, SepaPaymentFormController controller) {
        super(model, controller);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}