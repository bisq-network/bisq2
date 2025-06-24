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

import bisq.account.accounts.SepaAccountPayload;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class SepaPaymentFormController extends PaymentFormController<SepaPaymentFormView, SepaPaymentFormModel, SepaAccountPayload> {
    public SepaPaymentFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    public SepaAccountPayload getAccountPayload() {
        return null;
    }

    @Override
    protected SepaPaymentFormView createView() {
        return new SepaPaymentFormView(model, this);
    }

    @Override
    protected SepaPaymentFormModel createModel() {
        return new SepaPaymentFormModel(UUID.randomUUID().toString());
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public boolean validate() {
        return false;
    }
}