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

package bisq.desktop.main.content.user.accounts.create.account_data;

import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentDataEntryView extends View<VBox, PaymentDataEntryModel, PaymentDataEntryController> {
    private final Label titleLabel;
    private final VBox formContainer;

    public PaymentDataEntryView(PaymentDataEntryModel model, PaymentDataEntryController controller) {
        super(new VBox(10), model, controller);

        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("create-account-data-view");

        titleLabel = new Label(Res.get("user.paymentAccounts.createAccount.accountData.title"));
        titleLabel.getStyleClass().add("bisq-text-headline-2");

        formContainer = new VBox();
        formContainer.setAlignment(Pos.TOP_CENTER);
        formContainer.setPadding(new Insets(5));

        root.getChildren().addAll(titleLabel, formContainer, Spacer.fillVBox());
    }

    public void setFormView(View<?, ?, ?> formView) {
        updateFormContainer(formView);
        updateTitle(model.getPaymentMethod().get());
    }

    @Override
    protected void onViewAttached() {
        updateTitle(model.getPaymentMethod().get());
    }

    @Override
    protected void onViewDetached() {
    }

    private void updateFormContainer(View<?, ?, ?> formView) {
        formContainer.getChildren().clear();

        if (formView != null) {
            formContainer.getChildren().add(formView.getRoot());
        }
    }

    private void updateTitle(PaymentMethod<?> paymentMethod) {
        titleLabel.setText(Res.get("user.paymentAccounts.createAccount.accountData.title",
                paymentMethod.getDisplayString()));
    }
}