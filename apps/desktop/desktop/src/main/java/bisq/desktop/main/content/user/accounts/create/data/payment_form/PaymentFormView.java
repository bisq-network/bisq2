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

import bisq.desktop.common.view.View;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public abstract class PaymentFormView<M extends PaymentFormModel, C extends PaymentFormController<?, ?>> extends View<VBox, M, C> {

    protected final Map<String, Label> errorLabels = new HashMap<>();
    protected final Map<String, ChangeListener<?>> listeners = new HashMap<>();

    protected PaymentFormView(M model, C controller) {
        super(new VBox(15), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(0, 20, 0, 20));
        root.getStyleClass().add("payment-method-form");
    }
}