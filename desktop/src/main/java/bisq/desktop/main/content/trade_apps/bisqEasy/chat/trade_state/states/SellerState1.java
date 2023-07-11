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

package bisq.desktop.main.content.trade_apps.bisqEasy.chat.trade_state.states;

import bisq.account.accounts.Account;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.BisqText;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SellerState1 extends BaseState {
    private final Controller controller;

    public SellerState1(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Pin accountsPin, selectedAccountPin;

        private Controller(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            return new Model(bisqEasyTrade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();

            model.getSortedAccounts().setComparator(Comparator.comparing(Account::getAccountName));

            accountsPin = accountService.getAccounts().addListener(() -> {
                List<UserDefinedFiatAccount> accounts = accountService.getAccounts().stream()
                        .filter(account -> account instanceof UserDefinedFiatAccount)
                        .map(account -> (UserDefinedFiatAccount) account)
                        .collect(Collectors.toList());
                model.setAllAccounts(accounts);
                model.getAccountSelectionVisible().set(accounts.size() > 1);
                maybeSelectFirstAccount();
            });
            selectedAccountPin = accountService.selectedAccountAsObservable().addObserver(account -> {
                UIThread.run(() -> {
                    if (account instanceof UserDefinedFiatAccount) {
                        UserDefinedFiatAccount userDefinedFiatAccount = (UserDefinedFiatAccount) account;
                        model.selectedAccountProperty().set(userDefinedFiatAccount);
                        model.getPaymentAccountData().set(userDefinedFiatAccount.getAccountPayload().getAccountData());
                    }
                });
            });

            model.getButtonDisabled().bind(model.getPaymentAccountData().isEmpty());
            findUsersAccountData().ifPresent(accountData -> model.getPaymentAccountData().set(accountData));
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
            accountsPin.unbind();
            selectedAccountPin.unbind();
            model.getButtonDisabled().unbind();
        }

        private void onSendPaymentData() {
            String message = Res.get("bisqEasy.tradeState.info.seller.phase1.chatBotMessage", model.getPaymentAccountData().get());
            sendChatBotMessage(message);
            try {
                bisqEasyTradeService.sellerSendsPaymentAccount(model.getBisqEasyTrade(), model.getPaymentAccountData().get());
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }

        private void onSelectAccount(UserDefinedFiatAccount account) {
            if (account != null) {
                accountService.setSelectedAccount(account);
            }
        }

        private void maybeSelectFirstAccount() {
            if (!model.getSortedAccounts().isEmpty() && accountService.getSelectedAccount() == null) {
                accountService.setSelectedAccount(model.getSortedAccounts().get(0));
            }
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        private final StringProperty paymentAccountData = new SimpleStringProperty();
        private final BooleanProperty buttonDisabled = new SimpleBooleanProperty();
        private final BooleanProperty accountSelectionVisible = new SimpleBooleanProperty();
        private final ObservableList<UserDefinedFiatAccount> accounts = FXCollections.observableArrayList();
        private final SortedList<UserDefinedFiatAccount> sortedAccounts = new SortedList<>(accounts);
        private final ObjectProperty<UserDefinedFiatAccount> selectedAccount = new SimpleObjectProperty<>();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(bisqEasyTrade, channel);
        }

        @Nullable
        public UserDefinedFiatAccount getSelectedAccount() {
            return selectedAccount.get();
        }

        public ObjectProperty<UserDefinedFiatAccount> selectedAccountProperty() {
            return selectedAccount;
        }

        public void setAllAccounts(Collection<UserDefinedFiatAccount> collection) {
            accounts.setAll(collection);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button button;
        private final MaterialTextArea paymentAccountData;
        private final AutoCompleteComboBox<UserDefinedFiatAccount> accountSelection;
        private Subscription selectedAccountPin;

        private View(Model model, Controller controller) {
            super(model, controller);

            BisqText infoHeadline = new BisqText(Res.get("bisqEasy.tradeState.info.seller.phase1.headline"));
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");

            accountSelection = new AutoCompleteComboBox<>(model.getSortedAccounts(), Res.get("user.paymentAccounts.selectAccount"));
            accountSelection.setPrefWidth(300);
            accountSelection.setConverter(new StringConverter<>() {
                @Override
                public String toString(UserDefinedFiatAccount object) {
                    return object != null ? object.getAccountName() : "";
                }

                @Override
                public UserDefinedFiatAccount fromString(String string) {
                    return null;
                }
            });

            paymentAccountData = FormUtils.addTextArea(Res.get("bisqEasy.tradeState.info.seller.phase1.accountData"), "", true);
            paymentAccountData.setPromptText(Res.get("bisqEasy.tradeState.info.seller.phase1.accountData.prompt"));

            button = new Button(Res.get("bisqEasy.tradeState.info.seller.phase1.buttonText"));
            button.setDefaultButton(true);

            Label helpLabel = FormUtils.getHelpLabel(Res.get("bisqEasy.tradeState.info.seller.phase1.note"));

            VBox.setMargin(button, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(
                    infoHeadline,
                    accountSelection,
                    paymentAccountData,
                    button,
                    Spacer.fillVBox(),
                    helpLabel);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            paymentAccountData.textProperty().bindBidirectional(model.getPaymentAccountData());
            button.disableProperty().bind(model.getButtonDisabled());
            accountSelection.visibleProperty().bind(model.getAccountSelectionVisible());
            accountSelection.managedProperty().bind(model.getAccountSelectionVisible());

            selectedAccountPin = EasyBind.subscribe(model.selectedAccountProperty(),
                    accountName -> accountSelection.getSelectionModel().select(accountName));

            accountSelection.setOnChangeConfirmed(e -> {
                if (accountSelection.getSelectionModel().getSelectedItem() == null) {
                    accountSelection.getSelectionModel().select(model.getSelectedAccount());
                    return;
                }
                controller.onSelectAccount(accountSelection.getSelectionModel().getSelectedItem());
            });

            button.setOnAction(e -> controller.onSendPaymentData());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            paymentAccountData.textProperty().unbindBidirectional(model.getPaymentAccountData());
            button.disableProperty().unbind();
            accountSelection.visibleProperty().unbind();
            accountSelection.managedProperty().unbind();

            selectedAccountPin.unsubscribe();

            accountSelection.setOnChangeConfirmed(null);
            button.setOnAction(null);
        }
    }
}