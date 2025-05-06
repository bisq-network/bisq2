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

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.RichTableView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.Comparator;

public class MuSigOfferbookView extends View<VBox, MuSigOfferbookModel, MuSigOfferbookController> {
    private final RichTableView<MuSigOfferListItem> richTableView;
    private final Button createOfferButton;
    private BisqTableColumn<MuSigOfferListItem> scoreColumn, valueColumn;

    public MuSigOfferbookView(MuSigOfferbookModel model,
                              MuSigOfferbookController controller) {
        super(new VBox(), model, controller);

        Label headlineLabel = new Label(Res.get("muSig.offerbook.headline.buy"));
        headlineLabel.getStyleClass().add("bisq-text-3");

        createOfferButton = new Button(Res.get("muSig.offerbook.headline.buy"));
        createOfferButton.setDefaultButton(true);

        HBox hBox = new HBox(headlineLabel, Spacer.fillHBox(), createOfferButton);
        hBox.setAlignment(Pos.CENTER);

        richTableView = new RichTableView<>(model.getSortedList());
        configTableView();

        VBox contentBox = new VBox(10);
        contentBox.getStyleClass().add("bisq-common-bg");
        VBox.setVgrow(richTableView, Priority.ALWAYS);
        contentBox.getChildren().addAll(hBox, richTableView);

        VBox.setVgrow(contentBox, Priority.ALWAYS);
        root.getChildren().addAll(contentBox);
        root.setPadding(new Insets(-20, 20, 0, 20));
    }

    @Override
    protected void onViewAttached() {
        richTableView.initialize();
        richTableView.resetSearch();

        createOfferButton.setOnAction(e -> controller.onCreateOffer());
    }

    @Override
    protected void onViewDetached() {
        richTableView.dispose();
    }

    private void configTableView() {
        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.price", "EUR"))
                .left()
                .comparator(Comparator.comparing(MuSigOfferListItem::getPriceSpecAsString))
                .valueSupplier(MuSigOfferListItem::getPriceSpecAsString)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.btcAmount"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getBaseAmountAsString))
                .valueSupplier(MuSigOfferListItem::getBaseAmountAsString)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.otherAmount", "EUR"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getQuoteAmountAsString))
                .valueSupplier(MuSigOfferListItem::getQuoteAmountAsString)
                .build());
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
