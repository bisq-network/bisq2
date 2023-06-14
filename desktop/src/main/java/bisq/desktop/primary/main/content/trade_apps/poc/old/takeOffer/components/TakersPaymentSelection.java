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

package bisq.desktop.primary.main.content.trade_apps.poc.old.takeOffer.components;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodUtil;
import bisq.account.payment_method.PaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.currency.Market;
import bisq.common.currency.TradeCurrency;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.poc.PocOffer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

// Copied from PaymentMethodSelection and left the Property data even its mostly derived from offer now 
// Should be changed if we keep it, but as very unsure if that component will survive I leave it as minimal version for 
// now.
@Slf4j
public class TakersPaymentSelection {
    private final Controller controller;

    public TakersPaymentSelection(AccountService accountService) {
        controller = new Controller(accountService);
    }

    public void setOffer(PocOffer offer) {
        controller.setOffer(offer);
    }

    public void setSelectedMarket(Market selectedMarket) {
        controller.setSelectedMarket(selectedMarket);
    }

    public void setDirection(Direction direction) {
        controller.setDirection(direction);
    }

    public void setSelectedProtocolType(TradeProtocolType selectedProtocolType) {
        controller.setSelectedProtocolType(selectedProtocolType);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public ReadOnlyObjectProperty<Account<?, ? extends PaymentMethod<?>>> getSelectedBaseSideAccount() {
        return controller.model.selectedBaseSideAccount;
    }

    public ReadOnlyObjectProperty<Account<?, ? extends PaymentMethod<?>>> getSelectedQuoteSideAccount() {
        return controller.model.selectedQuoteSideAccount;
    }

    public ReadOnlyObjectProperty<PaymentRail> getSelectedBaseSidePaymentMethod() {
        return controller.model.selectedBaseSidePaymentMethod;
    }

    public ReadOnlyObjectProperty<PaymentRail> getSelectedQuoteSidePaymentMethod() {
        return controller.model.selectedQuoteSidePaymentMethod;
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller(AccountService accountService) {
            model = new Model(accountService);
            view = new View(model, this);
        }

        private void setSelectedProtocolType(TradeProtocolType selectedProtocolType) {
            model.selectedProtocolType = selectedProtocolType;
            resetAndApplyData();
        }

        private void setDirection(Direction direction) {
            model.direction = direction;
            updateStrings();
        }

        private void setSelectedMarket(Market selectedMarket) {
            model.selectedMarket = selectedMarket;
            resetAndApplyData();
        }

        private void setOffer(PocOffer offer) {
            model.offer = offer;
            resetAndApplyData();
        }

        private void resetAndApplyData() {
            if (model.offer == null) return;

            if (model.direction == null) return;

            Market market = model.selectedMarket;

            if (market == null) return;

            model.selectedBaseSideAccount.set(null);
            model.selectedQuoteSideAccount.set(null);
            model.selectedBaseSidePaymentMethod.set(null);
            model.selectedQuoteSidePaymentMethod.set(null);

            TradeProtocolType selectedProtocolType = model.selectedProtocolType;
            if (selectedProtocolType == null) {
                model.visibility.set(false);
                return;
            }

            model.visibility.set(true);

            String baseSideCode = market.getBaseCurrencyCode();
            Set<PaymentRail> baseSidePaymentPaymentRailByName = model.offer.getBaseSidePaymentMethodSpecs().stream()
                    .map(PaymentMethodSpec::getPaymentMethodName)
                    .map(method -> PaymentMethodUtil.getPaymentRail(method, baseSideCode))
                    .collect(Collectors.toSet());
            String quoteSideCode = market.getQuoteCurrencyCode();
            Set<PaymentRail> quoteSidePaymentPaymentRailByName = model.offer.getQuoteSidePaymentMethodSpecs().stream()
                    .map(PaymentMethodSpec::getPaymentMethodName)
                    .map(method -> PaymentMethodUtil.getPaymentRail(method, quoteSideCode))
                    .collect(Collectors.toSet());

            model.baseSideAccountObservableList.clear();
            model.baseSideAccountObservableList.setAll(model.accountService.getMatchingAccounts(selectedProtocolType, baseSideCode)
                    .stream()
                    .filter(account -> baseSidePaymentPaymentRailByName.contains(account.getPaymentMethod().getPaymentRail()))
                    .map(AccountListItem::new)
                    .collect(Collectors.toList()));
            if (model.baseSideAccountObservableList.size() == 1) {
                // todo: use last selected from settings if there are multiple
                model.selectedBaseSideAccountListItem.set(model.baseSideAccountObservableList.get(0));
                model.selectedBaseSideAccount.set(model.selectedBaseSideAccountListItem.get().getAccount());
                model.selectedBaseSidePaymentMethod.set(model.selectedBaseSideAccountListItem.get().getPaymentPaymentRail());
            }

            model.quoteSideAccountObservableList.clear();
            model.quoteSideAccountObservableList.setAll(model.accountService.getMatchingAccounts(selectedProtocolType, quoteSideCode)
                    .stream()
                    .filter(account -> quoteSidePaymentPaymentRailByName.contains(account.getPaymentMethod().getPaymentRail()))
                    .map(AccountListItem::new)
                    .collect(Collectors.toList()));
            if (model.quoteSideAccountObservableList.size() == 1) {
                // todo: use last selected from settings if there are multiple
                model.selectedQuoteSideAccountListItem.set(model.quoteSideAccountObservableList.get(0));
                model.selectedQuoteSideAccount.set(model.selectedQuoteSideAccountListItem.get().getAccount());
                model.selectedQuoteSidePaymentMethod.set(model.selectedQuoteSideAccountListItem.get().getPaymentPaymentRail());
            }

            model.baseSidePaymentMethodObservableList.setAll(PaymentMethodUtil.getPaymentRails(selectedProtocolType, baseSideCode)
                    .stream()
                    .filter(baseSidePaymentPaymentRailByName::contains)
                    .map(e -> new PaymentListItem(e, baseSideCode))
                    .collect(Collectors.toList()));
            if (model.baseSidePaymentMethodObservableList.size() == 1) {
                // todo: use last selected from settings if there are multiple
                model.selectedBaseSidePaymentMethodListItem.set(model.baseSidePaymentMethodObservableList.get(0));
                model.selectedBaseSidePaymentMethod.set(model.selectedBaseSidePaymentMethodListItem.get().getPaymentPaymentRail());
            }

            model.quoteSidePaymentMethodObservableList.setAll(PaymentMethodUtil.getPaymentRails(selectedProtocolType, quoteSideCode)
                    .stream()
                    .filter(quoteSidePaymentPaymentRailByName::contains)
                    .map(e -> new PaymentListItem(e, quoteSideCode))
                    .collect(Collectors.toList()));
            if (model.quoteSidePaymentMethodObservableList.size() == 1) {
                // todo: use last selected from settings if there are multiple
                model.selectedQuoteSidePaymentMethodListItem.set(model.quoteSidePaymentMethodObservableList.get(0));
                model.selectedQuoteSidePaymentMethod.set(model.selectedQuoteSidePaymentMethodListItem.get().getPaymentPaymentRail());
            }

            // For Fiat, we show always accounts. If no accounts set up yet the user gets the create-account button 
            // displayed (as prompt in the table view)
            if (TradeCurrency.isFiat(baseSideCode)) {
                model.baseSideAccountsVisibility.set(true);
            } else {
                model.baseSideAccountsVisibility.set(!model.baseSideAccountObservableList.isEmpty());
            }
            if (TradeCurrency.isFiat(quoteSideCode)) {
                model.quoteSideAccountsVisibility.set(true);
            } else {
                model.quoteSideAccountsVisibility.set(!model.quoteSideAccountObservableList.isEmpty());
            }

            model.baseSidePaymentMethodVisibility.set(!model.baseSideAccountsVisibility.get());
            model.quoteSidePaymentMethodVisibility.set(!model.quoteSideAccountsVisibility.get());

            updateStrings();
        }

        private void updateStrings() {
            Direction direction = model.direction;
            if (direction == null) return;

            Market market = model.selectedMarket;
            if (market == null) return;

            String baseSideVerb = direction == Direction.SELL ?
                    Res.get("poc.sending") :
                    Res.get("poc.receiving");
            String quoteSideVerb = direction == Direction.BUY ?
                    Res.get("poc.sending") :
                    Res.get("poc.receiving");

            if (model.baseSideAccountsVisibility.get()) {
                model.baseSideDescription.set(Res.get("takeOffer.account.description",
                        baseSideVerb, market.getBaseCurrencyCode()));
            } else {
                model.baseSideDescription.set(Res.get("takeOffer.paymentMethod.description",
                        baseSideVerb, market.getBaseCurrencyCode()));
            }
            if (model.quoteSideAccountsVisibility.get()) {
                model.quoteSideDescription.set(Res.get("takeOffer.account.description",
                        quoteSideVerb, market.getQuoteCurrencyCode()));
            } else {
                model.quoteSideDescription.set(Res.get("takeOffer.paymentMethod.description",
                        quoteSideVerb, market.getQuoteCurrencyCode()));
            }
        }

        @Override
        public void onActivate() {
            resetAndApplyData();
        }

        @Override
        public void onDeactivate() {
            model.selectedBaseSideAccount.set(null);
            model.selectedQuoteSideAccount.set(null);
            model.selectedBaseSidePaymentMethod.set(null);
            model.selectedQuoteSidePaymentMethod.set(null);
            model.selectedBaseSideAccountListItem.set(null);
            model.selectedQuoteSideAccountListItem.set(null);
            model.selectedBaseSidePaymentMethodListItem.set(null);
            model.selectedQuoteSidePaymentMethodListItem.set(null);
        }

        private void onAccountSelectionChanged(AccountListItem listItem, boolean isBaseSide) {
            if (listItem == null) return;

            var selectedAccount = isBaseSide ?
                    model.selectedBaseSideAccount :
                    model.selectedQuoteSideAccount;
            ObjectProperty<PaymentRail> selectedPaymentMethod = isBaseSide ?
                    model.selectedBaseSidePaymentMethod :
                    model.selectedQuoteSidePaymentMethod;

            selectedAccount.set(listItem.account);
            selectedPaymentMethod.set(listItem.paymentPaymentRail);
        }

        private void onPaymentMethodSelectionChanged(PaymentListItem listItem, boolean isBaseSide) {
            if (listItem == null) return;

            ObjectProperty<PaymentRail> selectedMethod = isBaseSide ?
                    model.selectedBaseSidePaymentMethod :
                    model.selectedQuoteSidePaymentMethod;
            selectedMethod.set(listItem.paymentPaymentRail);
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Account<?, ? extends PaymentMethod<?>>> selectedBaseSideAccount = new SimpleObjectProperty<>();
        private final ObjectProperty<Account<?, ? extends PaymentMethod<?>>> selectedQuoteSideAccount = new SimpleObjectProperty<>();
        private final ObjectProperty<PaymentRail> selectedBaseSidePaymentMethod = new SimpleObjectProperty<>();
        private final ObjectProperty<PaymentRail> selectedQuoteSidePaymentMethod = new SimpleObjectProperty<>();


        private final AccountService accountService;
        private final StringProperty baseSideDescription = new SimpleStringProperty();
        private final StringProperty quoteSideDescription = new SimpleStringProperty();
        private final BooleanProperty visibility = new SimpleBooleanProperty();
        private final BooleanProperty baseSidePaymentMethodVisibility = new SimpleBooleanProperty();
        private final BooleanProperty quoteSidePaymentMethodVisibility = new SimpleBooleanProperty();
        private final BooleanProperty baseSideAccountsVisibility = new SimpleBooleanProperty();
        private final BooleanProperty quoteSideAccountsVisibility = new SimpleBooleanProperty();

        private final ObservableList<AccountListItem> baseSideAccountObservableList = FXCollections.observableArrayList();
        private final SortedList<AccountListItem> baseSideAccountSortedList = new SortedList<>(baseSideAccountObservableList);
        private final ObservableList<AccountListItem> quoteSideAccountObservableList = FXCollections.observableArrayList();
        private final SortedList<AccountListItem> quoteSideAccountSortedList = new SortedList<>(quoteSideAccountObservableList);
        private final ObjectProperty<AccountListItem> selectedBaseSideAccountListItem = new SimpleObjectProperty<>();
        private final ObjectProperty<AccountListItem> selectedQuoteSideAccountListItem = new SimpleObjectProperty<>();

        private final ObservableList<PaymentListItem> baseSidePaymentMethodObservableList = FXCollections.observableArrayList();
        private final SortedList<PaymentListItem> baseSidePaymentMethodSortedList = new SortedList<>(baseSidePaymentMethodObservableList);
        private final ObservableList<PaymentListItem> quoteSidePaymentMethodObservableList = FXCollections.observableArrayList();
        private final SortedList<PaymentListItem> quoteSidePaymentMethodSortedList = new SortedList<>(quoteSidePaymentMethodObservableList);
        private final ObjectProperty<PaymentListItem> selectedBaseSidePaymentMethodListItem = new SimpleObjectProperty<>();
        private final ObjectProperty<PaymentListItem> selectedQuoteSidePaymentMethodListItem = new SimpleObjectProperty<>();
        private Market selectedMarket;
        private Direction direction;
        private TradeProtocolType selectedProtocolType;
        private PocOffer offer;


        private Model(AccountService accountService) {
            this.accountService = accountService;
        }
    }

    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final Label baseSideLabel, quoteSideLabel;
        private final AutoCompleteComboBox<AccountListItem> baseSideAccountsComboBox, quoteSideAccountsComboBox;
        private final AutoCompleteComboBox<PaymentListItem> baseSidePaymentMethodComboBox, quoteSidePaymentMethodComboBox;

        private View(Model model,
                     Controller controller) {
            super(new HBox(), model, controller);
            root.setSpacing(10);

            baseSideLabel = new Label();
            baseSideLabel.getStyleClass().add("titled-group-bg-label-active");

            baseSideAccountsComboBox = new AutoCompleteComboBox<>(model.baseSideAccountSortedList);
            setupAccountStringConverter(baseSideAccountsComboBox);
            VBox.setMargin(baseSideAccountsComboBox, new Insets(0, 0, 20, 0));

            baseSidePaymentMethodComboBox = new AutoCompleteComboBox<>(model.baseSidePaymentMethodSortedList);
            setupPaymentMethodStringConverter(baseSidePaymentMethodComboBox);
            VBox.setMargin(baseSidePaymentMethodComboBox, new Insets(0, 0, 20, 0));

            VBox baseSideBox = new VBox();
            baseSideBox.setSpacing(10);
            baseSideBox.getChildren().addAll(baseSideLabel, baseSideAccountsComboBox, baseSidePaymentMethodComboBox);

            quoteSideLabel = new Label();
            quoteSideLabel.getStyleClass().add("titled-group-bg-label-active");

            quoteSideAccountsComboBox = new AutoCompleteComboBox<>(model.quoteSideAccountSortedList);
            setupAccountStringConverter(quoteSideAccountsComboBox);
            VBox.setMargin(quoteSideAccountsComboBox, new Insets(0, 0, 20, 0));

            quoteSidePaymentMethodComboBox = new AutoCompleteComboBox<>(model.quoteSidePaymentMethodSortedList);
            setupPaymentMethodStringConverter(quoteSidePaymentMethodComboBox);
            VBox.setMargin(quoteSidePaymentMethodComboBox, new Insets(0, 0, 20, 0));

            VBox quoteSideBox = new VBox();
            quoteSideBox.setSpacing(10);
            quoteSideBox.getChildren().addAll(quoteSideLabel, quoteSideAccountsComboBox, quoteSidePaymentMethodComboBox);

            HBox.setHgrow(baseSideBox, Priority.ALWAYS);
            HBox.setHgrow(quoteSideBox, Priority.ALWAYS);
            root.getChildren().addAll(baseSideBox, quoteSideBox);
        }

        @Override
        protected void onViewAttached() {
            baseSideAccountsComboBox.setOnAction(e -> controller.onAccountSelectionChanged(
                    baseSideAccountsComboBox.getSelectionModel().getSelectedItem(), true));
            quoteSideAccountsComboBox.setOnAction(e -> controller.onAccountSelectionChanged(
                    quoteSideAccountsComboBox.getSelectionModel().getSelectedItem(), false));
            baseSidePaymentMethodComboBox.setOnAction(e -> controller.onPaymentMethodSelectionChanged(
                    baseSidePaymentMethodComboBox.getSelectionModel().getSelectedItem(), true));
            quoteSidePaymentMethodComboBox.setOnAction(e -> controller.onPaymentMethodSelectionChanged(
                    quoteSidePaymentMethodComboBox.getSelectionModel().getSelectedItem(), false));

            UIThread.runOnNextRenderFrame(() -> {
                baseSideAccountsComboBox.getSelectionModel().select(model.selectedBaseSideAccountListItem.get());
                quoteSideAccountsComboBox.getSelectionModel().select(model.selectedQuoteSideAccountListItem.get());
                baseSidePaymentMethodComboBox.getSelectionModel().select(model.selectedBaseSidePaymentMethodListItem.get());
                quoteSidePaymentMethodComboBox.getSelectionModel().select(model.selectedQuoteSidePaymentMethodListItem.get());
            });

            baseSideLabel.textProperty().bind(model.baseSideDescription);
            quoteSideLabel.textProperty().bind(model.quoteSideDescription);

            root.visibleProperty().bind(model.visibility);
            root.managedProperty().bind(model.visibility);

            baseSideAccountsComboBox.visibleProperty().bind(model.baseSideAccountsVisibility);
            baseSideAccountsComboBox.managedProperty().bind(model.baseSideAccountsVisibility);
            quoteSideAccountsComboBox.visibleProperty().bind(model.quoteSideAccountsVisibility);
            quoteSideAccountsComboBox.managedProperty().bind(model.quoteSideAccountsVisibility);

            baseSidePaymentMethodComboBox.visibleProperty().bind(model.baseSidePaymentMethodVisibility);
            baseSidePaymentMethodComboBox.managedProperty().bind(model.baseSidePaymentMethodVisibility);
            quoteSidePaymentMethodComboBox.visibleProperty().bind(model.quoteSidePaymentMethodVisibility);
            quoteSidePaymentMethodComboBox.managedProperty().bind(model.quoteSidePaymentMethodVisibility);
        }

        @Override
        protected void onViewDetached() {
            baseSideAccountsComboBox.setOnAction(null);
            quoteSideAccountsComboBox.setOnAction(null);
            baseSidePaymentMethodComboBox.setOnAction(null);
            quoteSidePaymentMethodComboBox.setOnAction(null);

            baseSideAccountsComboBox.getSelectionModel().clearSelection();
            quoteSideAccountsComboBox.getSelectionModel().clearSelection();
            baseSidePaymentMethodComboBox.getSelectionModel().clearSelection();
            quoteSidePaymentMethodComboBox.getSelectionModel().clearSelection();

            baseSideLabel.textProperty().unbind();
            quoteSideLabel.textProperty().unbind();

            root.visibleProperty().unbind();
            root.managedProperty().unbind();

            baseSideAccountsComboBox.visibleProperty().unbind();
            baseSideAccountsComboBox.managedProperty().unbind();
            quoteSideAccountsComboBox.visibleProperty().unbind();
            quoteSideAccountsComboBox.managedProperty().unbind();

            baseSidePaymentMethodComboBox.visibleProperty().unbind();
            baseSidePaymentMethodComboBox.managedProperty().unbind();
            quoteSidePaymentMethodComboBox.visibleProperty().unbind();
            quoteSidePaymentMethodComboBox.managedProperty().unbind();
        }

        private void setupAccountStringConverter(AutoCompleteComboBox<AccountListItem> comboBox) {
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(AccountListItem item) {
                    return item != null ? item.getAccountName() + " / " + item.getPaymentMethodName() : "";
                }

                @Override
                public AccountListItem fromString(String string) {
                    return null;
                }
            });
        }

