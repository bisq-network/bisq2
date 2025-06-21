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
import bisq.common.currency.TradeCurrency;
import bisq.common.locale.Country;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.i18n.Res;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PaymentMethodSelectionView extends View<VBox, PaymentMethodSelectionModel, PaymentMethodSelectionController> {
    private static final int TOOLTIP_NAME_THRESHOLD = 20;
    @Getter
    private final BisqTableView<PaymentMethodItem> tableView;
    private final SearchBox searchBox;
    private final Map<String, Double> textWidthCache = new LinkedHashMap<>() {
        private static final int MAX_CACHE_SIZE = 500;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Double> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private BisqTableColumn<PaymentMethodItem> currenciesColumn;
    private BisqTableColumn<PaymentMethodItem> countriesColumn;

    private ChangeListener<String> searchTextListener;
    private ChangeListener<Number> currenciesColumnWidthListener;
    private ChangeListener<Number> countriesColumnWidthListener;

    public PaymentMethodSelectionView(PaymentMethodSelectionModel model, PaymentMethodSelectionController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("payment-method-selection-view");
        root.setSpacing(20);

        Label titleLabel = new Label(Res.get("user.paymentAccounts.createAccount.paymentMethod.headline"));
        titleLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("user.paymentAccounts.createAccount.paymentMethod.subtitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(600);

        tableView = new BisqTableView<>(model.getSortedPaymentMethodItems());
        tableView.getStyleClass().add("payment-method-table");
        double tableHeight = 290;
        int tableWidth = 650;
        tableView.setMinHeight(tableHeight);
        tableView.setMaxHeight(tableHeight);
        tableView.setMinWidth(tableWidth);
        tableView.setMaxWidth(tableWidth);

        searchBox = new SearchBox();
        searchBox.setPromptText(Res.get("user.paymentAccounts.createAccount.paymentMethod.table.name").toUpperCase());
        searchBox.setMinWidth(140);
        searchBox.setMaxWidth(140);
        searchBox.getStyleClass().add("payment-method-search-box");
        searchBox.getSearchField().getStyleClass().add("payment-method-search-field");

        configureTableColumns();

        StackPane.setMargin(searchBox, new Insets(1, 0, 0, 15));
        StackPane tableContainer = new StackPane(tableView, searchBox);
        tableContainer.setAlignment(Pos.TOP_LEFT);
        tableContainer.setPrefSize(tableWidth, tableHeight);
        tableContainer.setMaxWidth(tableWidth);
        tableContainer.getStyleClass().add("payment-method-table-container");

        VBox contentArea = new VBox(10);
        contentArea.setAlignment(Pos.CENTER);
        contentArea.getChildren().addAll(titleLabel, subtitleLabel, tableContainer);

        VBox.setMargin(contentArea, new Insets(15, 40, 15, 40));

        VBox topSpacer = new VBox();
        topSpacer.setPrefHeight(10);
        topSpacer.setMaxHeight(10);

        root.getChildren().addAll(topSpacer, contentArea, Spacer.fillVBox());

        VBox.setVgrow(topSpacer, Priority.NEVER);
        VBox.setVgrow(contentArea, Priority.NEVER);
    }

    @Override
    protected void onViewAttached() {
        searchTextListener = (obs, oldValue, newValue) -> controller.onSearchTextChanged(newValue);
        searchBox.textProperty().addListener(searchTextListener);

        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                PaymentMethodItem selectedItem = tableView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    controller.onSelectPaymentMethod(selectedItem.getPaymentMethod());
                }
            }
        });

        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                PaymentMethodItem selectedItem = tableView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    controller.onSelectPaymentMethod(selectedItem.getPaymentMethod());
                    event.consume();
                }
            }
        });

        currenciesColumnWidthListener =
                (obs, oldWidth, newWidth) -> tableView.refresh();
        countriesColumnWidthListener =
                (obs, oldWidth, newWidth) -> tableView.refresh();

        currenciesColumn.widthProperty().addListener(currenciesColumnWidthListener);
        countriesColumn.widthProperty().addListener(countriesColumnWidthListener);

        tableView.initialize();
    }

    @Override
    protected void onViewDetached() {
        searchBox.textProperty().removeListener(searchTextListener);

        tableView.setOnMouseClicked(null);
        tableView.setOnKeyPressed(null);

        currenciesColumn.widthProperty().removeListener(currenciesColumnWidthListener);
        countriesColumn.widthProperty().removeListener(countriesColumnWidthListener);

        textWidthCache.clear();

        tableView.dispose();
    }

    private void configureTableColumns() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        BisqTableColumn<PaymentMethodItem> nameColumn = new BisqTableColumn.Builder<PaymentMethodItem>()
                .left()
                .minWidth(180)
                .setCellFactory(getNameWithIconCellFactory())
                .valueSupplier(PaymentMethodItem::getName)
                .comparator((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .build();

        currenciesColumn = new BisqTableColumn.Builder<PaymentMethodItem>()
                .title(Res.get("user.paymentAccounts.createAccount.paymentMethod.table.currencies"))
                .minWidth(120)
                .setCellFactory(getDynamicTextCellFactory(true))
                .valueSupplier(PaymentMethodItem::getAllCurrencies)
                .tooltipSupplier(PaymentMethodItem::getAllCurrencies)
                .comparator(Comparator.comparingInt(a ->
                        a.getPaymentMethod().getTradeCurrencies().size()))
                .build();

        countriesColumn = new BisqTableColumn.Builder<PaymentMethodItem>()
                .title(Res.get("user.paymentAccounts.createAccount.paymentMethod.table.countries"))
                .minWidth(130)
                .setCellFactory(getDynamicTextCellFactory(false))
                .valueSupplier(PaymentMethodItem::getAllCountries)
                .tooltipSupplier(PaymentMethodItem::getAllCountries)
                .comparator(Comparator.comparingInt(PaymentMethodItem::getCountriesCount))
                .build();

        BisqTableColumn<PaymentMethodItem> chargebackRiskColumn = new BisqTableColumn.Builder<PaymentMethodItem>()
                .title(Res.get("user.paymentAccounts.createAccount.paymentMethod.table.chargebackRisk"))
                .minWidth(110)
                .valueSupplier(PaymentMethodItem::getChargebackRisk)
                .tooltipSupplier(item ->
                        Res.get("user.paymentAccounts.createAccount.paymentMethod.chargebackRisk.tooltip",
                                item.getChargebackRisk()))
                .setCellFactory(getChargebackRiskCellFactory())
                .comparator(Comparator.comparingInt(PaymentMethodItem::getChargebackRiskLevel))
                .build();

        tableView.getColumns().addAll(List.of(nameColumn, currenciesColumn, countriesColumn, chargebackRiskColumn));
    }

    private Callback<TableColumn<PaymentMethodItem, PaymentMethodItem>, TableCell<PaymentMethodItem, PaymentMethodItem>> getDynamicTextCellFactory(
            boolean isCurrencyColumn) {
        return column -> new TableCell<>() {
            private final Label label = new Label();
            private final BisqTooltip tooltip = new BisqTooltip();

            {
                label.setPadding(new Insets(0, 5, 0, 5));
                label.getStyleClass().add("bisq-text-3");
                label.setWrapText(false);
                label.setTextOverrun(OverrunStyle.ELLIPSIS);
            }

            @Override
            protected void updateItem(PaymentMethodItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    String fullText = isCurrencyColumn ? item.getAllCurrencies() : item.getAllCountries();

                    label.setText(fullText);

                    double availableWidth = getTableColumn().getWidth();
                    label.setMaxWidth(availableWidth);

                    Platform.runLater(() -> {
                        Double cachedWidth = textWidthCache.get(fullText);
                        if (cachedWidth == null) {
                            Text textNode = new Text(fullText);
                            textNode.setFont(label.getFont());
                            cachedWidth = textNode.getLayoutBounds().getWidth();
                            textWidthCache.put(fullText, cachedWidth);
                        }

                        if (cachedWidth > availableWidth) {
                            if (label.getTooltip() == null) {
                                tooltip.setText(fullText);
                                tooltip.setWrapText(true);
                                label.setTooltip(tooltip);
                            }
                        } else {
                            label.setTooltip(null);
                        }
                    });

                    setGraphic(label);
                } else {
                    label.setTooltip(null);
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<PaymentMethodItem, PaymentMethodItem>, TableCell<PaymentMethodItem, PaymentMethodItem>> getChargebackRiskCellFactory() {
        return column -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.getStyleClass().add("bisq-text-3");
                label.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(PaymentMethodItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    String riskText = item.getChargebackRisk();
                    label.setText(riskText);

                    FiatPaymentMethodChargebackRisk riskLevel = item.getChargebackRiskEnum();
                    switch (riskLevel) {
                        case LOW -> label.setStyle("-fx-text-fill: -bisq2-green;");
                        case MEDIUM -> label.setStyle("-fx-text-fill: -bisq2-yellow;");
                        case HIGH -> label.setStyle("-fx-text-fill: -bisq2-red-lit-10;");
                    }

                    setGraphic(label);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<PaymentMethodItem, PaymentMethodItem>, TableCell<PaymentMethodItem, PaymentMethodItem>> getNameWithIconCellFactory() {
        return column -> new TableCell<>() {
            private final Label label = new Label();
            private final Tooltip tooltip = new BisqTooltip();

            {
                label.setPadding(new Insets(0, 0, 0, 10));
                label.setGraphicTextGap(8);
                label.getStyleClass().add("bisq-text-8");
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

                    if (item.getName().length() > TOOLTIP_NAME_THRESHOLD) {
                        tooltip.setText(item.getName());
                        label.setTooltip(tooltip);
                    } else {
                        label.setTooltip(null);
                    }

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
    public static class PaymentMethodItem {
        private final PaymentMethod<?> paymentMethod;
        private final String name;
        private final String allCurrencies;
        private final String allCountries;
        private final int countriesCount;
        private final String chargebackRisk;
        private final FiatPaymentMethodChargebackRisk chargebackRiskEnum;
        private final int chargebackRiskLevel;

        public PaymentMethodItem(PaymentMethod<?> paymentMethod) {
            this.paymentMethod = paymentMethod;
            name = paymentMethod.getDisplayString();

            List<String> currencyList = paymentMethod.getTradeCurrencies().stream()
                    .map(TradeCurrency::getCode)
                    .sorted()
                    .toList();

            allCurrencies = String.join(", ", currencyList);

            List<String> countryCodeList = getCountryCodes(paymentMethod);
            countriesCount = countryCodeList.size();

            if (countryCodeList.isEmpty()) {
                allCountries = Res.get("user.paymentAccounts.createAccount.paymentMethod.allCountries");
            } else {
                allCountries = String.join(", ", countryCodeList);
            }

            chargebackRiskEnum = getChargebackRiskEnum(paymentMethod);
            chargebackRisk = formatChargebackRisk(chargebackRiskEnum);
            chargebackRiskLevel = chargebackRiskEnum.ordinal();
        }

        private List<String> getCountryCodes(PaymentMethod<?> method) {
            if (method instanceof FiatPaymentMethod fiatMethod) {
                return fiatMethod.getPaymentRail().getCountries().stream()
                        .map(Country::getCode)
                        .sorted()
                        .toList();
            }
            return List.of();
        }

        private FiatPaymentMethodChargebackRisk getChargebackRiskEnum(PaymentMethod<?> method) {
            if (method instanceof FiatPaymentMethod fiatMethod) {
                return fiatMethod.getPaymentRail().getChargebackRisk();
            }
            return FiatPaymentMethodChargebackRisk.LOW;
        }

        private String formatChargebackRisk(FiatPaymentMethodChargebackRisk risk) {
            return switch (risk) {
                case LOW -> Res.get("user.paymentAccounts.createAccount.paymentMethod.risk.low");
                case MEDIUM -> Res.get("user.paymentAccounts.createAccount.paymentMethod.risk.medium");
                case HIGH -> Res.get("user.paymentAccounts.createAccount.paymentMethod.risk.high");
            };
        }
    }
}