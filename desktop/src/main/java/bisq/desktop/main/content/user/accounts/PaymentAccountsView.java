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

package bisq.desktop.main.content.user.accounts;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PaymentAccountsView extends View<VBox, PaymentAccountsModel, PaymentAccountsController> {
    private final Label headline;
    private final Button createButton, largeCreateButton, deletedButton, saveButton;
    private final MaterialTextArea accountData;
    private final AutoCompleteComboBox<Account<?, ? extends PaymentMethod<?>>> accountSelection;
    private final HBox buttonsHBox;
    private final HBox selectionButtonHBox;
    private final VBox noAccountsVBox;
    private Subscription selectedAccountPin, noAccountsSetupPin;

    public PaymentAccountsView(PaymentAccountsModel model, PaymentAccountsController controller) {
        super(new VBox(20), model, controller);

        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(20, 40, 40, 40));

        headline = new Label();
        headline.getStyleClass().add("large-thin-headline");
        headline.setPadding(new Insets(-8, 0, 0, 0));

        Label noAccountsInfo = new Label(Res.get("user.paymentAccounts.noAccounts.info"));
        noAccountsInfo.setWrapText(true);
        noAccountsInfo.getStyleClass().add("user-payment-account-no-data");
        Label whySetup = new Label(Res.get("user.paymentAccounts.noAccounts.whySetup"));
        whySetup.setWrapText(true);
        whySetup.getStyleClass().add("large-thin-headline");
        Label whySetupInfo = new Label(Res.get("user.paymentAccounts.noAccounts.whySetup.info"));
        whySetupInfo.setWrapText(true);
        whySetupInfo.getStyleClass().add("user-content-text");
        Label whySetupNote = new Label(Res.get("user.paymentAccounts.noAccounts.whySetup.note"));
        whySetupNote.setWrapText(true);
        whySetupNote.getStyleClass().add("user-content-note");

        VBox.setMargin(noAccountsInfo, new Insets(-10, 0, 0, 0));
        VBox.setMargin(whySetup, new Insets(15, 0, -10, 0));
        VBox.setMargin(whySetupNote, new Insets(10, 0, 20, 0));
        noAccountsVBox = new VBox(20, noAccountsInfo, whySetup, whySetupInfo, whySetupNote);

        largeCreateButton = new Button(Res.get("user.paymentAccounts.createAccount"));
        largeCreateButton.setDefaultButton(true);

        createButton = new Button(Res.get("user.paymentAccounts.createAccount"));
        createButton.getStyleClass().add("outlined-button");

        accountSelection = new AutoCompleteComboBox<>(model.getSortedAccounts(), Res.get("user.paymentAccounts.selectAccount"));
        accountSelection.setPrefWidth(300);
        accountSelection.setConverter(new StringConverter<>() {
            @Override
            public String toString(Account<?, ? extends PaymentMethod<?>> object) {
                return object != null ? object.getAccountName() : "";
            }

            @Override
            public Account<?, ? extends PaymentMethod<?>> fromString(String string) {
                return null;
            }
        });

        selectionButtonHBox = new HBox(20, accountSelection, Spacer.fillHBox(), createButton);

        accountData = new MaterialTextArea(Res.get("user.paymentAccounts.accountData"), Res.get("user.paymentAccounts.createAccount.accountData.prompt"));
        accountData.setEditable(true);
        accountData.showEditIcon();
        accountData.getIconButton().setOpacity(0.2);
        accountData.getIconButton().setMouseTransparent(true);
        accountData.setFixedHeight(300);

        saveButton = new Button(Res.get("action.save"));
        saveButton.setDefaultButton(true);

        deletedButton = new Button(Res.get("user.paymentAccounts.deleteAccount"));

        buttonsHBox = new HBox(20, saveButton, deletedButton);
        root.getChildren().addAll(headline, noAccountsVBox, largeCreateButton, selectionButtonHBox, accountData, buttonsHBox);
    }

    @Override
    protected void onViewAttached() {
        headline.textProperty().bind(model.getHeadline());
        accountData.textProperty().bindBidirectional(model.accountDataProperty());
        saveButton.disableProperty().bind(model.saveButtonDisabledProperty());
        deletedButton.disableProperty().bind(model.deleteButtonDisabledProperty());

        largeCreateButton.setOnAction(e -> controller.onCreateAccount());
        createButton.setOnAction(e -> controller.onCreateAccount());
        saveButton.setOnAction(e -> controller.onSaveAccount());
        deletedButton.setOnAction(e -> controller.onDeleteAccount());

        accountSelection.setOnChangeConfirmed(e -> {
            if (accountSelection.getSelectionModel().getSelectedItem() == null) {
                accountSelection.getSelectionModel().select(model.getSelectedAccount());
                return;
            }
            controller.onSelectAccount(accountSelection.getSelectionModel().getSelectedItem());
        });

        selectedAccountPin = EasyBind.subscribe(model.selectedAccountProperty(),
                accountName -> accountSelection.getSelectionModel().select(accountName));

        noAccountsSetupPin = EasyBind.subscribe(model.getNoAccountsSetup(), noAccountsSetup -> {
            headline.setVisible(!noAccountsSetup);
            headline.setManaged(!noAccountsSetup);
            noAccountsVBox.setVisible(noAccountsSetup);
            noAccountsVBox.setManaged(noAccountsSetup);
            largeCreateButton.setVisible(noAccountsSetup);
            largeCreateButton.setManaged(noAccountsSetup);

            boolean anyAccountSetup = !noAccountsSetup;
            selectionButtonHBox.setVisible(anyAccountSetup);
            selectionButtonHBox.setManaged(anyAccountSetup);
            accountData.setVisible(anyAccountSetup);
            accountData.setManaged(anyAccountSetup);
            buttonsHBox.setVisible(anyAccountSetup);
            buttonsHBox.setManaged(anyAccountSetup);
        });
    }

    @Override
    protected void onViewDetached() {
        headline.textProperty().unbind();
        accountData.textProperty().unbindBidirectional(model.getAccountData());
        saveButton.disableProperty().unbind();
        deletedButton.disableProperty().unbind();

        largeCreateButton.setOnAction(null);
        createButton.setOnAction(null);
        deletedButton.setOnAction(null);
        saveButton.setOnAction(null);

        accountSelection.setOnChangeConfirmed(null);

        selectedAccountPin.unsubscribe();
        noAccountsSetupPin.unsubscribe();
    }
}
