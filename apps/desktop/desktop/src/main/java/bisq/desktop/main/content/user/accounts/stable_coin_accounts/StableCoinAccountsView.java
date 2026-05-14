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

package bisq.desktop.main.content.user.accounts.stable_coin_accounts;

import bisq.account.accounts.stable_coin.StableCoinAccount;
import bisq.desktop.common.threading.UIThread;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class StableCoinAccountsView extends View<VBox, StableCoinAccountsModel, StableCoinAccountsController> {
    private final Label headline;
    private final Button createButtonWithAccounts, createButtonNoAccounts, deleteButton;
    private final AutoCompleteComboBox<StableCoinAccount> accountsComboBox;
    private final HBox comboBoxAndCreateButtonHBox;
    private final VBox noAccountsVBox, accountDetailsVBox;
    private final Region lineAfterHeadline;
    private final Label addressLabel, networkLabel, currencyLabel;
    private Subscription selectedAccountPin, noAccountsSetupPin;

    public StableCoinAccountsView(StableCoinAccountsModel model, StableCoinAccountsController controller) {
        super(new VBox(0), model, controller);

        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(0, 40, 40, 40));

        headline = new Label(Res.get("user.stableCoinAccounts.headline"));
        headline.getStyleClass().add("large-thin-headline");

        Label noAccountsInfo = new Label(Res.get("user.stableCoinAccounts.noAccounts"));
        noAccountsInfo.setWrapText(true);
        noAccountsInfo.getStyleClass().add("user-payment-account-no-data");
        noAccountsVBox = new VBox(20, noAccountsInfo);

        createButtonNoAccounts = new Button(Res.get("user.stableCoinAccounts.createAccount"));
        createButtonNoAccounts.setDefaultButton(true);

        createButtonWithAccounts = new Button(Res.get("user.stableCoinAccounts.createAccount"));
        createButtonWithAccounts.getStyleClass().add("outlined-button");

        accountsComboBox = new AutoCompleteComboBox<>(model.getSortedAccounts(), Res.get("paymentAccounts.selectAccount"));
        accountsComboBox.setPrefWidth(325);
        accountsComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(StableCoinAccount account) {
                return account != null ? account.getAccountName() : "";
            }

            @Override
            public StableCoinAccount fromString(String string) {
                return null;
            }
        });

        comboBoxAndCreateButtonHBox = new HBox(20, accountsComboBox, Spacer.fillHBox(), createButtonWithAccounts);

        addressLabel = new Label();
        addressLabel.setWrapText(true);
        addressLabel.getStyleClass().add("user-content-text");

        networkLabel = new Label();
        networkLabel.getStyleClass().add("user-content-text");

        currencyLabel = new Label();
        currencyLabel.getStyleClass().add("user-content-text");

        accountDetailsVBox = new VBox(10,
                createDetailRow(Res.get("user.stableCoinAccounts.create.summary.address"), addressLabel),
                createDetailRow(Res.get("user.stableCoinAccounts.create.summary.network"), networkLabel),
                createDetailRow(Res.get("user.stableCoinAccounts.create.summary.currency"), currencyLabel));

        deleteButton = new Button(Res.get("user.stableCoinAccounts.deleteAccount"));

        VBox contentBox = new VBox(30);
        lineAfterHeadline = SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing());
        contentBox.getChildren().addAll(headline, lineAfterHeadline,
                noAccountsVBox, createButtonNoAccounts,
                comboBoxAndCreateButtonHBox, accountDetailsVBox, deleteButton);
        contentBox.getStyleClass().add("bisq-common-bg");

        root.getChildren().add(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
        deleteButton.disableProperty().bind(model.getDeleteButtonDisabled());

        noAccountsSetupPin = EasyBind.subscribe(model.getNoAccountsAvailable(), noAccounts -> {
            boolean anyAccount = !noAccounts;
            lineAfterHeadline.setVisible(anyAccount);
            lineAfterHeadline.setManaged(anyAccount);
            headline.setVisible(anyAccount);
            headline.setManaged(anyAccount);
            noAccountsVBox.setVisible(noAccounts);
            noAccountsVBox.setManaged(noAccounts);
            createButtonNoAccounts.setVisible(noAccounts);
            createButtonNoAccounts.setManaged(noAccounts);
            comboBoxAndCreateButtonHBox.setVisible(anyAccount);
            comboBoxAndCreateButtonHBox.setManaged(anyAccount);
            accountDetailsVBox.setVisible(anyAccount);
            accountDetailsVBox.setManaged(anyAccount);
            deleteButton.setVisible(anyAccount);
            deleteButton.setManaged(anyAccount);
        });

        selectedAccountPin = EasyBind.subscribe(model.getSelectedAccount(), account -> {
            UIThread.runOnNextRenderFrame(() -> accountsComboBox.getSelectionModel().select(account));
            if (account != null) {
                addressLabel.setText(account.getAccountPayload().getAddress());
                networkLabel.setText(account.getAccountPayload().getNetwork());
                currencyLabel.setText(account.getAccountPayload().getCurrencyCode());
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
        deleteButton.setOnAction(e -> controller.onDeleteAccount());
    }

    @Override
    protected void onViewDetached() {
        deleteButton.disableProperty().unbind();
        noAccountsSetupPin.unsubscribe();
        selectedAccountPin.unsubscribe();
        accountsComboBox.setOnChangeConfirmed(null);
        createButtonNoAccounts.setOnAction(null);
        createButtonWithAccounts.setOnAction(null);
        deleteButton.setOnAction(null);
    }

    private HBox createDetailRow(String labelText, Label valueLabel) {
        Label title = new Label(labelText + ":");
        title.getStyleClass().add("user-content-text");
        title.setMinWidth(100);
        return new HBox(10, title, valueLabel);
    }
}
