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

package bisq.desktop.main.content.user.accounts.create.legacy;

import bisq.account.AccountService;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.account.accounts.UserDefinedFiatAccountPayload;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class LegacyCreatePaymentAccountController implements Controller {

    protected final LegacyCreatePaymentAccountModel model;
    @Getter
    protected final LegacyCreatePaymentAccountView view;
    private final AccountService accountService;
    private Subscription accountDataSubscription, accountNameSubscription;

    public LegacyCreatePaymentAccountController(ServiceProvider serviceProvider) {
        accountService = serviceProvider.getAccountService();
        model = new LegacyCreatePaymentAccountModel();
        view = new LegacyCreatePaymentAccountView(model, this);
    }

    @Override
    public void onActivate() {
        model.setAccountData("");
        model.setAccountName("");
        accountDataSubscription = EasyBind.subscribe(model.accountDataProperty(), accountData -> updateButtonStates());
        accountNameSubscription = EasyBind.subscribe(model.accountNameProperty(), accountName -> updateButtonStates());
    }

    @Override
    public void onDeactivate() {
        if (accountDataSubscription != null) {
            accountDataSubscription.unsubscribe();
            accountDataSubscription = null;
        }
        if (accountNameSubscription != null) {
            accountNameSubscription.unsubscribe();
            accountNameSubscription = null;
        }
    }

    void onCancel() {
        close();
    }

    void onSave() {
        checkArgument(isDataValid());
        if (accountService.findAccount(model.getAccountName()).isPresent()) {
            new Popup()
                    .warning(Res.get("user.paymentAccounts.createAccount.sameName"))
                    .onAction(() -> model.setAccountName(""))
                    .show();
        } else {
            String accountData = model.getAccountData();
            checkNotNull(accountData);
            checkArgument(accountData.length() <= UserDefinedFiatAccountPayload.MAX_DATA_LENGTH,
                    "Account data must not be longer than 1000 characters");
            UserDefinedFiatAccount newAccount = new UserDefinedFiatAccount(model.getAccountName(), accountData);
            accountService.addPaymentAccount(newAccount);
            accountService.setSelectedAccount(newAccount);
            close();
        }
    }

    void close() {
        OverlayController.hide();
    }

    private void updateButtonStates() {
        model.setSaveButtonDisabled(!isDataValid());
    }

    private boolean isDataValid() {
        return model.getAccountData() != null
                && !model.getAccountData().isEmpty()
                && model.getAccountName() != null
                && !model.getAccountName().isEmpty();
    }
}