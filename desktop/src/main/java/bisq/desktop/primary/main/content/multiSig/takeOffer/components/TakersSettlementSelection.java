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

package bisq.desktop.primary.main.content.multiSig.takeOffer.components;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.protocol.SwapProtocolType;
import bisq.account.settlement.SettlementMethod;
import bisq.common.currency.TradeCurrency;
import bisq.common.monetary.Market;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.offer.Offer;
import bisq.offer.spec.Direction;
import bisq.offer.spec.SettlementSpec;
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

// Copied from SettlementSelection and left the Property data even its mostly derived from offer now 
// Should be changed if we keep it, but as very unsure if that component will survive I leave it as minimal version for 
// now.
@Slf4j
public class TakersSettlementSelection {
    private final Controller controller;

    public TakersSettlementSelection(AccountService accountService) {
        controller = new Controller(accountService);
    }

    public void setOffer(Offer offer) {
        controller.setOffer(offer);
    }

    public void setSelectedMarket(Market selectedMarket) {
        controller.setSelectedMarket(selectedMarket);
    }

    public void setDirection(Direction direction) {
        controller.setDirection(direction);
    }

    public void setSelectedProtocolType(SwapProtocolType selectedProtocolType) {
        controller.setSelectedProtocolType(selectedProtocolType);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public ReadOnlyObjectProperty<Account<? extends SettlementMethod>> getSelectedBaseSideAccount() {
        return controller.model.selectedBaseSideAccount;
    }

    public ReadOnlyObjectProperty<Account<? extends SettlementMethod>> getSelectedQuoteSideAccount() {
        return controller.model.selectedQuoteSideAccount;
    }

    public ReadOnlyObjectProperty<SettlementMethod> getSelectedBaseSideSettlementMethod() {
        return controller.model.selectedBaseSideSettlementMethod;
    }

    public ReadOnlyObjectProperty<SettlementMethod> getSelectedQuoteSideSettlementMethod() {
        return controller.model.selectedQuoteSideSettlementMethod;
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller(AccountService accountService) {
            model = new Model(accountService);
            view = new View(model, this);
        }

        private void setSelectedProtocolType(SwapProtocolType selectedProtocolType) {
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

        private void setOffer(Offer offer) {
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
            model.selectedBaseSideSettlementMethod.set(null);
            model.selectedQuoteSideSettlementMethod.set(null);

            SwapProtocolType selectedProtocolType = model.selectedProtocolType;
            if (selectedProtocolType == null) {
                model.visibility.set(false);
                return;
            }

            model.visibility.set(true);

            String baseSideCode = market.baseCurrencyCode();
            Set<SettlementMethod> baseSideSettlementMethodByName = model.offer.getBaseSideSettlementSpecs().stream()
                    .map(SettlementSpec::settlementMethodName)
                    .map(settlementMethodName -> {
                        return SettlementMethod.from(settlementMethodName, baseSideCode);
                    })
                    .collect(Collectors.toSet());
            String quoteSideCode = market.quoteCurrencyCode();
            Set<SettlementMethod> quoteSideSettlementMethodByName = model.offer.getQuoteSideSettlementSpecs().stream()
                    .map(SettlementSpec::settlementMethodName)
                    .map(settlementMethodName -> SettlementMethod.from(settlementMethodName, quoteSideCode))
                    .collect(Collectors.toSet());

            model.baseSideAccountObservableList.clear();
            model.baseSideAccountObservableList.setAll(model.accountService.getMatchingAccounts(selectedProtocolType, baseSideCode)
                    .stream()
                    .filter(account -> baseSideSettlementMethodByName.contains(account.getSettlementMethod()))
                    .map(AccountListItem::new)
                    .collect(Collectors.toList()));
            if (model.baseSideAccountObservableList.size() == 1) {
                // todo: use last selected from settings if there are multiple
                model.selectedBaseSideAccountListItem.set(model.baseSideAccountObservableList.get(0));
                model.selectedBaseSideAccount.set(model.selectedBaseSideAccountListItem.get().getAccount());
                model.selectedBaseSideSettlementMethod.set(model.selectedBaseSideAccountListItem.get().getSettlementMethod());
            }

            model.quoteSideAccountObservableList.clear();
            model.quoteSideAccountObservableList.setAll(model.accountService.getMatchingAccounts(selectedProtocolType, quoteSideCode)
                    .stream()
                    .filter(account -> quoteSideSettlementMethodByName.contains(account.getSettlementMethod()))
                    .map(AccountListItem::new)
                    .collect(Collectors.toList()));
            if (model.quoteSideAccountObservableList.size() == 1) {
                // todo: use last selected from settings if there are multiple
                model.selectedQuoteSideAccountListItem.set(model.quoteSideAccountObservableList.get(0));
                model.selectedQuoteSideAccount.set(model.selectedQuoteSideAccountListItem.get().getAccount());
                model.selectedQuoteSideSettlementMethod.set(model.selectedQuoteSideAccountListItem.get().getSettlementMethod());
            }

            model.baseSideSettlementObservableList.setAll(SettlementMethod.from(selectedProtocolType, baseSideCode)
                    .stream()
                    .filter(baseSideSettlementMethodByName::contains)
                    .map(e -> new SettlementListItem(e, baseSideCode))
                    .collect(Collectors.toList()));
            if (model.baseSideSettlementObservableList.size() == 1) {
                // todo: use last selected from settings if there are multiple
                model.selectedBaseSideSettlementListItem.set(model.baseSideSettlementObservableList.get(0));
                model.selectedBaseSideSettlementMethod.set(model.selectedBaseSideSettlementListItem.get().getSettlementMethod());
            }

            model.quoteSideSettlementObservableList.setAll(SettlementMethod.from(selectedProtocolType, quoteSideCode)
                    .stream()
                    .filter(quoteSideSettlementMethodByName::contains)
                    .map(e -> new SettlementListItem(e, quoteSideCode))
                    .collect(Collectors.toList()));
            if (model.quoteSideSettlementObservableList.size() == 1) {
                // todo: use last selected from settings if there are multiple
                model.selectedQuoteSideSettlementListItem.set(model.quoteSideSettlementObservableList.get(0));
                model.selectedQuoteSideSettlementMethod.set(model.selectedQuoteSideSettlementListItem.get().getSettlementMethod());
            }

            // For Fiat we show always accounts. If no accounts set up yet the user gets the create-account button 
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

            // If no account is visible we show the settlement
            model.baseSideSettlementVisibility.set(!model.baseSideAccountsVisibility.get());
            model.quoteSideSettlementVisibility.set(!model.quoteSideAccountsVisibility.get());

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
                model.baseSideDescription.set(Res.get("takeOffer.account.description",
                        baseSideVerb, market.baseCurrencyCode()));
            } else {
                model.baseSideDescription.set(Res.get("takeOffer.settlement.description",
                        baseSideVerb, market.baseCurrencyCode()));
            }
            if (model.quoteSideAccountsVisibility.get()) {
                model.quoteSideDescription.set(Res.get("takeOffer.account.description",
                        quoteSideVerb, market.quoteCurrencyCode()));
            } else {
                model.quoteSideDescription.set(Res.get("takeOffer.settlement.description",
                        quoteSideVerb, market.quoteCurrencyCode()));
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
            model.selectedBaseSideSettlementMethod.set(null);
            model.selectedQuoteSideSettlementMethod.set(null);
            model.selectedBaseSideAccountListItem.set(null);
            model.selectedQuoteSideAccountListItem.set(null);
            model.selectedBaseSideSettlementListItem.set(null);
            model.selectedQuoteSideSettlementListItem.set(null);
        }

        private void onAccountSelectionChanged(AccountListItem listItem, boolean isBaseSide) {
            if (listItem == null) return;

            var selectedAccount = isBaseSide ?
                    model.selectedBaseSideAccount :
                    model.selectedQuoteSideAccount;
            var selectedSettlementMethod = isBaseSide ?
                    model.selectedBaseSideSettlementMethod :
                    model.selectedQuoteSideSettlementMethod;

            selectedAccount.set(listItem.account);
            selectedSettlementMethod.set(listItem.settlementMethod);
        }

        private void onSettlementSelectionChanged(SettlementListItem listItem, boolean isBaseSide) {
            if (listItem == null) return;

            var selectedSettlementMethod = isBaseSide ?
                    model.selectedBaseSideSettlementMethod :
                    model.selectedQuoteSideSettlementMethod;
            selectedSettlementMethod.set(listItem.settlementMethod);
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Account<? extends SettlementMethod>> selectedBaseSideAccount = new SimpleObjectProperty<>();
        private final ObjectProperty<Account<? extends SettlementMethod>> selectedQuoteSideAccount = new SimpleObjectProperty<>();
        private final ObjectProperty<SettlementMethod> selectedBaseSideSettlementMethod = new SimpleObjectProperty<>();
        private final ObjectProperty<SettlementMethod> selectedQuoteSideSettlementMethod = new SimpleObjectProperty<>();


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
        private final ObjectProperty<AccountListItem> selectedBaseSideAccountListItem = new SimpleObjectProperty<>();
        private final ObjectProperty<AccountListItem> selectedQuoteSideAccountListItem = new SimpleObjectProperty<>();

        private final ObservableList<SettlementListItem> baseSideSettlementObservableList = FXCollections.observableArrayList();
        private final SortedList<SettlementListItem> baseSideSettlementSortedList = new SortedList<>(baseSideSettlementObservableList);
        private final ObservableList<SettlementListItem> quoteSideSettlementObservableList = FXCollections.observableArrayList();
        private final SortedList<SettlementListItem> quoteSideSettlementSortedList = new SortedList<>(quoteSideSettlementObservableList);
        private final ObjectProperty<SettlementListItem> selectedBaseSideSettlementListItem = new SimpleObjectProperty<>();
        private final ObjectProperty<SettlementListItem> selectedQuoteSideSettlementListItem = new SimpleObjectProperty<>();
        private Market selectedMarket;
        private Direction direction;
        private SwapProtocolType selectedProtocolType;
        private Offer offer;


        private Model(AccountService accountService) {
            this.accountService = accountService;
        }
    }

    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final Label baseSideLabel, quoteSideLabel;
        private final AutoCompleteComboBox<AccountListItem> baseSideAccountsComboBox, quoteSideAccountsComboBox;
        private final AutoCompleteComboBox<SettlementListItem> baseSideSettlementComboBox, quoteSideSettlementComboBox;
        private final VBox baseSideBox, quoteSideBox;

        private View(Model model,
                     Controller controller) {
            super(new HBox(), model, controller);
            root.setSpacing(10);

            baseSideLabel = new Label();
            baseSideLabel.getStyleClass().add("titled-group-bg-label-active");

            baseSideAccountsComboBox = new AutoCompleteComboBox<>(model.baseSideAccountSortedList);
            setupAccountStringConverter(baseSideAccountsComboBox);
            VBox.setMargin(baseSideAccountsComboBox, new Insets(0, 0, 20, 0));

            baseSideSettlementComboBox = new AutoCompleteComboBox<>(model.baseSideSettlementSortedList);
            setupSettlementStringConverter(baseSideSettlementComboBox);
            VBox.setMargin(baseSideSettlementComboBox, new Insets(0, 0, 20, 0));

            baseSideBox = new VBox();
            baseSideBox.setSpacing(10);
            baseSideBox.getChildren().addAll(baseSideLabel, baseSideAccountsComboBox, baseSideSettlementComboBox);

            quoteSideLabel = new Label();
            quoteSideLabel.getStyleClass().add("titled-group-bg-label-active");

            quoteSideAccountsComboBox = new AutoCompleteComboBox<>(model.quoteSideAccountSortedList);
            setupAccountStringConverter(quoteSideAccountsComboBox);
            VBox.setMargin(quoteSideAccountsComboBox, new Insets(0, 0, 20, 0));

            quoteSideSettlementComboBox = new AutoCompleteComboBox<>(model.quoteSideSettlementSortedList);
            setupSettlementStringConverter(quoteSideSettlementComboBox);
            VBox.setMargin(quoteSideSettlementComboBox, new Insets(0, 0, 20, 0));

            quoteSideBox = new VBox();
            quoteSideBox.setSpacing(10);
            quoteSideBox.getChildren().addAll(quoteSideLabel, quoteSideAccountsComboBox, quoteSideSettlementComboBox);

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
            baseSideSettlementComboBox.setOnAction(e -> controller.onSettlementSelectionChanged(
                    baseSideSettlementComboBox.getSelectionModel().getSelectedItem(), true));
            quoteSideSettlementComboBox.setOnAction(e -> controller.onSettlementSelectionChanged(
                    quoteSideSettlementComboBox.getSelectionModel().getSelectedItem(), false));

            UIThread.runOnNextRenderFrame(() -> {
                baseSideAccountsComboBox.getSelectionModel().select(model.selectedBaseSideAccountListItem.get());
                quoteSideAccountsComboBox.getSelectionModel().select(model.selectedQuoteSideAccountListItem.get());
                baseSideSettlementComboBox.getSelectionModel().select(model.selectedBaseSideSettlementListItem.get());
                quoteSideSettlementComboBox.getSelectionModel().select(model.selectedQuoteSideSettlementListItem.get());
            });

            baseSideLabel.textProperty().bind(model.baseSideDescription);
            quoteSideLabel.textProperty().bind(model.quoteSideDescription);

            root.visibleProperty().bind(model.visibility);
            root.managedProperty().bind(model.visibility);

            baseSideAccountsComboBox.visibleProperty().bind(model.baseSideAccountsVisibility);
            baseSideAccountsComboBox.managedProperty().bind(model.baseSideAccountsVisibility);
            quoteSideAccountsComboBox.visibleProperty().bind(model.quoteSideAccountsVisibility);
            quoteSideAccountsComboBox.managedProperty().bind(model.quoteSideAccountsVisibility);

            baseSideSettlementComboBox.visibleProperty().bind(model.baseSideSettlementVisibility);
            baseSideSettlementComboBox.managedProperty().bind(model.baseSideSettlementVisibility);
            quoteSideSettlementComboBox.visibleProperty().bind(model.quoteSideSettlementVisibility);
            quoteSideSettlementComboBox.managedProperty().bind(model.quoteSideSettlementVisibility);
        }

        @Override
        protected void onViewDetached() {
            baseSideAccountsComboBox.setOnAction(null);
            quoteSideAccountsComboBox.setOnAction(null);
            baseSideSettlementComboBox.setOnAction(null);
            quoteSideSettlementComboBox.setOnAction(null);

            baseSideAccountsComboBox.getSelectionModel().clearSelection();
            quoteSideAccountsComboBox.getSelectionModel().clearSelection();
            baseSideSettlementComboBox.getSelectionModel().clearSelection();
            quoteSideSettlementComboBox.getSelectionModel().clearSelection();

            baseSideLabel.textProperty().unbind();
            quoteSideLabel.textProperty().unbind();

            root.visibleProperty().unbind();
            root.managedProperty().unbind();

            baseSideAccountsComboBox.visibleProperty().unbind();
            baseSideAccountsComboBox.managedProperty().unbind();
            quoteSideAccountsComboBox.visibleProperty().unbind();
            quoteSideAccountsComboBox.managedProperty().unbind();

            baseSideSettlementComboBox.visibleProperty().unbind();
            baseSideSettlementComboBox.managedProperty().unbind();
            quoteSideSettlementComboBox.visibleProperty().unbind();
            quoteSideSettlementComboBox.managedProperty().unbind();
        }

        private void setupAccountStringConverter(AutoCompleteComboBox<AccountListItem> comboBox) {
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(AccountListItem item) {
                    return item != null ? item.getAccountName() + " / " + item.getSettlementMethodName() : "";
                }

                @Override
                public AccountListItem fromString(String string) {
                    return null;
                }
            });
        }

        private void setupSettlementStringConverter(AutoCompleteComboBox<SettlementListItem> comboBox) {
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(SettlementListItem item) {
                    return item != null ? item.getName() : "";
                }

                @Override
                public SettlementListItem fromString(String string) {
                    return null;
                }
            });
        }
    }

    @Getter
    private static class AccountListItem implements TableItem {
        private final Account<? extends SettlementMethod> account;
        private final String accountName;
        private final SettlementMethod settlementMethod;
        private final String settlementMethodName;

        private AccountListItem(Account<? extends SettlementMethod> account) {
            this.account = account;
            accountName = account.getAccountName();
            settlementMethod = account.getSettlementMethod();
            settlementMethodName = Res.get(settlementMethod.name());
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

        private SettlementListItem(SettlementMethod settlementMethod, String currencyCode) {
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