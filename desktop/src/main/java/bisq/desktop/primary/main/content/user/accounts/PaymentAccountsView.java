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

package bisq.desktop.primary.main.content.user.accounts;

import bisq.account.accounts.Account;
import bisq.account.settlement.Settlement;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PaymentAccountsView extends View<HBox, PaymentAccountsModel, PaymentAccountsController> {
    private final Button createButton, deletedButton, saveButton;
    private final MaterialTextArea accountData;
    private final VBox formVBox;
    private final AutoCompleteComboBox<Account<?, ? extends Settlement<?>>> accountSelection;
    private Subscription selectedAccountPin;

    public PaymentAccountsView(PaymentAccountsModel model, PaymentAccountsController controller) {
        super(new HBox(20), model, controller);

        root.setPadding(new Insets(40, 0, 0, 0));

        formVBox = new VBox(20);
        HBox.setHgrow(formVBox, Priority.ALWAYS);
        root.getChildren().add(formVBox);

        createButton = new Button(Res.get("user.paymentAccounts.createAccount"));
        createButton.getStyleClass().addAll("outlined-button");

        accountSelection = new AutoCompleteComboBox<>(model.getSortedAccounts(), Res.get("user.paymentAccounts.selectAccount"));
        accountSelection.setPrefWidth(300);
        accountSelection.setConverter(new StringConverter<>() {
            @Override
            public String toString(Account<?, ? extends Settlement<?>> object) {
                return object != null ? object.getAccountName() : "";
            }

            @Override
            public Account<?, ? extends Settlement<?>> fromString(String string) {
                return null;
            }
        });

        HBox selectionButtonHBox = new HBox(20, accountSelection, Spacer.fillHBox(), createButton);
        formVBox.getChildren().add(selectionButtonHBox);

        accountData = addTextArea(Res.get("user.paymentAccounts.accountData"), Res.get("user.paymentAccounts.createAccount.accountData.prompt"));
        accountData.setEditable(true);
        accountData.setIcon(AwesomeIcon.EDIT);
        accountData.getIconButton().setOpacity(0.2);
        accountData.getIconButton().setMouseTransparent(true);
        accountData.setFixedHeight(300);

        saveButton = new Button(Res.get("save"));
        saveButton.setDefaultButton(true);

        deletedButton = new Button(Res.get("user.paymentAccounts.deleteAccount"));

        HBox buttonsHBox = new HBox(20, saveButton, deletedButton);
        formVBox.getChildren().add(buttonsHBox);
    }

    @Override
    protected void onViewAttached() {
        accountData.textProperty().bindBidirectional(model.accountDataProperty());
        saveButton.disableProperty().bind(model.saveButtonDisabledProperty());
        deletedButton.disableProperty().bind(model.deleteButtonDisabledProperty());

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
    }

    @Override
    protected void onViewDetached() {
        accountData.textProperty().unbindBidirectional(model.getAccountData());
        saveButton.disableProperty().unbind();
        deletedButton.disableProperty().unbind();

        deletedButton.setOnAction(null);
        saveButton.setOnAction(null);
        createButton.setOnAction(null);

        accountSelection.setOnChangeConfirmed(null);

        selectedAccountPin.unsubscribe();
    }

    private MaterialTextArea addTextArea(String description, String prompt) {
        MaterialTextArea field = new MaterialTextArea(description, prompt);
        field.setEditable(false);
        field.setFixedHeight(2 * 56 + 20); // MaterialTextField has height 56
        formVBox.getChildren().add(field);
        return field;
    }
}
