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

package bisq.desktop.main.content.user.fiat_accounts;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class FiatPaymentAccountsView extends View<VBox, FiatPaymentAccountsModel, FiatPaymentAccountsController> {
    private final Label headline;
    private final Button createButtonWithAccounts, createButtonNoAccounts, saveButton, deletedButton, importBisq1AccountDataButton;
    private final AutoCompleteComboBox<Account<?, ?>> accountsComboBox;
    private final HBox comboBoxAndCreateButtonHBox;
    private final VBox noAccountsVBox;
    private final Pane accountDisplayPane;
    private final Region lineAfterHeadline;
    private Subscription selectedAccountPin, noAccountsSetupPin, accountDisplayPin;

    public FiatPaymentAccountsView(FiatPaymentAccountsModel model, FiatPaymentAccountsController controller) {
        super(new VBox(0), model, controller);

        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(0, 40, 40, 40));

        headline = new Label(Res.get("paymentAccounts.headline"));
        headline.getStyleClass().add("large-thin-headline");

        Label noAccountsInfo = new Label(Res.get("paymentAccounts.noAccounts.info"));
        noAccountsInfo.setWrapText(true);
        noAccountsInfo.getStyleClass().add("user-payment-account-no-data");
        Label whySetup = new Label(Res.get("paymentAccounts.noAccounts.whySetup"));
        whySetup.setWrapText(true);
        whySetup.getStyleClass().add("large-thin-headline");
        Label whySetupInfo = new Label(Res.get("paymentAccounts.noAccounts.whySetup.info"));
        whySetupInfo.setWrapText(true);
        whySetupInfo.getStyleClass().add("user-content-text");
        Label whySetupNote = new Label(Res.get("paymentAccounts.noAccounts.whySetup.note"));
        whySetupNote.setWrapText(true);
        whySetupNote.getStyleClass().add("user-content-note");

        VBox.setMargin(whySetup, new Insets(5, 0, -10, 0));
        VBox.setMargin(whySetupNote, new Insets(10, 0, 15, 0));
        noAccountsVBox = new VBox(20, noAccountsInfo, whySetup, whySetupInfo, whySetupNote);

        createButtonNoAccounts = new Button(Res.get("paymentAccounts.createAccount"));
        createButtonNoAccounts.setDefaultButton(true);

        createButtonWithAccounts = new Button(Res.get("paymentAccounts.createAccount"));
        createButtonWithAccounts.getStyleClass().add("outlined-button");

        accountsComboBox = new AutoCompleteComboBox<>(model.getSortedAccounts(), Res.get("paymentAccounts.selectAccount"));
        accountsComboBox.setPrefWidth(325);
        accountsComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Account<? extends PaymentMethod<?>, ?> account) {
                return account != null ? account.getAccountName() : "";
            }

            @Override
            public Account<? extends PaymentMethod<?>, ?> fromString(String string) {
                return null;
            }
        });

        comboBoxAndCreateButtonHBox = new HBox(20, accountsComboBox, Spacer.fillHBox(), createButtonWithAccounts);

        saveButton = new Button(Res.get("action.save"));
        saveButton.setDefaultButton(true);

        deletedButton = new Button(Res.get("paymentAccounts.deleteAccount"));

        importBisq1AccountDataButton= new Button(Res.get("paymentAccounts.importBisq1AccountData"));

        accountDisplayPane = new StackPane();

        VBox contentBox = new VBox(30);
        lineAfterHeadline = SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing());
        contentBox.getChildren().addAll(headline,
                lineAfterHeadline,
                noAccountsVBox,
                createButtonNoAccounts,
                comboBoxAndCreateButtonHBox,
                accountDisplayPane,
                new HBox(10, saveButton, deletedButton, importBisq1AccountDataButton));
        contentBox.getStyleClass().add("bisq-common-bg");

        root.getChildren().add(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
        deletedButton.disableProperty().bind(model.getDeleteButtonDisabled());
        saveButton.disableProperty().bind(model.getSaveButtonDisabled());
        saveButton.visibleProperty().bind(model.getSaveButtonVisible());
        saveButton.managedProperty().bind(model.getSaveButtonVisible());
        noAccountsSetupPin = EasyBind.subscribe(model.getNoAccountsAvailable(), noAccountsSetup -> {
            boolean anyAccountSetup = !noAccountsSetup;
            lineAfterHeadline.setVisible(anyAccountSetup);
            lineAfterHeadline.setManaged(anyAccountSetup);
            headline.setVisible(anyAccountSetup);
            headline.setManaged(anyAccountSetup);
            noAccountsVBox.setVisible(noAccountsSetup);
            noAccountsVBox.setManaged(noAccountsSetup);
            createButtonNoAccounts.setVisible(noAccountsSetup);
            createButtonNoAccounts.setManaged(noAccountsSetup);

            comboBoxAndCreateButtonHBox.setVisible(anyAccountSetup);
            comboBoxAndCreateButtonHBox.setManaged(anyAccountSetup);
            deletedButton.setVisible(anyAccountSetup);
            deletedButton.setManaged(anyAccountSetup);
        });

        selectedAccountPin = EasyBind.subscribe(model.getSelectedAccount(),
                account -> accountsComboBox.getSelectionModel().select(account));

        accountDisplayPin = EasyBind.subscribe(model.getAccountDetails(), accountDetails -> {
            if (accountDetails != null) {
                accountDisplayPane.getChildren().setAll(accountDetails);
            } else {
                accountDisplayPane.getChildren().clear();
            }
        });

        accountsComboBox.setOnChangeConfirmed(e -> {
            if (accountsComboBox.getSelectionModel().getSelectedItem() == null) {
                accountsComboBox.getSelectionModel().select(model.getSelectedAccount().get());
                return;
            }
            controller.onSelectAccount(accountsComboBox.getSelectionModel().getSelectedItem());
        });

        createButtonNoAccounts.setOnAction(e -> controller.onCreateAccount());
        createButtonWithAccounts.setOnAction(e -> controller.onCreateAccount());
        saveButton.setOnAction(e -> controller.onSaveAccount());
        deletedButton.setOnAction(e -> controller.onDeleteAccount());
        importBisq1AccountDataButton.setOnAction(e -> controller.onImportBisq1AccountData());
    }

    @Override
    protected void onViewDetached() {
        deletedButton.disableProperty().unbind();
        saveButton.disableProperty().unbind();
        saveButton.visibleProperty().unbind();
        saveButton.managedProperty().unbind();
        noAccountsSetupPin.unsubscribe();
        selectedAccountPin.unsubscribe();
        accountDisplayPin.unsubscribe();

        accountsComboBox.setOnChangeConfirmed(null);

        createButtonNoAccounts.setOnAction(null);
        createButtonWithAccounts.setOnAction(null);
        saveButton.setOnAction(null);
        deletedButton.setOnAction(null);
        importBisq1AccountDataButton.setOnAction(null);
    }
}
