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

package bisq.desktop.primary.main.content.user.accounts.create;

import bisq.account.bisqeasy.BisqEasyPaymentAccount;
import bisq.account.bisqeasy.BisqEasyPaymentAccountService;
import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.i18n.Res;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class CreatePaymentAccountController implements Controller {

    protected final CreatePaymentAccountModel model;
    @Getter
    protected final CreatePaymentAccountView view;
    private final DefaultApplicationService applicationService;
    private final BisqEasyPaymentAccountService service;
    private Subscription accountDataSubscription, accountNameSubscription;

    public CreatePaymentAccountController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        service = applicationService.getAccountService().getBisqEasyPaymentAccountService();

        model = new CreatePaymentAccountModel();
        view = new CreatePaymentAccountView(model, this);
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
        accountDataSubscription.unsubscribe();
        accountNameSubscription.unsubscribe();
    }

    void onCancel() {
        close();
    }

    void onQuit() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }

    protected void onSave() {
        checkArgument(isDataValid());
        if (service.findAccount(model.getAccountName()).isPresent()) {
            new Popup()
                    .warning(Res.get("user.paymentAccounts.createAccount.sameName"))
                    .onAction(() -> model.setAccountName(""))
                    .show();
        } else {
            BisqEasyPaymentAccount newAccount = new BisqEasyPaymentAccount(model.getAccountName(),
                    model.getAccountData());
            service.addPaymentAccount(newAccount);
            service.setSelectedAccount(newAccount);
            close();
        }
    }

    protected void close() {
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