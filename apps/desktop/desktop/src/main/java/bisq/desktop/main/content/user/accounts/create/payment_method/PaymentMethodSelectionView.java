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

package bisq.desktop.main.content.user.accounts.create.payment_method;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethodChargebackRisk;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.locale.Country;
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

import java.util.List;

@Slf4j
public class PaymentMethodSelectionView extends View<VBox, PaymentMethodSelectionModel, PaymentMethodSelectionController> {
    private final BisqTableView<PaymentMethodItem> tableView;
    private final SearchBox searchBox;
    private final ListChangeListener<PaymentMethodItem> listChangeListener;
    private final StackPane searchBoxPane;
    private Subscription searchTextPin, selectedItemPin;
    private Label paymentMethodHeaderLabel;

    public PaymentMethodSelectionView(PaymentMethodSelectionModel model, PaymentMethodSelectionController controller) {
        super(new VBox(20), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("payment-method-selection-view");

        Label headline = new Label(Res.get("user.paymentAccounts.createAccount.paymentMethod.headline"));
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
        VBox.setMargin(tableView, new Insets(0, 10, 20, 10));
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

        selectedItemPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(), controller::onPaymentMethodSelected);

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

        BisqTableColumn<PaymentMethodItem> methodNameColumn = new BisqTableColumn.Builder<PaymentMethodItem>()
                .title(Res.get("user.paymentAccounts.createAccount.paymentMethod.table.name"))
                .left()
                .minWidth(180)
                .setCellFactory(getNameWithIconCellFactory())
                .valueSupplier(PaymentMethodItem::getName)
                .build();
        tableView.getColumns().add(methodNameColumn);

        Node node = methodNameColumn.getGraphic();
        if (node instanceof Label label) {
            paymentMethodHeaderLabel = label;
            searchBoxPane.getChildren().add(0, label);
            StackPane.setMargin(label, new Insets(0, 0, 0, 20));
            methodNameColumn.setGraphic(searchBoxPane);
        }

        tableView.getColumns().add(new BisqTableColumn.Builder<PaymentMethodItem>()
                .title(Res.get("user.paymentAccounts.createAccount.paymentMethod.table.currencies"))
                .minWidth(120)
                .valueSupplier(PaymentMethodItem::getCurrencyCodes)
                .tooltipSupplier(PaymentMethodItem::getCurrencyCodeAndDisplayNames)
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<PaymentMethodItem>()
                .title(Res.get("user.paymentAccounts.createAccount.paymentMethod.table.countries"))
                .minWidth(130)
                .valueSupplier(PaymentMethodItem::getCountryCodes)
                .tooltipSupplier(PaymentMethodItem::getCountryNames)
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<PaymentMethodItem>()
                .title(Res.get("user.paymentAccounts.createAccount.paymentMethod.table.chargebackRisk"))
                .right()
                .minWidth(110)
                .valueSupplier(PaymentMethodItem::getChargebackRisk)
                .setCellFactory(getChargebackRiskCellFactory())
                .build());
    }

    private Callback<TableColumn<PaymentMethodItem, PaymentMethodItem>, TableCell<PaymentMethodItem, PaymentMethodItem>> getNameWithIconCellFactory() {
        return column -> new TableCell<>() {
            private final Label label = new Label();
            private final Tooltip tooltip = new BisqTooltip(BisqTooltip.Style.DARK);

            {
                label.setGraphicTextGap(8);
            }

            @Override
            protected void updateItem(PaymentMethodItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    PaymentMethod<?> paymentMethod = item.getPaymentMethod();

                    Node icon = !paymentMethod.isCustomPaymentMethod()
                            ? ImageUtil.getImageViewById(paymentMethod.getName())
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

    private Callback<TableColumn<PaymentMethodItem, PaymentMethodItem>, TableCell<PaymentMethodItem, PaymentMethodItem>> getChargebackRiskCellFactory() {
        return column -> new TableCell<>() {
            private final Label label = new Label();

            @Override
            protected void updateItem(PaymentMethodItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    label.setText(item.getChargebackRisk());

                    // TODO maybe add some risk gauge graphic?
                   /* switch (item.getChargebackRiskEnum()) {
                        case VERY_LOW -> label.setStyle("-fx-text-fill: -bisq2-green;");
                        case LOW -> label.setStyle("-fx-text-fill: -fx-light-text-color;");
                        case MODERATE -> label.setStyle("-fx-text-fill: -bisq2-yellow;");
                    }*/

                    setGraphic(label);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    @Getter
    public static class PaymentMethodItem {
        private final PaymentMethod<?> paymentMethod;
        private final String name, currencyCodes, currencyCodeAndDisplayNames, countryCodes, countryNames, chargebackRisk;

        public PaymentMethodItem(PaymentMethod<?> paymentMethod) {
            this.paymentMethod = paymentMethod;
            name = paymentMethod.getDisplayString();

            currencyCodes = paymentMethod.getSupportedCurrencyCodesAsDisplayString();
            currencyCodeAndDisplayNames = paymentMethod.getSupportedCurrencyDisplayNameAndCodeAsDisplayString();
            countryCodes = String.join(", ", getCountryCodes(paymentMethod));
            countryNames = String.join(", ", getCountryNames(paymentMethod));
            chargebackRisk = getChargebackRiskEnum(paymentMethod).getDisplayString();
        }

        private List<String> getCountryCodes(PaymentMethod<?> method) {
            if (method instanceof FiatPaymentMethod fiatMethod) {
                return fiatMethod.getPaymentRail().getSupportedCountries().stream()
                        .map(Country::getCode)
                        .sorted()
                        .toList();
            }
            return List.of();
        }

        private List<String> getCountryNames(PaymentMethod<?> method) {
            if (method instanceof FiatPaymentMethod fiatMethod) {
                return fiatMethod.getPaymentRail().getSupportedCountries().stream()
                        .map(Country::getName)
                        .sorted()
                        .toList();
            }
            return List.of();
        }

        private FiatPaymentMethodChargebackRisk getChargebackRiskEnum(PaymentMethod<?> method) {
            if (method instanceof FiatPaymentMethod fiatMethod) {
                return fiatMethod.getPaymentRail().getChargebackRisk();
            } else {
                return FiatPaymentMethodChargebackRisk.VERY_LOW;
            }
        }
    }
}