        private void setupPaymentMethodStringConverter(AutoCompleteComboBox<PaymentListItem> comboBox) {
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(PaymentListItem item) {
                    return item != null ? item.getName() : "";
                }

                @Override
                public PaymentListItem fromString(String string) {
                    return null;
                }
            });
        }
    }

    @Getter
    private static class AccountListItem implements TableItem {
        private final Account<?, ? extends PaymentMethod<?>> account;
        private final String accountName;
        private final PaymentRail paymentPaymentRail;
        private final String paymentMethodName;

        private AccountListItem(Account<?, ? extends PaymentMethod<?>> account) {
            this.account = account;
            accountName = account.getAccountName();
            paymentPaymentRail = account.getPaymentMethod().getPaymentRail();
            paymentMethodName = Res.get(paymentPaymentRail.name());
        }

        @Override
        public void activate() {
        }

        @Override
        public void deactivate() {
        }
    }

    @Getter
    private static class PaymentListItem implements TableItem {
        private final PaymentRail paymentPaymentRail;
        private final String name;

        private PaymentListItem(PaymentRail paymentPaymentRail, String currencyCode) {
            this.paymentPaymentRail = paymentPaymentRail;
            name = paymentPaymentRail.name();
        }

        @Override
        public void activate() {
        }

        @Override
        public void deactivate() {
        }
    }
}