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
import bisq.account.payment_method.StableCoinPaymentMethod;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.RichTableView;
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
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class CryptoAssetSelectionView extends View<VBox, CryptoAssetSelectionModel, CryptoAssetSelectionController> {
    private final RichTableView<CryptoAssetItem> richTableView;
    private final ListChangeListener<CryptoAssetItem> listChangeListener;
    private Subscription searchTextPin, selectedItemPin, selectedTypePin;
    private Label paymentMethodHeaderLabel;
    private BisqTableColumn<CryptoAssetItem> tokenStandardColumn, networkColumn, pegCurrencyColumn;

    public CryptoAssetSelectionView(CryptoAssetSelectionModel model, CryptoAssetSelectionController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("payment-method-selection-view");

        Label headline = new Label(Res.get("paymentAccounts.crypto.paymentMethod.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        richTableView = new RichTableView<>(model.getSortedList(),
                "",
                model.getFilterMenuItems(),
                model.getFilterMenuItemToggleGroup(),
                controller::onSearchTextChanged);
        richTableView.setPrefHeight(360);

        richTableView.getNumEntriesLabel().setVisible(false);
        richTableView.getNumEntriesLabel().setManaged(false);
        richTableView.getExportHyperlink().setVisible(false);
        richTableView.getExportHyperlink().setManaged(false);

        configureTableColumns();

        VBox.setMargin(headline, new Insets(10, 0, -20, 0));
        root.getChildren().addAll(headline, richTableView);

        listChangeListener = c -> {
            c.next();
            if (c.wasAdded() && c.getAddedSubList().contains(model.getSelectedItem().get())) {
                UIThread.runOnNextRenderFrame(() -> {
                    richTableView.getSelectionModel().select(model.getSelectedItem().get());
                });
            }
        };
    }

    @Override
    protected void onViewAttached() {
        richTableView.initialize();
        richTableView.resetSearch();

        selectedItemPin = EasyBind.subscribe(richTableView.getSelectionModel().selectedItemProperty(), controller::onItemSelected);
        selectedTypePin = EasyBind.subscribe(model.getSelectedType(), type -> {
            if (type == null) {
                tokenStandardColumn.setVisible(true);
                networkColumn.setVisible(true);
                pegCurrencyColumn.setVisible(true);
            } else {
                switch (type) {
                    case CRYPTO_CURRENCY -> {
                        tokenStandardColumn.setVisible(false);
                        networkColumn.setVisible(false);
                        pegCurrencyColumn.setVisible(false);
                    }
                    case STABLE_COIN -> {
                        tokenStandardColumn.setVisible(true);
                        networkColumn.setVisible(true);
                        pegCurrencyColumn.setVisible(true);
                    }
                    case CBDC -> {
                        tokenStandardColumn.setVisible(false);
                        networkColumn.setVisible(false);
                        pegCurrencyColumn.setVisible(true);
                    }
                }
            }
        });

        model.getFilteredList().addListener(listChangeListener);

        richTableView.initialize();
    }

    @Override
    protected void onViewDetached() {
        richTableView.dispose();
        searchTextPin.unsubscribe();
        selectedItemPin.unsubscribe();
        model.getFilteredList().removeListener(listChangeListener);

        richTableView.dispose();
    }

    private void configureTableColumns() {
        richTableView.getColumns().add(richTableView.getSelectionMarkerColumn());

        richTableView.getColumns().add(new BisqTableColumn.Builder<CryptoAssetItem>()
                .title(Res.get("paymentAccounts.crypto.createAccount.paymentMethod.table.ticker"))
                .minWidth(100)
                .left()
                .valueSupplier(CryptoAssetItem::getTicker)
                .tooltipSupplier(CryptoAssetItem::getTicker)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<CryptoAssetItem>()
                .title(Res.get("paymentAccounts.crypto.createAccount.paymentMethod.table.name"))
                .minWidth(140)
                .left()
                .valueSupplier(CryptoAssetItem::getName)
                .tooltipSupplier(CryptoAssetItem::getName)
                .build());

        tokenStandardColumn = new BisqTableColumn.Builder<CryptoAssetItem>()
                .title(Res.get("paymentAccounts.crypto.createAccount.paymentMethod.table.tokenStandard"))
                .minWidth(140)
                .left()
                .valueSupplier(CryptoAssetItem::getTokenStandard)
                .tooltipSupplier(CryptoAssetItem::getTokenStandard)
                .build();
        richTableView.getColumns().add(tokenStandardColumn);

        networkColumn = new BisqTableColumn.Builder<CryptoAssetItem>()
                .title(Res.get("paymentAccounts.crypto.createAccount.paymentMethod.table.network"))
                .minWidth(140)
                .left()
                .valueSupplier(CryptoAssetItem::getNetwork)
                .tooltipSupplier(CryptoAssetItem::getNetwork)
                .build();
        richTableView.getColumns().add(networkColumn);

        pegCurrencyColumn = new BisqTableColumn.Builder<CryptoAssetItem>()
                .title(Res.get("paymentAccounts.crypto.createAccount.paymentMethod.table.pegCurrency"))
                .minWidth(100)
                .left()
                .valueSupplier(CryptoAssetItem::getPegCurrency)
                .tooltipSupplier(CryptoAssetItem::getPegCurrency)
                .build();
        richTableView.getColumns().add(pegCurrencyColumn);

       /* Node node = column.getGraphic();
        if (node instanceof Label label) {
            paymentMethodHeaderLabel = label;
            searchBoxPane.getChildren().addFirst(label);
            StackPane.setMargin(label, new Insets(0, 0, 0, 20));
            column.setGraphic(searchBoxPane);
        }*/
    }

    private Callback<TableColumn<CryptoAssetItem, CryptoAssetItem>, TableCell<CryptoAssetItem, CryptoAssetItem>> getNameWithIconCellFactory() {
        return column -> new TableCell<>() {
            private final Label label = new Label();
            private final Tooltip tooltip = new BisqTooltip(BisqTooltip.Style.DARK);

            {
                label.setGraphicTextGap(8);
            }

            @Override
            protected void updateItem(CryptoAssetItem item, boolean empty) {
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
    public static class CryptoAssetItem {
        enum Type {
            CRYPTO_CURRENCY(Res.get("paymentAccounts.crypto.type.cryptoCurrency")),
            STABLE_COIN(Res.get("paymentAccounts.crypto.type.stableCoin")),
            CBDC(Res.get("paymentAccounts.crypto.type.cbdc"));

            @Getter
            private final String displayString;

            Type(String displayString) {
                this.displayString = displayString;
            }
        }

        private final PaymentMethod<?> paymentMethod;
        private final String ticker, name, tokenStandard, network, pegCurrency;
        private final Type type;

        public CryptoAssetItem(PaymentMethod<?> paymentMethod) {
            this.paymentMethod = paymentMethod;

            if (paymentMethod instanceof CryptoPaymentMethod cryptoPaymentMethod) {
                ticker = cryptoPaymentMethod.getCode();
                name = cryptoPaymentMethod.getName();
                tokenStandard = "-";
                network = "-";
                pegCurrency = "-";
                type = Type.CRYPTO_CURRENCY;
            } else if (paymentMethod instanceof StableCoinPaymentMethod stablecoinPaymentMethod) {
                ticker = stablecoinPaymentMethod.getCode();
                name = stablecoinPaymentMethod.getName();
                tokenStandard = stablecoinPaymentMethod.getPaymentRail().getStableCoin().getTokenStandard().getDisplayName();
                network = stablecoinPaymentMethod.getPaymentRail().getStableCoin().getNetwork().getDisplayName();
                pegCurrency = stablecoinPaymentMethod.getPaymentRail().getStableCoin().getPegCurrencyCode();
                type = Type.STABLE_COIN;
            } else {
                throw new UnsupportedOperationException("paymentMethod not supported " + paymentMethod.getClass().getSimpleName());
            }
        }

        public String relevantStrings() {
            return ticker + ", " +
                    name + ", " +
                    tokenStandard + ", " +
                    network + ", " +
                    pegCurrency;
        }
    }
}