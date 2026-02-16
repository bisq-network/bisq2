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

import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class InteracETransferFormView extends FormView<InteracETransferFormModel, InteracETransferFormController> {
    private final MaterialTextField holderName;
    private final MaterialTextField email;
    private final MaterialTextField question;
    private final MaterialTextField answer;
    private Subscription runValidationPin;

    public InteracETransferFormView(InteracETransferFormModel model, InteracETransferFormController controller) {
        super(model, controller);

        VBox.setMargin(titleLabel, new Insets(20, 0, 10, 0));

        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        email = new MaterialTextField(Res.get("paymentAccounts.email"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.email"))));
        email.setValidators(model.getEmailValidator());
        email.setMaxWidth(Double.MAX_VALUE);

        question = new MaterialTextField(Res.get("paymentAccounts.interacETransfer.question"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.interacETransfer.question"))));
        question.setValidators(model.getQuestionValidator());
        question.setMaxWidth(Double.MAX_VALUE);

        answer = new MaterialTextField(Res.get("paymentAccounts.interacETransfer.answer"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.interacETransfer.answer"))));
        answer.setValidators(model.getAnswerValidator());
        answer.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(holderName, email, question, answer);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }
        if (StringUtils.isNotEmpty(model.getEmail().get())) {
            email.setText(model.getEmail().get());
            email.validate();
        }
        if (StringUtils.isNotEmpty(model.getQuestion().get())) {
            question.setText(model.getQuestion().get());
            question.validate();
        }
        if (StringUtils.isNotEmpty(model.getAnswer().get())) {
            answer.setText(model.getAnswer().get());
            answer.validate();
        }

        holderName.textProperty().bindBidirectional(model.getHolderName());
        email.textProperty().bindBidirectional(model.getEmail());
        question.textProperty().bindBidirectional(model.getQuestion());
        answer.textProperty().bindBidirectional(model.getAnswer());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                email.validate();
                question.validate();
                answer.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        holderName.resetValidation();
        email.resetValidation();
        question.resetValidation();
        answer.resetValidation();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        email.textProperty().unbindBidirectional(model.getEmail());
        question.textProperty().unbindBidirectional(model.getQuestion());
        answer.textProperty().unbindBidirectional(model.getAnswer());

        runValidationPin.unsubscribe();
    }
}
