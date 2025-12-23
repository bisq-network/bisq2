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

package bisq.desktop.main.content.wallet.dashboard;

import bisq.common.data.Triple;
import bisq.desktop.common.Layout;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.wallet.WalletTxListItem;
import bisq.i18n.Res;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletDashboardView extends View<VBox, WalletDashboardModel, WalletDashboardController> {
    private static final double LATEST_TXS_TABLE_CELL_HEIGHT = 70;
    private static final double DROPDOWN_MENU_MAX_HEIGHT = 450;

    private final Button send, receive;
    private final Label btcBalanceLabel, availableBalanceValueLabel, reservedFundsValueLabel,
            lockedFundsValueLabel, currencyConverterValueLabel, currencyConverterCodeLabel;
    private final DropdownMenu currencyConverterDropdownMenu;
    private final BisqTableView<WalletTxListItem> latestTxsTableView;
    private final ChangeListener<Number> latestTxsTableViewHeightListener;
    private final ListChangeListener<WalletTxListItem> sortedWalletTxListItemsListener;
    private final EventHandler<Event> onShowingContextMenu = this::onShowingContextMenu;

    public WalletDashboardView(WalletDashboardModel model, WalletDashboardController controller) {
        super(new VBox(20), model, controller);

        // Header
        Label headlineLabel = new Label(Res.get("wallet.dashboard.headline"));
        headlineLabel.getStyleClass().addAll("dashboard-headline", "bisq-text-green");

        Triple<HBox, Label, Label> btcBalanceTriple = createBtcBalanceHBox();
        HBox btcBalanceHBox = btcBalanceTriple.getFirst();
        btcBalanceLabel = btcBalanceTriple.getSecond();

        Triple<HBox, Label, Label> currencyConverterBalanceTriple = createCurrencyConverterBalanceHBox();
        HBox currencyConverterBalanceHBox = currencyConverterBalanceTriple.getFirst();
        currencyConverterValueLabel = currencyConverterBalanceTriple.getSecond();
        currencyConverterCodeLabel = currencyConverterBalanceTriple.getThird();

        currencyConverterDropdownMenu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        currencyConverterDropdownMenu.setContent(currencyConverterBalanceHBox);
        currencyConverterDropdownMenu.setMaxWidth(Region.USE_PREF_SIZE);
        currencyConverterDropdownMenu.getContextMenu().setMaxHeight(DROPDOWN_MENU_MAX_HEIGHT);

        VBox.setMargin(btcBalanceHBox, new Insets(25, 0, 5, 0));
        VBox balanceVBox = new VBox(headlineLabel, btcBalanceHBox, currencyConverterDropdownMenu);
        balanceVBox.setAlignment(Pos.CENTER);

        Triple<HBox, Label, Label> availableBalanceTriple = createSummaryRow(Res.get("wallet.dashboard.availableBalance"), "btcoins-grey");
        HBox availableBalanceHBox = availableBalanceTriple.getFirst();
        availableBalanceValueLabel = availableBalanceTriple.getThird();

        Triple<HBox, Label, Label> reservedFundsTriple = createSummaryRow(Res.get("wallet.dashboard.reservedFunds"), "interchangeable-grey");
        HBox reservedFundsHBox = reservedFundsTriple.getFirst();
        reservedFundsValueLabel = reservedFundsTriple.getThird();

        Triple<HBox, Label, Label> lockedFundsTriple = createSummaryRow(Res.get("wallet.dashboard.lockedFunds"), "lock-icon-grey");
        HBox lockedFundsHBox = lockedFundsTriple.getFirst();
        lockedFundsValueLabel = lockedFundsTriple.getThird();

        double buttonsWidth = 220;
        send = new Button(Res.get("wallet.dashboard.sendBtc"));
        send.setDefaultButton(true);
        send.setMinWidth(buttonsWidth);
        send.setMaxWidth(buttonsWidth);
        receive = new Button(Res.get("wallet.dashboard.receiveBtc"));
        receive.getStyleClass().add("outlined-button");
        receive.setMinWidth(buttonsWidth);
        receive.setMaxWidth(buttonsWidth);

        double summaryAndButtonsBoxWidth = 500;
        HBox buttonsHBox = new HBox(receive, Spacer.fillHBox(), send);
        buttonsHBox.setMaxWidth(summaryAndButtonsBoxWidth);
        buttonsHBox.setMinWidth(summaryAndButtonsBoxWidth);

        VBox summaryAndButtonsVBox = new VBox(10, availableBalanceHBox, reservedFundsHBox, lockedFundsHBox, buttonsHBox);
        summaryAndButtonsVBox.setMaxWidth(summaryAndButtonsBoxWidth);
        summaryAndButtonsVBox.setMinWidth(summaryAndButtonsBoxWidth);
        VBox.setMargin(buttonsHBox, new Insets(30, 0, 0, 0));

        HBox headerHBox = new HBox(balanceVBox, Spacer.fillHBox(), summaryAndButtonsVBox);
        headerHBox.setPadding(new Insets(0, 50, 0, 50));

        // Latest txs
        Label latestTxsHeadline = new Label(Res.get("wallet.dashboard.latestTxs.headline"));
        latestTxsHeadline.getStyleClass().addAll("dashboard-headline", "bisq-grey-dimmed");

        latestTxsTableView = new BisqTableView<>(model.getVisibleWalletTxListItems(), false);
        latestTxsTableView.getStyleClass().add("latest-txs-table");
        latestTxsTableView.setFixedCellSize(LATEST_TXS_TABLE_CELL_HEIGHT);
        latestTxsTableView.hideVerticalScrollbar();
        configLatestTxsTable();

        VBox latestTxsVBox = new VBox(20, latestTxsHeadline, latestTxsTableView);
        latestTxsVBox.setPadding(new Insets(0, 50, 0, 50));

        VBox contentBox = new VBox(20);
        VBox.setMargin(headerHBox, new Insets(0, 0, 15, 0));
        contentBox.getChildren().addAll(headerHBox, getHLine(), latestTxsVBox);
        contentBox.getStyleClass().add("dashboard-bg");
        contentBox.setPadding(new Insets(50, 0, 0, 0));
        root.getChildren().addAll(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
        root.getStyleClass().add("wallet-dashboard");
        root.setMinWidth(1080);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        latestTxsTableViewHeightListener = (observable, oldValue, newValue) -> updateVisibleWalletTxListItems(newValue.doubleValue());
        sortedWalletTxListItemsListener = change -> updateVisibleWalletTxListItems(latestTxsTableView.getHeight());
    }

    @Override
    protected void onViewAttached() {
        btcBalanceLabel.textProperty().bind(model.getFormattedBtcBalanceProperty());
        currencyConverterValueLabel.textProperty().bind(model.getFormattedCurrencyConverterValueProperty());
        currencyConverterCodeLabel.textProperty().bind(model.getCurrencyConverterCodeProperty());
        availableBalanceValueLabel.textProperty().bind(model.getFormattedAvailableBalanceProperty());
        reservedFundsValueLabel.textProperty().bind(model.getFormattedReservedFundsProperty());
        lockedFundsValueLabel.textProperty().bind(model.getFormattedLockedFundsProperty());

        latestTxsTableView.heightProperty().addListener(latestTxsTableViewHeightListener);
        model.getSortedWalletTxListItems().addListener(sortedWalletTxListItemsListener);
        updateVisibleWalletTxListItems(latestTxsTableView.getHeight());

        send.setOnAction(e -> controller.onSend());
        receive.setOnAction(e -> controller.onReceive());

        currencyConverterDropdownMenu.getContextMenu().addEventHandler(Menu.ON_SHOWING, onShowingContextMenu);

        addCurrencyConverterDropdownMenuItems();
    }

    @Override
    protected void onViewDetached() {
        btcBalanceLabel.textProperty().unbind();
        currencyConverterValueLabel.textProperty().unbind();
        currencyConverterCodeLabel.textProperty().unbind();
        availableBalanceValueLabel.textProperty().unbind();
        reservedFundsValueLabel.textProperty().unbind();
        lockedFundsValueLabel.textProperty().unbind();

        latestTxsTableView.heightProperty().removeListener(latestTxsTableViewHeightListener);
        model.getSortedWalletTxListItems().removeListener(sortedWalletTxListItemsListener);

        send.setOnAction(null);
        receive.setOnAction(null);

        currencyConverterDropdownMenu.getContextMenu().removeEventHandler(Menu.ON_SHOWING, onShowingContextMenu);
    }

    private Triple<HBox, Label, Label> createBtcBalanceHBox() {
        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("btc-balance-value");

        Label codeLabel = new Label("BTC");
        codeLabel.getStyleClass().addAll("bisq-text-12");

        HBox hBox = new HBox(10, valueLabel, codeLabel);
        hBox.setAlignment(Pos.BASELINE_CENTER);
        return new Triple<>(hBox, valueLabel, codeLabel);
    }

    private Triple<HBox, Label, Label> createCurrencyConverterBalanceHBox() {
        Label valueLabel = new Label("2500.00");
        valueLabel.getStyleClass().add("currency-converter-balance-value");
        valueLabel.setMinWidth(Region.USE_PREF_SIZE);

        Label codeLabel = new Label("USD");
        codeLabel.getStyleClass().addAll("currency-converter-balance-code");
        codeLabel.setMinWidth(Region.USE_PREF_SIZE);

        HBox hBox = new HBox(7, valueLabel, codeLabel);
        hBox.setAlignment(Pos.BASELINE_CENTER);
        hBox.setMaxWidth(Region.USE_PREF_SIZE);
        return new Triple<>(hBox, valueLabel, codeLabel);
    }

    private Triple<HBox, Label, Label> createSummaryRow(String title, String imageId) {
        Label titleLabel = new Label(title);
        titleLabel.setGraphic(ImageUtil.getImageViewById(imageId));
        titleLabel.getStyleClass().add("summary-title");
        titleLabel.setGraphicTextGap(15);

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("summary-value");

        Label codeLabel = new Label("BTC");
        codeLabel.getStyleClass().addAll("summary-code");

        HBox hBox = new HBox(titleLabel, Spacer.fillHBox(), valueLabel, codeLabel);
        hBox.setAlignment(Pos.BASELINE_CENTER);
        HBox.setMargin(valueLabel, new Insets(0, 7, 0, 0));
        return new Triple<>(hBox, titleLabel, valueLabel);
    }

    private Region getHLine() {
        Region line = Layout.hLine();
        line.setPrefWidth(30);
        return line;
    }

    private void configLatestTxsTable() {
        latestTxsTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.date"))
                .left()
                .minWidth(100)
                .valueSupplier(WalletTxListItem::getDateTimeString)
                .build());

        latestTxsTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.type"))
                .minWidth(70)
                .left()
                .valueSupplier(WalletTxListItem::getType)
                .build());

        latestTxsTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.address"))
                .minWidth(180)
                .left()
                .valueSupplier(WalletTxListItem::getDestinationAddress)
                .build());

        latestTxsTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.txId"))
                .minWidth(280)
                .left()
                .valueSupplier(WalletTxListItem::getTxId)
                .build());

        latestTxsTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.amount"))
                .minWidth(70)
                .valueSupplier(WalletTxListItem::getAmountAsString)
                .build());

        latestTxsTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.confirmations"))
                .minWidth(70)
                .valueSupplier(WalletTxListItem::getNumConfirmationsAsString)
                .right()
                .build());
    }

    private void updateVisibleWalletTxListItems(double tableHeight) {
        int numRows = (int) Math.floor((tableHeight - 35) / LATEST_TXS_TABLE_CELL_HEIGHT); // 35 for the header
        int maxNumRows = Math.max(0, numRows);
        int numVisibleListItems = Math.min(model.getSortedWalletTxListItems().size(), maxNumRows);
        model.getVisibleWalletTxListItems().setAll(model.getSortedWalletTxListItems().subList(0, numVisibleListItems));
    }

    private void addCurrencyConverterDropdownMenuItems() {
        model.getMarketItems().forEach(marketItem -> {
                Label value = new Label();
                value.textProperty().bind(marketItem.getFormattedValue());
                Label code = new Label(marketItem.getCode());
                // TODO: Add icon
                HBox displayBox = new HBox(5, value, code);
                CurrencyConverterMenuItem menuItem = new CurrencyConverterMenuItem(marketItem, displayBox, value.textProperty());
//                    menuItem.setOnAction();
                currencyConverterDropdownMenu.addMenuItems(menuItem);
            });
    }

    private void onShowingContextMenu(Event event) {
        // How to apply max height to ContextMenu
        // https://stackoverflow.com/questions/51272738/javafx-contextmenu-max-size-has-no-effect#51284139
        Node content = currencyConverterDropdownMenu.getContextMenu().getSkin().getNode();
        if (content instanceof Region) {
            ((Region) content).setMaxHeight(currencyConverterDropdownMenu.getContextMenu().getMaxHeight());
        }
    }

    @Getter
    private static final class CurrencyConverterMenuItem extends DropdownMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        private final MarketItem marketItem;
        private final StringProperty valueTextProperty;

        public CurrencyConverterMenuItem(MarketItem marketItem, HBox displayBox, StringProperty valueTextProperty) {
            super("check-white", "check-white", displayBox);

            this.marketItem = marketItem;
            this.valueTextProperty = valueTextProperty;
            getStyleClass().add("dropdown-menu-item");
            updateSelection(false);
        }

        public void dispose() {
            setOnAction(null);
            valueTextProperty.unbind();
        }

        void updateSelection(boolean isSelected) {
            getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
        }

        boolean isSelected() {
            return getContent().getPseudoClassStates().contains(SELECTED_PSEUDO_CLASS);
        }
    }
}
