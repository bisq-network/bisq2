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
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.RichTableView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public final class MuSigOfferbookView extends View<VBox, MuSigOfferbookModel, MuSigOfferbookController> {
    private final RichTableView<MuSigOfferListItem> richTableView;
    private BisqTableColumn<MuSigOfferListItem> priceColumn;

    public MuSigOfferbookView(MuSigOfferbookModel model, MuSigOfferbookController controller) {
        super(new VBox(20), model, controller);

        richTableView = new RichTableView<>(model.getSortedList());
        richTableView.getFooterVBox().setVisible(false);
        richTableView.getFooterVBox().setManaged(false);

        configTableView();

        VBox.setVgrow(richTableView, Priority.ALWAYS);
        VBox contentBox = new VBox(20);
        contentBox.getChildren().addAll(richTableView);
        contentBox.getStyleClass().add("bisq-common-bg");
        root.getChildren().addAll(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
        richTableView.initialize();
        richTableView.resetSearch();
        richTableView.sort();
    }

    @Override
    protected void onViewDetached() {
        richTableView.dispose();
    }

    private void configTableView() {
//        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
//                .title(Res.get("muSig.offerbook.table.header.intent"))
//                .setCellFactory(getActionButtonCellFactory())
//                .fixWidth(130)
//                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title("Amount to send") // TODO: FIXME
                .comparator(Comparator.comparing(MuSigOfferListItem::getBaseAmountAsString))
                .valueSupplier(MuSigOfferListItem::getBaseAmountAsString)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title("Amount to receive") // TODO: FIXME
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
                .title("Peer profile") // TODO: FIXME
                .comparator(Comparator.comparing(MuSigOfferListItem::getMaker))
                .valueSupplier(MuSigOfferListItem::getMaker)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.header.deposit"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getDeposit))
                .valueSupplier(MuSigOfferListItem::getDeposit)
                .build());
    }
}
