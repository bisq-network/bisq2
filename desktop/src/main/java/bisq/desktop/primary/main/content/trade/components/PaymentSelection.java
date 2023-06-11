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

package bisq.desktop.primary.main.content.trade.components;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodUtil;
import bisq.account.payment_method.PaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.currency.Market;
import bisq.common.currency.TradeCurrency;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.offer.Direction;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class PaymentSelection {
    private final Controller controller;

    public PaymentSelection(AccountService accountService) {
        controller = new Controller(accountService);
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

    public ObservableSet<Account<?, ? extends PaymentMethod<?>>> getSelectedBaseSideAccounts() {
        return controller.model.selectedBaseSideAccounts;
    }

    public ObservableSet<Account<?, ? extends PaymentMethod<?>>> getSelectedQuoteSideAccounts() {
        return controller.model.selectedQuoteSideAccounts;
    }

    public ObservableSet<PaymentRail> getSelectedBaseSidePaymentMethods() {
        return controller.model.selectedBaseSidePaymentPaymentRails;
    }

    public ObservableSet<PaymentRail> getSelectedQuoteSidePaymentMethods() {
        return controller.model.selectedQuoteSidePaymentPaymentRails;
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller(AccountService accountService) {
            model = new Model(accountService);
            view = new View(model, this);
        }

        private void setDirection(Direction direction) {
            model.direction = direction;
            updateStrings();
        }

        private void setSelectedMarket(Market selectedMarket) {
            model.selectedMarket = selectedMarket;
            resetAndApplyData();
        }

        private void setSelectedProtocolType(TradeProtocolType selectedProtocolType) {
            model.selectedProtocolType = selectedProtocolType;
            resetAndApplyData();
        }

        private void resetAndApplyData() {
            if (model.direction == null) return;

            Market market = model.selectedMarket;

            if (market == null) return;

            model.selectedBaseSideAccounts.clear();
            model.selectedQuoteSideAccounts.clear();
            model.selectedBaseSidePaymentPaymentRails.clear();
            model.selectedQuoteSidePaymentPaymentRails.clear();

            TradeProtocolType selectedProtocolTyp = model.selectedProtocolType;
            if (selectedProtocolTyp == null) {
                model.visibility.set(false);
                return;
            }

            model.visibility.set(true);

            model.baseSideAccountObservableList.setAll(model.accountService.getMatchingAccounts(selectedProtocolTyp, market.getBaseCurrencyCode())
                    .stream()
                    .map(AccountListItem::new)
                    .collect(Collectors.toList()));
            List<AccountListItem> collect = model.accountService.getMatchingAccounts(selectedProtocolTyp, market.getQuoteCurrencyCode())
                    .stream()
                    .map(AccountListItem::new)
                    .collect(Collectors.toList());
            model.quoteSideAccountObservableList.clear();
            model.quoteSideAccountObservableList.setAll(collect);

            model.baseSidePaymentObservableList.setAll(PaymentMethodUtil.getPaymentMethods(selectedProtocolTyp, market.getBaseCurrencyCode())
                    .stream()
                    .map(e -> new PaymentListItem(e, market.getBaseCurrencyCode()))
                    .collect(Collectors.toList()));
            model.quoteSidePaymentObservableList.setAll(PaymentMethodUtil.getPaymentMethods(selectedProtocolTyp, market.getQuoteCurrencyCode())
                    .stream()
                    .map(e -> new PaymentListItem(e, market.getQuoteCurrencyCode()))
                    .collect(Collectors.toList()));

            // For Fiat, we show always accounts. If no accounts set up yet the user gets the create-account button 
            // displayed (as prompt in the table view)
            if (TradeCurrency.isFiat(market.getBaseCurrencyCode())) {
                model.baseSideAccountsVisibility.set(true);
            } else {
                model.baseSideAccountsVisibility.set(!model.baseSideAccountObservableList.isEmpty());
            }
            if (TradeCurrency.isFiat(market.getQuoteCurrencyCode())) {
                model.quoteSideAccountsVisibility.set(true);
            } else {
                model.quoteSideAccountsVisibility.set(!model.quoteSideAccountObservableList.isEmpty());
            }

            model.baseSidePaymentVisibility.set(!model.baseSideAccountsVisibility.get());
            model.quoteSidePaymentVisibility.set(!model.quoteSideAccountsVisibility.get());

            updateStrings();
        }

        private void updateStrings() {
            Direction direction = model.direction;
            if (direction == null) return;

            Market market = model.selectedMarket;
            if (market == null) return;

            String baseSideVerb = direction == Direction.SELL ?
                    Res.get("sending") :
                    Res.get("receiving");
            String quoteSideVerb = direction == Direction.BUY ?
                    Res.get("sending") :
                    Res.get("receiving");

            if (model.baseSideAccountsVisibility.get()) {
                model.baseSideDescription.set(Res.get("createOffer.account.description",
                        baseSideVerb, market.getBaseCurrencyCode()));
            } else {
                model.baseSideDescription.set(Res.get("createOffer.paymentMethod.description",
                        baseSideVerb, market.getBaseCurrencyCode()));
            }
            if (model.quoteSideAccountsVisibility.get()) {
                model.quoteSideDescription.set(Res.get("createOffer.account.description",
                        quoteSideVerb, market.getQuoteCurrencyCode()));
            } else {
                model.quoteSideDescription.set(Res.get("createOffer.paymentMethod.description",
                        quoteSideVerb, market.getQuoteCurrencyCode()));
            }
        }

        @Override
        public void onActivate() {
            resetAndApplyData();
        }

        @Override
        public void onDeactivate() {
            model.selectedBaseSideAccounts.clear();
            model.selectedQuoteSideAccounts.clear();
        }

        private void onAccountSelectionChanged(AccountListItem listItem, boolean selected, boolean isBaseSide) {
            var observableAccountsSet = isBaseSide ?
                    model.selectedBaseSideAccounts :
                    model.selectedQuoteSideAccounts;
            ObservableSet<PaymentRail> observablePaymentMethodsSet = isBaseSide ?
                    model.selectedBaseSidePaymentPaymentRails :
                    model.selectedQuoteSidePaymentPaymentRails;
            if (selected) {
                observableAccountsSet.add(listItem.account);
                observablePaymentMethodsSet.add(listItem.paymnetPaymentRail);
            } else {
                observableAccountsSet.remove(listItem.account);
                observablePaymentMethodsSet.remove(listItem.paymnetPaymentRail);
            }
        }

        private void onPaymentSelectionChanged(PaymentListItem listItem, boolean selected, boolean isBaseSide) {
            ObservableSet<PaymentRail> observableSet = isBaseSide ?
                    model.selectedBaseSidePaymentPaymentRails :
                    model.selectedQuoteSidePaymentPaymentRails;
            if (selected) {
                observableSet.add(listItem.paymentPaymentRail);
            } else {
                observableSet.remove(listItem.paymentPaymentRail);
            }
        }

        private void onCreateBaseSideAccount() {

        }

        private void onCreateQuoteSideAccount() {

        }


    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObservableSet<Account<?, ? extends PaymentMethod<?>>> selectedBaseSideAccounts = FXCollections.observableSet(new HashSet<>());
        private final ObservableSet<Account<?, ? extends PaymentMethod<?>>> selectedQuoteSideAccounts = FXCollections.observableSet(new HashSet<>());
        private final ObservableSet<PaymentRail> selectedBaseSidePaymentPaymentRails = FXCollections.observableSet(new HashSet<>());
        private final ObservableSet<PaymentRail> selectedQuoteSidePaymentPaymentRails = FXCollections.observableSet(new HashSet<>());

        private final AccountService accountService;
        private final StringProperty baseSideDescription = new SimpleStringProperty();
        private final StringProperty quoteSideDescription = new SimpleStringProperty();
        private final BooleanProperty visibility = new SimpleBooleanProperty();
        private final BooleanProperty baseSidePaymentVisibility = new SimpleBooleanProperty();
        private final BooleanProperty quoteSidePaymentVisibility = new SimpleBooleanProperty();
        private final BooleanProperty baseSideAccountsVisibility = new SimpleBooleanProperty();
        private final BooleanProperty quoteSideAccountsVisibility = new SimpleBooleanProperty();

        private final ObservableList<AccountListItem> baseSideAccountObservableList = FXCollections.observableArrayList();
        private final SortedList<AccountListItem> baseSideAccountSortedList = new SortedList<>(baseSideAccountObservableList);
        private final ObservableList<AccountListItem> quoteSideAccountObservableList = FXCollections.observableArrayList();
        private final SortedList<AccountListItem> quoteSideAccountSortedList = new SortedList<>(quoteSideAccountObservableList);

        private final ObservableList<PaymentListItem> baseSidePaymentObservableList = FXCollections.observableArrayList();
        private final SortedList<PaymentListItem> baseSidePaymentSortedList = new SortedList<>(baseSidePaymentObservableList);
        private final ObservableList<PaymentListItem> quoteSidePaymentObservableList = FXCollections.observableArrayList();
        private final SortedList<PaymentListItem> quoteSidePaymentSortedList = new SortedList<>(quoteSidePaymentObservableList);

        private Market selectedMarket;
        private Direction direction;
        private TradeProtocolType selectedProtocolType;

        private Model(AccountService accountService) {
            this.accountService = accountService;
        }
    }

    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final Label baseSideLabel, quoteSideLabel;
        private final BisqTableView<AccountListItem> baseSideAccountsTableView, quoteSideAccountsTableView;
        private final BisqTableView<PaymentListItem> baseSidePaymentTableView, quoteSidePaymentTableView;
        private final Button baseSideButton, quoteSideButton;

        private View(Model model,
                     Controller controller) {
            super(new HBox(), model, controller);
            root.setSpacing(10);

            baseSideLabel = new Label();
            baseSideLabel.getStyleClass().add("titled-group-bg-label-active");

            baseSideAccountsTableView = new BisqTableView<>(model.baseSideAccountSortedList);
            int tableHeight = 210;
            baseSideAccountsTableView.setFixHeight(tableHeight);
            configAccountTableView(baseSideAccountsTableView, true);
            VBox.setMargin(baseSideAccountsTableView, new Insets(0, 0, 20, 0));
            baseSideButton = new Button(Res.get("createOffer.account.createNew"));
            VBox baseSidePlaceHolderBox = createPlaceHolderBox(baseSideButton);
            baseSideAccountsTableView.setPlaceholder(baseSidePlaceHolderBox);

            baseSidePaymentTableView = new BisqTableView<>(model.baseSidePaymentSortedList);
            baseSidePaymentTableView.setFixHeight(tableHeight);
            configPaymentTableView(baseSidePaymentTableView, true);
            VBox.setMargin(baseSidePaymentTableView, new Insets(0, 0, 20, 0));

            VBox baseSideBox = new VBox();
            baseSideBox.setSpacing(10);
            baseSideBox.getChildren().addAll(baseSideLabel, baseSideAccountsTableView, baseSidePaymentTableView);

            quoteSideLabel = new Label();
            quoteSideLabel.getStyleClass().add("titled-group-bg-label-active");

            quoteSideAccountsTableView = new BisqTableView<>(model.quoteSideAccountSortedList);
            quoteSideAccountsTableView.setFixHeight(tableHeight);
            configAccountTableView(quoteSideAccountsTableView, false);
            VBox.setMargin(quoteSideAccountsTableView, new Insets(0, 0, 20, 0));
            quoteSideButton = new Button(Res.get("createOffer.account.createNew"));
            VBox quoteSidePlaceHolderBox = createPlaceHolderBox(quoteSideButton);
            quoteSideAccountsTableView.setPlaceholder(quoteSidePlaceHolderBox);

            quoteSidePaymentTableView = new BisqTableView<>(model.quoteSidePaymentSortedList);
            quoteSidePaymentTableView.setFixHeight(tableHeight);
            configPaymentTableView(quoteSidePaymentTableView, false);
            VBox.setMargin(quoteSidePaymentTableView, new Insets(0, 0, 20, 0));

            VBox quoteSideBox = new VBox();
            quoteSideBox.setSpacing(10);
            quoteSideBox.getChildren().addAll(quoteSideLabel, quoteSideAccountsTableView, quoteSidePaymentTableView);

            HBox.setHgrow(baseSideBox, Priority.ALWAYS);
            HBox.setHgrow(quoteSideBox, Priority.ALWAYS);
            root.getChildren().addAll(baseSideBox, quoteSideBox);
        }

        @Override
        protected void onViewAttached() {
            baseSideButton.setOnAction(e -> controller.onCreateBaseSideAccount());
            quoteSideButton.setOnAction(e -> controller.onCreateQuoteSideAccount());

            baseSideLabel.textProperty().bind(model.baseSideDescription);
            quoteSideLabel.textProperty().bind(model.quoteSideDescription);

            root.visibleProperty().bind(model.visibility);
            root.managedProperty().bind(model.visibility);

            baseSideAccountsTableView.visibleProperty().bind(model.baseSideAccountsVisibility);
            baseSideAccountsTableView.managedProperty().bind(model.baseSideAccountsVisibility);
            quoteSideAccountsTableView.visibleProperty().bind(model.quoteSideAccountsVisibility);
            quoteSideAccountsTableView.managedProperty().bind(model.quoteSideAccountsVisibility);

            baseSidePaymentTableView.visibleProperty().bind(model.baseSidePaymentVisibility);
            baseSidePaymentTableView.managedProperty().bind(model.baseSidePaymentVisibility);
            quoteSidePaymentTableView.visibleProperty().bind(model.quoteSidePaymentVisibility);
            quoteSidePaymentTableView.managedProperty().bind(model.quoteSidePaymentVisibility);
        }

        @Override
        protected void onViewDetached() {
            baseSideButton.setOnAction(null);
            quoteSideButton.setOnAction(null);

            baseSideLabel.textProperty().unbind();
            quoteSideLabel.textProperty().unbind();

            root.visibleProperty().unbind();
            root.managedProperty().unbind();

            baseSideAccountsTableView.visibleProperty().unbind();
            baseSideAccountsTableView.managedProperty().unbind();
            quoteSideAccountsTableView.visibleProperty().unbind();
            quoteSideAccountsTableView.managedProperty().unbind();

            baseSidePaymentTableView.visibleProperty().unbind();
            baseSidePaymentTableView.managedProperty().unbind();
            quoteSidePaymentTableView.visibleProperty().unbind();
            quoteSidePaymentTableView.managedProperty().unbind();
        }

        private VBox createPlaceHolderBox(Button baseSideButton) {
            Label placeholderLabel = new Label(Res.get("createOffer.account.placeholder.noAccounts"));
            VBox vBox = new VBox();
            vBox.setSpacing(10);
            vBox.getChildren().addAll(placeholderLabel, baseSideButton);
            vBox.setAlignment(Pos.CENTER);
            return vBox;
        }

        private void configAccountTableView(BisqTableView<AccountListItem> tableView, boolean isBaseSide) {
            tableView.getColumns().add(new BisqTableColumn.Builder<AccountListItem>()
                    .title(Res.get("createOffer.account.table.accountName"))
                    .minWidth(120)
                    .valueSupplier(AccountListItem::getAccountName)
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<AccountListItem>()
                    .title(Res.get("createOffer.account.table.method"))
                    .minWidth(120)
                    .valueSupplier(AccountListItem::getPaymentMethodName)
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<AccountListItem>()
                    .title(Res.get("createOffer.account.table.select"))
                    .minWidth(40)
                    .defaultCellFactory(BisqTableColumn.DefaultCellFactory.CHECKBOX)
                    .toggleHandler((item, selected) -> controller.onAccountSelectionChanged(item, selected, isBaseSide))
                    .build());
        }

        private void configPaymentTableView(BisqTableView<PaymentListItem> tableView, boolean isBaseSide) {
            tableView.getColumns().add(new BisqTableColumn.Builder<PaymentListItem>()
                    .title(Res.get("createOffer.account.table.method"))
                    .minWidth(150)
                    .valueSupplier(PaymentListItem::getName)
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<PaymentListItem>()
                    .title(Res.get("createOffer.account.table.select"))
                    .minWidth(40)
                    .defaultCellFactory(BisqTableColumn.DefaultCellFactory.CHECKBOX)
                    .toggleHandler((item, selected) -> controller.onPaymentSelectionChanged(item, selected, isBaseSide))
                    .build());
        }
    }

    @Getter
    private static class AccountListItem implements TableItem {
        private final Account<?, ? extends PaymentMethod<?>> account;
        private final String accountName;
        private final PaymentRail paymnetPaymentRail;
        private final String paymentMethodName;

        private AccountListItem(Account<?, ? extends PaymentMethod<?>> account) {
            this.account = account;
            accountName = account.getAccountName();
            paymnetPaymentRail = account.getPaymentMethod().getPaymentRail();
            paymentMethodName = Res.get(paymnetPaymentRail.name());
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