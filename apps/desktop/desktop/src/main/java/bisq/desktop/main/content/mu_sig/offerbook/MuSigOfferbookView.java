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

package bisq.desktop.main.content.mu_sig.offerbook;

import bisq.common.currency.Market;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.RichTableView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.Locale;

@Slf4j
public abstract class MuSigOfferbookView<M extends MuSigOfferbookModel, C extends MuSigOfferbookController<?, ?>> extends View<VBox, M, C> {
    private final RichTableView<MuSigOfferListItem> richTableView;
    private BisqTableColumn<MuSigOfferListItem> priceColumn;
    private final Button createOfferButton;
    protected final AutoCompleteComboBox<Market> marketSelection;
    private Subscription priceTableHeaderPin, quoteCurrencyTableHeaderPin;

    public MuSigOfferbookView(M model, C controller) {
        super(new VBox(20), model, controller);

        createOfferButton = new Button(model.getCreateOfferButtonText());

        marketSelection = new AutoCompleteComboBox<>(model.getMarkets(), Res.get("muSig.offerbook.market.select"));
        marketSelection.setPrefWidth(300);
        marketSelection.setConverter(getConverter());

        HBox hBox = new HBox(marketSelection, Spacer.fillHBox(), createOfferButton);
        hBox.setAlignment(Pos.CENTER);

        richTableView = new RichTableView<>(model.getSortedList(), controller::onSearchInput);
        richTableView.getFooterVBox().setVisible(false);
        richTableView.getFooterVBox().setManaged(false);

        configTableView();

        VBox.setVgrow(richTableView, Priority.ALWAYS);
        root.getStyleClass().add("offerbook-container");
        root.setPadding(new Insets(20, 20, 500, 20));
        root.getChildren().addAll(hBox, richTableView);
    }
    protected abstract StringConverter<Market> getConverter();

    @Override
    protected void onViewAttached() {
        priceColumn.setComparator(model.getPriceComparator());

        richTableView.initialize();
        richTableView.resetSearch();
        richTableView.sort();

        createOfferButton.getStyleClass().add(model.getDirection().isBuy() ? "buy-button" : "sell-button");

        marketSelection.getSelectionModel().select(model.getSelectedMarket().get());
        marketSelection.setOnChangeConfirmed(e -> {
            if (marketSelection.getSelectionModel().getSelectedItem() == null) {
                marketSelection.getSelectionModel().select(model.getSelectedMarket().get());
                return;
            }
            controller.onSelectMarket(marketSelection.getSelectionModel().getSelectedItem());
        });


        createOfferButton.setOnAction(e -> controller.onCreateOffer());
        priceTableHeaderPin = EasyBind.subscribe(model.getPriceTableHeader(), title -> {
            priceColumn.applyTitle(title);
        });
        quoteCurrencyTableHeaderPin = EasyBind.subscribe(model.getAmountToReceive(), title -> {
            // quoteCurrencyAmountColumn.applyTitle(title);
        });
    }

    @Override
    protected void onViewDetached() {
        richTableView.dispose();
        priceTableHeaderPin.unsubscribe();
        quoteCurrencyTableHeaderPin.unsubscribe();

        marketSelection.setOnChangeConfirmed(null);
        marketSelection.resetValidation();

        createOfferButton.setOnAction(null);
    }

    private void configTableView() {
        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.header.intent"))
                .setCellFactory(getActionButtonCellFactory())
                .fixWidth(130)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .titleProperty(model.getAmountToSend())
                .comparator(Comparator.comparing(MuSigOfferListItem::getBaseAmountAsString))
                .valueSupplier(MuSigOfferListItem::getBaseAmountAsString)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .titleProperty(model.getAmountToReceive())
                .comparator(Comparator.comparing(MuSigOfferListItem::getQuoteAmountAsString))
                .valueSupplier(MuSigOfferListItem::getQuoteAmountAsString)
                .build());

        priceColumn = new BisqTableColumn.Builder<MuSigOfferListItem>()
                .left()
                .comparator(Comparator.comparing(MuSigOfferListItem::getPrice))
                .valueSupplier(MuSigOfferListItem::getPrice)
                .tooltipSupplier(MuSigOfferListItem::getPriceTooltip)
                .build();
        richTableView.getColumns().add(priceColumn);
        richTableView.getSortOrder().add(priceColumn);

        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.header.paymentMethod"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getPaymentMethod))
                .valueSupplier(MuSigOfferListItem::getPaymentMethod)
                .tooltipSupplier(MuSigOfferListItem::getPaymentMethodTooltip)
                .build());


        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(model.getMaker())
                .comparator(Comparator.comparing(MuSigOfferListItem::getMaker))
                .valueSupplier(MuSigOfferListItem::getMaker)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.header.deposit"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getDeposit))
                .valueSupplier(MuSigOfferListItem::getDeposit)
                .build());
    }

    private Callback<TableColumn<MuSigOfferListItem, MuSigOfferListItem>, TableCell<MuSigOfferListItem, MuSigOfferListItem>> getActionButtonCellFactory() {
        return column -> new TableCell<>() {
            private final Button takeOfferButton = new Button();

            {
                takeOfferButton.setMinWidth(110);
                takeOfferButton.setMaxWidth(takeOfferButton.getMinWidth());
                takeOfferButton.getStyleClass().add("button-min-horizontal-padding");
            }

            @Override
            protected void updateItem(MuSigOfferListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    if (item.isMyOffer()) {
                        takeOfferButton.setText(Res.get("muSig.offerbook.table.cell.intent.remove").toUpperCase(Locale.ROOT));
                        resetStyles();
                        // FIXME Label text always stays white independent of style class or even if setting style here directly.
                        //  If using grey-transparent-outlined-button we have a white label. Quick fix is to use opacity with a while style...
                        takeOfferButton.getStyleClass().add("white-transparent-outlined-button");
                        takeOfferButton.setOpacity(0.5);
                        takeOfferButton.setOnAction(e -> controller.onRemoveOffer(item.getOffer()));
                    } else {
                        takeOfferButton.setText(model.getTakeOfferButtonText());
                        takeOfferButton.setOpacity(1);
                        resetStyles();
                        if (item.getOffer().getDirection().mirror().isBuy()) {
                            takeOfferButton.getStyleClass().add("buy-button");
                        } else {
                            takeOfferButton.getStyleClass().add("sell-button");
                        }
                        takeOfferButton.setOnAction(e -> controller.onTakeOffer(item.getOffer()));
                    }
                    setGraphic(takeOfferButton);
                } else {
                    resetStyles();
                    takeOfferButton.setOnAction(null);
                    setGraphic(null);
                }
            }

            private void resetStyles() {
                takeOfferButton.getStyleClass().remove("buy-button");
                takeOfferButton.getStyleClass().remove("sell-button");
                takeOfferButton.getStyleClass().remove("white-transparent-outlined-button");
            }
        };
    }
}
