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

import bisq.account.accounts.AccountPayload;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import lombok.Getter;

public abstract class FormController<V extends FormView<?, ?>, M extends FormModel, P extends AccountPayload<?>> implements Controller {
    @Getter
    protected final V view;
    protected final M model;

    protected FormController(ServiceProvider serviceProvider) {
        this.model = createModel();
        this.view = createView();
    }

    protected abstract V createView();

    protected abstract M createModel();

    public abstract boolean validate();

    public abstract P createAccountPayload();
}