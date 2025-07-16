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

package bisq.desktop.main.content.user.crypto_accounts.create.currency;

import bisq.account.payment_method.CryptoPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.i18n.Res;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class CryptoCurrencySelectionView extends View<VBox, CryptoCurrencySelectionModel, CryptoCurrencySelectionController> {
    private final BisqTableView<CryptoCurrencyItem> tableView;
    private final SearchBox searchBox;
    private final ListChangeListener<CryptoCurrencyItem> listChangeListener;
    private final StackPane searchBoxPane;
    private Subscription searchTextPin, selectedItemPin;
    private Label paymentMethodHeaderLabel;

    public CryptoCurrencySelectionView(CryptoCurrencySelectionModel model, CryptoCurrencySelectionController controller) {
        super(new VBox(20), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("payment-method-selection-view");

        Label headline = new Label(Res.get("paymentAccounts.crypto.paymentMethod.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        searchBox = new SearchBox();
        searchBox.setPromptText("");
        searchBox.getStyleClass().add("payment-method-search-box");
        searchBox.getSearchField().getStyleClass().add("payment-method-header");

        StackPane.setMargin(searchBox, new Insets(-1, 0, 0, 0));
        searchBoxPane = new StackPane(searchBox);
        searchBoxPane.setAlignment(Pos.CENTER_LEFT);

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.getStyleClass().add("payment-method-table");

        configureTableColumns();

        VBox.setMargin(headline, new Insets(30, 0, 0, 0));
        VBox.setMargin(tableView, new Insets(0, 120, 20, 120));
        root.getChildren().addAll(headline, tableView);

        listChangeListener = c -> {
            c.next();
            if (c.wasAdded() && c.getAddedSubList().contains(model.getSelectedItem().get())) {
                UIThread.runOnNextRenderFrame(() -> {
                    tableView.getSelectionModel().select(model.getSelectedItem().get());
                });
            }
        };
    }

    @Override
    protected void onViewAttached() {
        searchTextPin = EasyBind.subscribe(searchBox.textProperty(), searchText -> {
            boolean isEmpty = StringUtils.isEmpty(searchText);
            paymentMethodHeaderLabel.setVisible(isEmpty);
            paymentMethodHeaderLabel.setManaged(isEmpty);
            controller.onSearchTextChanged(searchText);
        });

        selectedItemPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(), controller::onItemSelected);

        model.getFilteredList().addListener(listChangeListener);

        tableView.initialize();
    }

    @Override
    protected void onViewDetached() {
        searchTextPin.unsubscribe();
        selectedItemPin.unsubscribe();
        model.getFilteredList().removeListener(listChangeListener);

        tableView.dispose();
    }

    private void configureTableColumns() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        BisqTableColumn<CryptoCurrencyItem> column = new BisqTableColumn.Builder<CryptoCurrencyItem>()
                .title(Res.get("paymentAccounts.createAccount.paymentMethod.table.currencies"))
                .minWidth(120)
                .left()
                .valueSupplier(CryptoCurrencyItem::getCurrencyCodeAndDisplayNames)
                .tooltipSupplier(CryptoCurrencyItem::getCurrencyCodeAndDisplayNames)
                .build();
        tableView.getColumns().add(column);

        Node node = column.getGraphic();
        if (node instanceof Label label) {
            paymentMethodHeaderLabel = label;
            searchBoxPane.getChildren().addFirst(label);
            StackPane.setMargin(label, new Insets(0, 0, 0, 20));
            column.setGraphic(searchBoxPane);
        }
    }

    private Callback<TableColumn<CryptoCurrencyItem, CryptoCurrencyItem>, TableCell<CryptoCurrencyItem, CryptoCurrencyItem>> getNameWithIconCellFactory() {
        return column -> new TableCell<>() {
            private final Label label = new Label();
            private final Tooltip tooltip = new BisqTooltip(BisqTooltip.Style.DARK);

            {
                label.setGraphicTextGap(8);
            }

            @Override
            protected void updateItem(CryptoCurrencyItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    PaymentMethod<?> paymentMethod = item.getPaymentMethod();

                    Node icon = !paymentMethod.isCustomPaymentMethod()
                            ? ImageUtil.getImageViewById(paymentMethod.getPaymentRailName())
                            : BisqEasyViewUtils.getCustomPaymentMethodIcon(paymentMethod.getDisplayString());

                    label.setGraphic(icon);
                    label.setText(item.getName());

                    tooltip.setText(item.getName());
                    label.setTooltip(tooltip);

                    setGraphic(label);
                } else {
                    label.setTooltip(null);
                    label.setGraphic(null);
                    setGraphic(null);
                }
            }
        };
    }

    @Getter
    public static class CryptoCurrencyItem {
        private final CryptoPaymentMethod paymentMethod;
        private final String name, currencyCode, currencyCodeAndDisplayNames;

        public CryptoCurrencyItem(CryptoPaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            name = paymentMethod.getDisplayString();

            currencyCode = paymentMethod.getCurrencyCode();
            currencyCodeAndDisplayNames = paymentMethod.getSupportedCurrencyDisplayNameAndCodeAsDisplayString();
        }
    }
}