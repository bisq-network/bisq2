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
import bisq.account.settlement.Settlement;
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
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

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

            // model.accountService.getAccounts().stream().
        }

        private void resetAndApplyData() {
            Direction direction = model.getDirection();
            if (direction == null) return;

            Market market = model.getSelectedMarket();

            if (market == null) return;

            model.getSelectedBaseSideAccounts().clear();
            model.getSelectedQuoteSideAccounts().clear();

            SwapProtocolType selectedProtocolTyp = model.getSelectedProtocolTyp();
            if (selectedProtocolTyp == null) {
                model.visibility.set(false);
                return;
            }
            
            model.visibility.set(true);

            model.baseSideObservableList.setAll(model.accountService.getMatchingAccounts(selectedProtocolTyp, market.baseCurrencyCode())
                    .stream()
                    .map(ListItem::new)
                    .collect(Collectors.toList()));
            model.quoteSideObservableList.setAll(model.accountService.getMatchingAccounts(selectedProtocolTyp, market.quoteCurrencyCode())
                    .stream()
                    .map(ListItem::new)
                    .collect(Collectors.toList()));

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
            model.baseSideDescription.set(Res.offerbook.get("createOffer.account.description",
                    baseSideVerb, market.baseCurrencyCode()));
            model.quoteSideDescription.set(Res.offerbook.get("createOffer.account.description",
                    quoteSideVerb, market.quoteCurrencyCode()));
        }

        public void onViewAttached() {
            resetAndApplyData();
            model.selectedProtocolTypProperty().addListener(selectedProtocolListener);
            model.selectedMarketProperty().addListener(selectedMarketListener);
            model.directionProperty().addListener(directionListener);
        }

        public void onViewDetached() {
            model.selectedProtocolTypProperty().removeListener(selectedProtocolListener);
            model.selectedMarketProperty().removeListener(selectedMarketListener);
            model.directionProperty().removeListener(directionListener);
            model.getSelectedBaseSideAccounts().clear();
            model.getSelectedQuoteSideAccounts().clear();
        }

        public void onAccountSelectionChanged(ListItem listItem, boolean selected) {
            if (selected) {
                model.getSelectedQuoteSideAccounts().add(listItem.account);
            } else {
                model.getSelectedQuoteSideAccounts().remove(listItem.account);
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
        private final BooleanProperty baseSideVisibility = new SimpleBooleanProperty();
        private final BooleanProperty quoteSideVisibility = new SimpleBooleanProperty();

        private final ObservableList<ListItem> baseSideObservableList = FXCollections.observableArrayList();
        private final FilteredList<ListItem> baseSideFilteredList = new FilteredList<>(baseSideObservableList);
        private final SortedList<ListItem> baseSideSortedList = new SortedList<>(baseSideFilteredList);

        private final ObservableList<ListItem> quoteSideObservableList = FXCollections.observableArrayList();
        private final FilteredList<ListItem> quoteSideFilteredList = new FilteredList<>(quoteSideObservableList);
        private final SortedList<ListItem> quoteSideSortedList = new SortedList<>(quoteSideFilteredList);

        public AccountModel(OfferPreparationModel offerPreparationModel, AccountService accountService) {
            this.offerPreparationModel = offerPreparationModel;
            this.accountService = accountService;
        }
    }

    public static class AccountView extends View<HBox, AccountModel, AccountController> {
        private final BisqLabel baseSideLabel, quoteSideLabel;
        private final BisqTableView<ListItem> baseSideTableView, quoteSideTableView;
        private final BisqButton baseSideButton, quoteSideButton;
        private final VBox baseSideBox, quoteSideBox;

        public AccountView(AccountModel model,
                           AccountController controller) {
            super(new HBox(), model, controller);
            root.setSpacing(10);

            baseSideLabel = new BisqLabel();
            baseSideLabel.getStyleClass().add("titled-group-bg-label-active");

            baseSideTableView = new BisqTableView<>(model.baseSideSortedList);
            baseSideTableView.setFixHeight(130);
            configTableView(baseSideTableView);
            VBox.setMargin(baseSideTableView, new Insets(0, 0, 20, 0));

            baseSideButton = new BisqButton(Res.offerbook.get("createOffer.account.createNew"));
            VBox baseSidePlaceHolderBox = createPlaceHolderBox(baseSideButton);
            baseSideTableView.setPlaceholder(baseSidePlaceHolderBox);

            baseSideBox = new VBox();
            baseSideBox.setSpacing(10);
            baseSideBox.getChildren().addAll(baseSideLabel, baseSideTableView);

            quoteSideLabel = new BisqLabel();
            quoteSideLabel.getStyleClass().add("titled-group-bg-label-active");

            quoteSideTableView = new BisqTableView<>(model.quoteSideSortedList);
            quoteSideTableView.setFixHeight(130);
            configTableView(quoteSideTableView);
            VBox.setMargin(quoteSideTableView, new Insets(0, 0, 20, 0));

            quoteSideButton = new BisqButton(Res.offerbook.get("createOffer.account.createNew"));
            VBox quoteSidePlaceHolderBox = createPlaceHolderBox(quoteSideButton);
            quoteSideTableView.setPlaceholder(quoteSidePlaceHolderBox);
            
            quoteSideBox = new VBox();
            quoteSideBox.setSpacing(10);
            quoteSideBox.getChildren().addAll(quoteSideLabel, quoteSideTableView);

            HBox.setHgrow(baseSideBox, Priority.ALWAYS);
            HBox.setHgrow(quoteSideBox, Priority.ALWAYS);
            root.getChildren().addAll(baseSideBox, quoteSideBox);
        }

        private VBox createPlaceHolderBox(BisqButton baseSideButton) {
            BisqLabel placeholderLabel = new BisqLabel(Res.offerbook.get("createOffer.account.placeholder.noAccounts"));
            VBox vBox = new VBox();
            vBox.setSpacing(10);
            vBox.getChildren().addAll(placeholderLabel, baseSideButton);
            vBox.setAlignment(Pos.CENTER);
            return vBox;
        }

        private void configTableView(BisqTableView<ListItem> tableView) {
            tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .title(Res.offerbook.get("createOffer.account.table.accountName"))
                    .minWidth(120)
                    .valueSupplier(ListItem::getAccountName)
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .title(Res.offerbook.get("createOffer.account.table.method"))
                    .minWidth(120)
                    .valueSupplier(ListItem::getSettlementMethodName)
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .title(Res.offerbook.get("createOffer.account.table.select"))
                    .minWidth(120)
                    .cellFactory(BisqTableColumn.CellFactory.CHECKBOX)
                    .toggleHandler(controller::onAccountSelectionChanged)
                    .build());
        }

        public void onViewAttached() {
            baseSideButton.setOnAction(e -> controller.onCreateBaseSideAccount());
            quoteSideButton.setOnAction(e -> controller.onCreateQuoteSideAccount());

            baseSideLabel.textProperty().bind(model.baseSideDescription);
            quoteSideLabel.textProperty().bind(model.quoteSideDescription);

            root.visibleProperty().bind(model.visibility);
            root.managedProperty().bind(model.visibility);
        }

        public void onViewDetached() {
            baseSideButton.setOnAction(null);
            quoteSideButton.setOnAction(null);

            baseSideLabel.textProperty().unbind();
            quoteSideLabel.textProperty().unbind();
            
            root.visibleProperty().unbind();
            root.managedProperty().unbind();
        }
    }

    @Getter
    private static class ListItem implements TableItem {
        private final Account<? extends Settlement.Method> account;
        private final String accountName;
        private final Settlement.Method settlementMethod;
        private final String settlementMethodName;

        public ListItem(Account<? extends Settlement.Method> account) {
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
}