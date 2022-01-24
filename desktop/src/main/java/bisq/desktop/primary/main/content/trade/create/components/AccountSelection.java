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

package bisq.desktop.primary.main.content.trade.create.components;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.protocol.SwapProtocolType;
import bisq.account.settlement.SettlementMethod;
import bisq.common.currency.TradeCurrency;
import bisq.common.monetary.Market;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.offer.Direction;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class AccountSelection {
    public static class AccountController implements Controller {
        private final AccountModel model;
        @Getter
        private final AccountView view;
        private final ChangeListener<SwapProtocolType> selectedProtocolListener;
        private final ChangeListener<Direction> directionListener;
        private final ChangeListener<Market> selectedMarketListener;

        public AccountController(OfferPreparationModel offerPreparationModel, AccountService accountService) {
            model = new AccountModel(offerPreparationModel, accountService);
            view = new AccountView(model, this);

            selectedProtocolListener = (observable, oldValue, newValue) -> resetAndApplyData();
            directionListener = (observable, oldValue, newValue) -> updateStrings();
            selectedMarketListener = (observable, oldValue, newValue) -> resetAndApplyData();
        }

        private void resetAndApplyData() {
            Direction direction = model.getDirection();
            if (direction == null) return;

            Market market = model.getSelectedMarket();

            if (market == null) return;

            model.getSelectedBaseSideAccounts().clear();
            model.getSelectedQuoteSideAccounts().clear();
            model.getSelectedBaseSideSettlementMethods().clear();
            model.getSelectedQuoteSideSettlementMethods().clear();

            SwapProtocolType selectedProtocolTyp = model.getSelectedProtocolType();
            if (selectedProtocolTyp == null) {
                model.visibility.set(false);
                return;
            }

            model.visibility.set(true);

            model.baseSideAccountObservableList.setAll(model.accountService.getMatchingAccounts(selectedProtocolTyp, market.baseCurrencyCode())
                    .stream()
                    .map(AccountListItem::new)
                    .collect(Collectors.toList()));
            List<AccountListItem> collect = model.accountService.getMatchingAccounts(selectedProtocolTyp, market.quoteCurrencyCode())
                    .stream()
                    .map(AccountListItem::new)
                    .collect(Collectors.toList());
            model.quoteSideAccountObservableList.clear();
            model.quoteSideAccountObservableList.setAll(collect);

            model.baseSideSettlementObservableList.setAll(SettlementMethod.from(selectedProtocolTyp, market.baseCurrencyCode())
                    .stream()
                    .map(e -> new SettlementListItem(e, market.baseCurrencyCode()))
                    .collect(Collectors.toList()));
            model.quoteSideSettlementObservableList.setAll(SettlementMethod.from(selectedProtocolTyp, market.quoteCurrencyCode())
                    .stream()
                    .map(e -> new SettlementListItem(e, market.quoteCurrencyCode()))
                    .collect(Collectors.toList()));

            // For Fiat we show always accounts. If no accounts set up yet the user gets the create-account button 
            // displayed (as prompt in the table view)
            if (TradeCurrency.isFiat(market.baseCurrencyCode())) {
                model.baseSideAccountsVisibility.set(true);
            } else {
                model.baseSideAccountsVisibility.set(!model.baseSideAccountObservableList.isEmpty());
            }
            if (TradeCurrency.isFiat(market.quoteCurrencyCode())) {
                model.quoteSideAccountsVisibility.set(true);
            } else {
                model.quoteSideAccountsVisibility.set(!model.quoteSideAccountObservableList.isEmpty());
            }

            // If no account is visible we show the settlement
            model.baseSideSettlementVisibility.set(!model.baseSideAccountsVisibility.get());
            model.quoteSideSettlementVisibility.set(!model.quoteSideAccountsVisibility.get());

            updateStrings();
        }

        private void updateStrings() {
            Direction direction = model.getDirection();
            if (direction == null) return;

            Market market = model.getSelectedMarket();
            if (market == null) return;

            String baseSideVerb = direction == Direction.SELL ?
                    Res.offerbook.get("sending") :
                    Res.offerbook.get("receiving");
            String quoteSideVerb = direction == Direction.BUY ?
                    Res.offerbook.get("sending") :
                    Res.offerbook.get("receiving");

            if (model.baseSideAccountsVisibility.get()) {
                model.baseSideDescription.set(Res.offerbook.get("createOffer.account.description",
                        baseSideVerb, market.baseCurrencyCode()));
            } else {
                model.baseSideDescription.set(Res.offerbook.get("createOffer.settlement.description",
                        baseSideVerb, market.baseCurrencyCode()));
            }
            if (model.quoteSideAccountsVisibility.get()) {
                model.quoteSideDescription.set(Res.offerbook.get("createOffer.account.description",
                        quoteSideVerb, market.quoteCurrencyCode()));
            } else {
                model.quoteSideDescription.set(Res.offerbook.get("createOffer.settlement.description",
                        quoteSideVerb, market.quoteCurrencyCode()));
            }
        }

        public void onViewAttached() {
            resetAndApplyData();
            model.selectedProtocolTypeProperty().addListener(selectedProtocolListener);
            model.selectedMarketProperty().addListener(selectedMarketListener);
            model.directionProperty().addListener(directionListener);
        }

        public void onViewDetached() {
            model.selectedProtocolTypeProperty().removeListener(selectedProtocolListener);
            model.selectedMarketProperty().removeListener(selectedMarketListener);
            model.directionProperty().removeListener(directionListener);
            model.getSelectedBaseSideAccounts().clear();
            model.getSelectedQuoteSideAccounts().clear();
        }

        public void onAccountSelectionChanged(AccountListItem listItem, boolean selected, boolean isBaseSide) {
            var observableSet = isBaseSide ?
                    model.getSelectedBaseSideAccounts() :
                    model.getSelectedQuoteSideAccounts();
            if (selected) {
                observableSet.add(listItem.account);
            } else {
                observableSet.remove(listItem.account);
            }
        }

        public void onSettlementSelectionChanged(SettlementListItem listItem, boolean selected, boolean isBaseSide) {
            var observableSet = isBaseSide ?
                    model.getSelectedBaseSideSettlementMethods() :
                    model.getSelectedQuoteSideSettlementMethods();
            if (selected) {
                observableSet.add(listItem.settlementMethod);
            } else {
                observableSet.remove(listItem.settlementMethod);
            }
        }

        public void onCreateBaseSideAccount() {

        }

        public void onCreateQuoteSideAccount() {

        }


    }

    private static class AccountModel implements Model {
        @Delegate
        private final OfferPreparationModel offerPreparationModel;
        private final AccountService accountService;
        private final StringProperty baseSideDescription = new SimpleStringProperty();
        private final StringProperty quoteSideDescription = new SimpleStringProperty();
        private final BooleanProperty visibility = new SimpleBooleanProperty();
        private final BooleanProperty baseSideSettlementVisibility = new SimpleBooleanProperty();
        private final BooleanProperty quoteSideSettlementVisibility = new SimpleBooleanProperty();
        private final BooleanProperty baseSideAccountsVisibility = new SimpleBooleanProperty();
        private final BooleanProperty quoteSideAccountsVisibility = new SimpleBooleanProperty();


        private final ObservableList<AccountListItem> baseSideAccountObservableList = FXCollections.observableArrayList();
        private final SortedList<AccountListItem> baseSideAccountSortedList = new SortedList<>(baseSideAccountObservableList);
        private final ObservableList<AccountListItem> quoteSideAccountObservableList = FXCollections.observableArrayList();
        private final SortedList<AccountListItem> quoteSideAccountSortedList = new SortedList<>(quoteSideAccountObservableList);

        private final ObservableList<SettlementListItem> baseSideSettlementObservableList = FXCollections.observableArrayList();
        private final SortedList<SettlementListItem> baseSideSettlementSortedList = new SortedList<>(baseSideSettlementObservableList);
        private final ObservableList<SettlementListItem> quoteSideSettlementObservableList = FXCollections.observableArrayList();
        private final SortedList<SettlementListItem> quoteSideSettlementSortedList = new SortedList<>(quoteSideSettlementObservableList);


        public AccountModel(OfferPreparationModel offerPreparationModel, AccountService accountService) {
            this.offerPreparationModel = offerPreparationModel;
            this.accountService = accountService;
        }
    }

    public static class AccountView extends View<HBox, AccountModel, AccountController> {
        private final BisqLabel baseSideLabel, quoteSideLabel;
        private final BisqTableView<AccountListItem> baseSideAccountsTableView, quoteSideAccountsTableView;
        private final BisqTableView<SettlementListItem> baseSideSettlementTableView, quoteSideSettlementTableView;
        private final BisqButton baseSideButton, quoteSideButton;
        private final VBox baseSideBox, quoteSideBox;

        public AccountView(AccountModel model,
                           AccountController controller) {
            super(new HBox(), model, controller);
            root.setSpacing(10);

            baseSideLabel = new BisqLabel();
            baseSideLabel.getStyleClass().add("titled-group-bg-label-active");

            baseSideAccountsTableView = new BisqTableView<>(model.baseSideAccountSortedList);
            int tableHeight = 210;
            baseSideAccountsTableView.setFixHeight(tableHeight);
            configAccountTableView(baseSideAccountsTableView, true);
            VBox.setMargin(baseSideAccountsTableView, new Insets(0, 0, 20, 0));
            baseSideButton = new BisqButton(Res.offerbook.get("createOffer.account.createNew"));
            VBox baseSidePlaceHolderBox = createPlaceHolderBox(baseSideButton);
            baseSideAccountsTableView.setPlaceholder(baseSidePlaceHolderBox);

            baseSideSettlementTableView = new BisqTableView<>(model.baseSideSettlementSortedList);
            baseSideSettlementTableView.setFixHeight(tableHeight);
            configSettlementTableView(baseSideSettlementTableView, true);
            VBox.setMargin(baseSideSettlementTableView, new Insets(0, 0, 20, 0));

            baseSideBox = new VBox();
            baseSideBox.setSpacing(10);
            baseSideBox.getChildren().addAll(baseSideLabel, baseSideAccountsTableView, baseSideSettlementTableView);

            quoteSideLabel = new BisqLabel();
            quoteSideLabel.getStyleClass().add("titled-group-bg-label-active");

            quoteSideAccountsTableView = new BisqTableView<>(model.quoteSideAccountSortedList);
            quoteSideAccountsTableView.setFixHeight(tableHeight);
            configAccountTableView(quoteSideAccountsTableView, false);
            VBox.setMargin(quoteSideAccountsTableView, new Insets(0, 0, 20, 0));
            quoteSideButton = new BisqButton(Res.offerbook.get("createOffer.account.createNew"));
            VBox quoteSidePlaceHolderBox = createPlaceHolderBox(quoteSideButton);
            quoteSideAccountsTableView.setPlaceholder(quoteSidePlaceHolderBox);

            quoteSideSettlementTableView = new BisqTableView<>(model.quoteSideSettlementSortedList);
            quoteSideSettlementTableView.setFixHeight(tableHeight);
            configSettlementTableView(quoteSideSettlementTableView, false);
            VBox.setMargin(quoteSideSettlementTableView, new Insets(0, 0, 20, 0));

            quoteSideBox = new VBox();
            quoteSideBox.setSpacing(10);
            quoteSideBox.getChildren().addAll(quoteSideLabel, quoteSideAccountsTableView, quoteSideSettlementTableView);

            HBox.setHgrow(baseSideBox, Priority.ALWAYS);
            HBox.setHgrow(quoteSideBox, Priority.ALWAYS);
            root.getChildren().addAll(baseSideBox, quoteSideBox);
        }

        public void onViewAttached() {
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

            baseSideSettlementTableView.visibleProperty().bind(model.baseSideSettlementVisibility);
            baseSideSettlementTableView.managedProperty().bind(model.baseSideSettlementVisibility);
            quoteSideSettlementTableView.visibleProperty().bind(model.quoteSideSettlementVisibility);
            quoteSideSettlementTableView.managedProperty().bind(model.quoteSideSettlementVisibility);
        }

        public void onViewDetached() {
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

            baseSideSettlementTableView.visibleProperty().unbind();
            baseSideSettlementTableView.managedProperty().unbind();
            quoteSideSettlementTableView.visibleProperty().unbind();
            quoteSideSettlementTableView.managedProperty().unbind();
        }

        private VBox createPlaceHolderBox(BisqButton baseSideButton) {
            BisqLabel placeholderLabel = new BisqLabel(Res.offerbook.get("createOffer.account.placeholder.noAccounts"));
            VBox vBox = new VBox();
            vBox.setSpacing(10);
            vBox.getChildren().addAll(placeholderLabel, baseSideButton);
            vBox.setAlignment(Pos.CENTER);
            return vBox;
        }

        private void configAccountTableView(BisqTableView<AccountListItem> tableView, boolean isBaseSide) {
            tableView.getColumns().add(new BisqTableColumn.Builder<AccountListItem>()
                    .title(Res.offerbook.get("createOffer.account.table.accountName"))
                    .minWidth(120)
                    .valueSupplier(AccountListItem::getAccountName)
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<AccountListItem>()
                    .title(Res.offerbook.get("createOffer.account.table.method"))
                    .minWidth(120)
                    .valueSupplier(AccountListItem::getSettlementMethodName)
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<AccountListItem>()
                    .title(Res.offerbook.get("createOffer.account.table.select"))
                    .minWidth(40)
                    .cellFactory(BisqTableColumn.CellFactory.CHECKBOX)
                    .toggleHandler((item, selected) -> controller.onAccountSelectionChanged(item, selected, isBaseSide))
                    .build());
        }

        private void configSettlementTableView(BisqTableView<SettlementListItem> tableView, boolean isBaseSide) {
            tableView.getColumns().add(new BisqTableColumn.Builder<SettlementListItem>()
                    .title(Res.offerbook.get("createOffer.account.table.method"))
                    .minWidth(150)
                    .valueSupplier(SettlementListItem::getName)
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<SettlementListItem>()
                    .title(Res.offerbook.get("createOffer.account.table.select"))
                    .minWidth(40)
                    .cellFactory(BisqTableColumn.CellFactory.CHECKBOX)
                    .toggleHandler((item, selected) -> controller.onSettlementSelectionChanged(item, selected, isBaseSide))
                    .build());
        }
    }

    @Getter
    private static class AccountListItem implements TableItem {
        private final Account<? extends SettlementMethod> account;
        private final String accountName;
        private final SettlementMethod settlementMethod;
        private final String settlementMethodName;

        public AccountListItem(Account<? extends SettlementMethod> account) {
            this.account = account;
            accountName = account.getAccountName();
            settlementMethod = account.getSettlementMethod();
            settlementMethodName = Res.offerbook.get(settlementMethod.name());
        }

        @Override
        public void activate() {
        }

        @Override
        public void deactivate() {
        }
    }

    @Getter
    private static class SettlementListItem implements TableItem {
        private final SettlementMethod settlementMethod;
        private final String name;

        public SettlementListItem(SettlementMethod settlementMethod, String currencyCode) {
            this.settlementMethod = settlementMethod;
            name = settlementMethod.getDisplayName(currencyCode);
        }

        @Override
        public void activate() {
        }

        @Override
        public void deactivate() {
        }
    }
}