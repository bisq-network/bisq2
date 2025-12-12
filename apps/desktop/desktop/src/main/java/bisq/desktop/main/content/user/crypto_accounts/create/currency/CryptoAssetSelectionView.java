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

import bisq.account.payment_method.DigitalAssetPaymentMethod;
import bisq.account.payment_method.cbdc.CbdcPaymentMethod;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethod;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.locale.CountryRepository;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.RichTableView;
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
    private Subscription  selectedItemPin, selectedTypePin;
    private BisqTableColumn<CryptoAssetItem> tokenStandardColumn, networkColumn, pegCurrencyColumn, countryColumn;

    public CryptoAssetSelectionView(CryptoAssetSelectionModel model, CryptoAssetSelectionController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("payment-method-selection-view");

        Label headline = new Label(Res.get("paymentAccounts.crypto.paymentMethod.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        richTableView = new RichTableView<>(model.getSortedList(),
                model.getFilterMenuItems(),
                model.getFilterMenuItemToggleGroup(),
                controller::onSearchTextChanged,
                Res.get("paymentAccounts.crypto.paymentMethod.entriesUnit"));
        richTableView.setPrefHeight(360);

        richTableView.getNumEntriesLabel().setVisible(false);
        richTableView.getNumEntriesLabel().setManaged(false);
        richTableView.getExportButton().setVisible(false);
        richTableView.getExportButton().setManaged(false);

        configureTableColumns();

        VBox.setMargin(headline, new Insets(30, 0, 0, 0));
        VBox.setMargin(richTableView, new Insets(0, 0, 20, 0));
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
                        countryColumn.setVisible(false);
                    }
                    case STABLE_COIN -> {
                        tokenStandardColumn.setVisible(true);
                        networkColumn.setVisible(true);
                        pegCurrencyColumn.setVisible(true);
                        countryColumn.setVisible(false);
                    }
                    case CBDC -> {
                        tokenStandardColumn.setVisible(false);
                        networkColumn.setVisible(false);
                        pegCurrencyColumn.setVisible(true);
                        countryColumn.setVisible(true);
                    }
                }
            }
        });

        model.getFilteredList().addListener(listChangeListener);
    }

    @Override
    protected void onViewDetached() {
        richTableView.dispose();
        selectedItemPin.unsubscribe();
        selectedTypePin.unsubscribe();
        model.getFilteredList().removeListener(listChangeListener);
    }

    private void configureTableColumns() {
        richTableView.getColumns().add(richTableView.getSelectionMarkerColumn());

        richTableView.getColumns().add(new BisqTableColumn.Builder<CryptoAssetItem>()
                .title(Res.get("paymentAccounts.crypto.createAccount.paymentMethod.table.ticker"))
                .minWidth(70)
                .left()
                .setCellFactory(getTickerWithIconCellFactory())
                .tooltipSupplier(CryptoAssetItem::getTicker)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<CryptoAssetItem>()
                .title(Res.get("paymentAccounts.crypto.createAccount.paymentMethod.table.name"))
                .minWidth(140)
                .valueSupplier(CryptoAssetItem::getName)
                .tooltipSupplier(CryptoAssetItem::getName)
                .build());

        tokenStandardColumn = new BisqTableColumn.Builder<CryptoAssetItem>()
                .title(Res.get("paymentAccounts.crypto.createAccount.paymentMethod.table.tokenStandard"))
                .minWidth(100)
                .valueSupplier(CryptoAssetItem::getTokenStandard)
                .tooltipSupplier(CryptoAssetItem::getTokenStandard)
                .build();
        richTableView.getColumns().add(tokenStandardColumn);

        networkColumn = new BisqTableColumn.Builder<CryptoAssetItem>()
                .title(Res.get("paymentAccounts.crypto.createAccount.paymentMethod.table.network"))
                .minWidth(100)
                .valueSupplier(CryptoAssetItem::getNetwork)
                .tooltipSupplier(CryptoAssetItem::getNetwork)
                .build();
        richTableView.getColumns().add(networkColumn);

        pegCurrencyColumn = new BisqTableColumn.Builder<CryptoAssetItem>()
                .title(Res.get("paymentAccounts.crypto.createAccount.paymentMethod.table.pegCurrency"))
                .minWidth(160)
                .valueSupplier(CryptoAssetItem::getPegCurrency)
                .tooltipSupplier(CryptoAssetItem::getPegCurrency)
                .build();
        richTableView.getColumns().add(pegCurrencyColumn);

        countryColumn = new BisqTableColumn.Builder<CryptoAssetItem>()
                .title(Res.get("paymentAccounts.crypto.createAccount.paymentMethod.table.country"))
                .minWidth(140)
                .right()
                .valueSupplier(CryptoAssetItem::getCountry)
                .tooltipSupplier(CryptoAssetItem::getCountry)
                .build();
        richTableView.getColumns().add(countryColumn);
    }

    private Callback<TableColumn<CryptoAssetItem, CryptoAssetItem>, TableCell<CryptoAssetItem, CryptoAssetItem>> getTickerWithIconCellFactory() {
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
                    DigitalAssetPaymentMethod paymentMethod = item.getPaymentMethod();

                    Node icon = ImageUtil.getImageViewById(item.getTicker());

                    label.setGraphic(icon);
                    label.setText(item.getTicker());

                    tooltip.setText(item.getTicker());
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

        private final DigitalAssetPaymentMethod paymentMethod;
        private final String ticker, name, tokenStandard, network, pegCurrency, country;
        private final Type type;

        public CryptoAssetItem(DigitalAssetPaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;

            String notAvailable = "-";
            if (paymentMethod instanceof CryptoPaymentMethod cryptoPaymentMethod) {
                ticker = cryptoPaymentMethod.getCode();
                name = cryptoPaymentMethod.getName();
                tokenStandard = notAvailable;
                network = notAvailable;
                pegCurrency = notAvailable;
                country = notAvailable;
                type = Type.CRYPTO_CURRENCY;
            } else if (paymentMethod instanceof StableCoinPaymentMethod stablecoinPaymentMethod) {
                ticker = stablecoinPaymentMethod.getCode();
                name = stablecoinPaymentMethod.getName();
                tokenStandard = stablecoinPaymentMethod.getPaymentRail().getStableCoin().getTokenStandard().getDisplayName();
                network = stablecoinPaymentMethod.getPaymentRail().getStableCoin().getNetwork().getDisplayName();
                pegCurrency = stablecoinPaymentMethod.getPaymentRail().getStableCoin().getPegCurrencyCode();
                country = notAvailable;
                type = Type.STABLE_COIN;
            } else if (paymentMethod instanceof CbdcPaymentMethod cbdcPaymentMethod) {
                ticker = cbdcPaymentMethod.getCode();
                name = cbdcPaymentMethod.getName();
                tokenStandard = notAvailable;
                network = notAvailable;
                pegCurrency = FiatCurrencyRepository.getCodeAndDisplayName(cbdcPaymentMethod.getPaymentRail().getCbdc().getPegCurrencyCode());
                country = CountryRepository.getNameByCode(cbdcPaymentMethod.getPaymentRail().getCbdc().getCountryCode());
                type = Type.CBDC;
            } else {
                throw new UnsupportedOperationException("paymentMethod not supported " + paymentMethod.getClass().getSimpleName());
            }
        }

        public String relevantStrings() {
            return ticker + ", " +
                    name + ", " +
                    tokenStandard + ", " +
                    network + ", " +
                    pegCurrency + ", " +
                    country;
        }
    }
}