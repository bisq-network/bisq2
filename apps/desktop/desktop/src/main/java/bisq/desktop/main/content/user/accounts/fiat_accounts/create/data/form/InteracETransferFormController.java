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

import bisq.account.accounts.fiat.InteracETransferAccountPayload;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class InteracETransferFormController extends FormController<InteracETransferFormView, InteracETransferFormModel, InteracETransferAccountPayload> {
    public InteracETransferFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected InteracETransferFormView createView() {
        return new InteracETransferFormView(model, this);
    }

    @Override
    protected InteracETransferFormModel createModel() {
        return new InteracETransferFormModel(StringUtils.createUid());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
    }

    @Override
    public boolean validate() {
        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean emailValid = model.getEmailValidator().validateAndGet();
        boolean questionValid = model.getQuestionValidator().validateAndGet();
        boolean answerValid = model.getAnswerValidator().validateAndGet();
        model.getRunValidation().set(true);
        return holderNameValid && emailValid && questionValid && answerValid;
    }

    @Override
    public InteracETransferAccountPayload createAccountPayload() {
        return new InteracETransferAccountPayload(model.getId(),
                model.getHolderName().get(),
                model.getEmail().get(),
                model.getQuestion().get(),
                model.getAnswer().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }
}
