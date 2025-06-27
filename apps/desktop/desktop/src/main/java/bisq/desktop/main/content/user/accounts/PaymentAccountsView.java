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
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PaymentAccountsView extends View<VBox, PaymentAccountsModel, PaymentAccountsController> {
    private final Label headline;
    private final Button createButtonWithAccounts, createButtonNoAccounts, deletedButton;
    private final AutoCompleteComboBox<Account<?, ?>> comboBox;
    private final HBox comboBoxAndCreateButtonHBox;
    private final VBox noAccountsVBox;
    private final Pane accountDisplayPane;
    private Subscription selectedAccountPin, noAccountsSetupPin, accountDisplayPin;

    public PaymentAccountsView(PaymentAccountsModel model, PaymentAccountsController controller) {
        super(new VBox(0), model, controller);

        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(0, 40, 40, 40));

        headline = new Label();
        headline.getStyleClass().add("large-thin-headline");

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

        VBox.setMargin(whySetup, new Insets(5, 0, -10, 0));
        VBox.setMargin(whySetupNote, new Insets(10, 0, 15, 0));
        noAccountsVBox = new VBox(20, noAccountsInfo, whySetup, whySetupInfo, whySetupNote);

        createButtonNoAccounts = new Button(Res.get("user.paymentAccounts.createAccount"));
        createButtonNoAccounts.setDefaultButton(true);

        createButtonWithAccounts = new Button(Res.get("user.paymentAccounts.createAccount"));
        createButtonWithAccounts.getStyleClass().add("outlined-button");

        comboBox = new AutoCompleteComboBox<>(model.getSortedAccounts(), Res.get("user.paymentAccounts.selectAccount"));
        comboBox.setPrefWidth(230);
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Account<? extends PaymentMethod<?>, ?> account) {
                return account != null ? account.getAccountName() : "";
            }

            @Override
            public Account<? extends PaymentMethod<?>, ?> fromString(String string) {
                return null;
            }
        });

        comboBoxAndCreateButtonHBox = new HBox(20, comboBox, Spacer.fillHBox(), createButtonWithAccounts);

        deletedButton = new Button(Res.get("user.paymentAccounts.deleteAccount"));

        accountDisplayPane = new StackPane();

        VBox contentBox = new VBox(30);
        contentBox.getChildren().addAll(headline,
                SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()),
                noAccountsVBox,
                createButtonNoAccounts,
                comboBoxAndCreateButtonHBox,
                accountDisplayPane,
                deletedButton);
        contentBox.getStyleClass().add("bisq-common-bg");

        root.getChildren().add(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
        headline.textProperty().bind(model.getHeadline());
        deletedButton.disableProperty().bind(model.getDeleteButtonDisabled());

        createButtonNoAccounts.setOnAction(e -> controller.onCreateAccount());
        createButtonWithAccounts.setOnAction(e -> controller.onCreateAccount());
        deletedButton.setOnAction(e -> controller.onDeleteAccount());

        comboBox.setOnChangeConfirmed(e -> {
            if (comboBox.getSelectionModel().getSelectedItem() == null) {
                comboBox.getSelectionModel().select(model.getSelectedAccount().get());
                return;
            }
            controller.onSelectAccount(comboBox.getSelectionModel().getSelectedItem());
        });

        noAccountsSetupPin = EasyBind.subscribe(model.getNoAccountsSetup(), noAccountsSetup -> {
            headline.setVisible(!noAccountsSetup);
            headline.setManaged(!noAccountsSetup);
            noAccountsVBox.setVisible(noAccountsSetup);
            noAccountsVBox.setManaged(noAccountsSetup);
            createButtonNoAccounts.setVisible(noAccountsSetup);
            createButtonNoAccounts.setManaged(noAccountsSetup);

            boolean anyAccountSetup = !noAccountsSetup;
            comboBoxAndCreateButtonHBox.setVisible(anyAccountSetup);
            comboBoxAndCreateButtonHBox.setManaged(anyAccountSetup);
        });

        selectedAccountPin = EasyBind.subscribe(model.getSelectedAccount(),
                account -> comboBox.getSelectionModel().select(account));

        accountDisplayPin = EasyBind.subscribe(model.getAccountDetailsGridPane(), accountDisplay -> {
            if (accountDisplay != null) {
                accountDisplayPane.getChildren().setAll(accountDisplay);
            } else {
                accountDisplayPane.getChildren().clear();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        headline.textProperty().unbind();
        deletedButton.disableProperty().unbind();

        createButtonNoAccounts.setOnAction(null);
        createButtonWithAccounts.setOnAction(null);
        deletedButton.setOnAction(null);

        comboBox.setOnChangeConfirmed(null);

        selectedAccountPin.unsubscribe();
        noAccountsSetupPin.unsubscribe();
        accountDisplayPin.unsubscribe();
    }
}
