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
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

public class MuSigOfferbookView extends View<VBox, MuSigOfferbookModel, MuSigOfferbookController> {
    private final RichTableView<MuSigOfferListItem> richTableView;
    private final Button createOfferButton;
    private final AutoCompleteComboBox<Market> marketSelection;
    private BisqTableColumn<MuSigOfferListItem> scoreColumn, valueColumn;
    private BisqTableColumn<MuSigOfferListItem> priceColumn, quoteCurrencyAmountColumn;
    private Subscription priceTableHeaderPin, quoteCurrencyTableHeaderPin;

    public MuSigOfferbookView(MuSigOfferbookModel model,
                              MuSigOfferbookController controller) {
        super(new VBox(20), model, controller);

       /* Label headlineLabel = new Label(Res.get("muSig.offerbook.headline.buy"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");*/

        createOfferButton = new Button(Res.get("muSig.offerbook.headline.buy"));
        createOfferButton.setDefaultButton(true);

        marketSelection = new AutoCompleteComboBox<>(model.getMarkets(), Res.get("muSig.offerbook.market.select"));
        marketSelection.setPrefWidth(300);
        marketSelection.setConverter(new StringConverter<>() {
            @Override
            public String toString(Market market) {
                return market != null ? market.getQuoteCurrencyDisplayName() : "";
            }

            @Override
            public Market fromString(String string) {
                return null;
            }
        });

        HBox hBox = new HBox(marketSelection, Spacer.fillHBox(), createOfferButton);
        hBox.setAlignment(Pos.CENTER);

        richTableView = new RichTableView<>(model.getSortedList());
        configTableView();

        VBox.setVgrow(richTableView, Priority.ALWAYS);
        root.getChildren().addAll(hBox, richTableView);
        root.setPadding(new Insets(0, 40, 0, 40));
    }

    @Override
    protected void onViewAttached() {
        richTableView.initialize();
        richTableView.resetSearch();

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
        quoteCurrencyTableHeaderPin = EasyBind.subscribe(model.getQuoteCurrencyTableHeader(), title -> {
            quoteCurrencyAmountColumn.applyTitle(title);
        });


        marketSelection.setOnChangeConfirmed(null);
        marketSelection.resetValidation();
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
                .title(Res.get("muSig.offerbook.table.direction"))
                .left()
                .comparator(Comparator.comparing(MuSigOfferListItem::getDirection))
                .valueSupplier(MuSigOfferListItem::getDirection)
                .build());
        priceColumn = new BisqTableColumn.Builder<MuSigOfferListItem>()
                .comparator(Comparator.comparing(MuSigOfferListItem::getPrice))
                .valueSupplier(MuSigOfferListItem::getPrice)
                .tooltipSupplier(MuSigOfferListItem::getPriceTooltip)
                .build();
        richTableView.getColumns().add(priceColumn);
        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.btcAmount"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getBaseAmountAsString))
                .valueSupplier(MuSigOfferListItem::getBaseAmountAsString)
                .build());
        quoteCurrencyAmountColumn = new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.quoteCurrencyAmount", "EUR"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getQuoteAmountAsString))
                .valueSupplier(MuSigOfferListItem::getQuoteAmountAsString)
                .build();
        richTableView.getColumns().add(quoteCurrencyAmountColumn);
        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.paymentMethod"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getPaymentMethod))
                .valueSupplier(MuSigOfferListItem::getPaymentMethod)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.deposit"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getDeposit))
                .valueSupplier(MuSigOfferListItem::getDeposit)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title("")
                .setCellFactory(getTakeOfferButtonCellFactory())
                .minWidth(60)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.maker.seller"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getMaker))
                .valueSupplier(MuSigOfferListItem::getMaker)
                .build());
    }

    private Callback<TableColumn<MuSigOfferListItem, MuSigOfferListItem>, TableCell<MuSigOfferListItem, MuSigOfferListItem>> getTakeOfferButtonCellFactory() {
        return column -> new TableCell<>() {
            private final Button takeOfferButton = new Button(Res.get("muSig.offerbook.takeOffer.buy"));

            {
                takeOfferButton.setDefaultButton(true);
                takeOfferButton.setStyle("-fx-padding: 5 8 5 8;");
            }

            @Override
            protected void updateItem(MuSigOfferListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    setGraphic(takeOfferButton);
                    takeOfferButton.setOnAction(e -> {
                        controller.onTakeOffer(item.getOffer());
                    });
                } else {
                    takeOfferButton.setOnAction(null);
                    setGraphic(null);
                }
            }
        };
    }
}
