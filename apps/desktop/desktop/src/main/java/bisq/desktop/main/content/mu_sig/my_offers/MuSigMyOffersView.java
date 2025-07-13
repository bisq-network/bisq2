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

package bisq.desktop.main.content.mu_sig.my_offers;

import bisq.desktop.common.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.mu_sig.MuSigOfferListItem;
import bisq.desktop.main.content.mu_sig.MuSigOfferUtil;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Comparator;

public class MuSigMyOffersView extends View<VBox, MuSigMyOffersModel, MuSigMyOffersController> {
    private static final double SIDE_PADDING = 40;

    private final RichTableView<MuSigOfferListItem> muSigMyOffersListView;

    public MuSigMyOffersView(MuSigMyOffersModel model, MuSigMyOffersController controller) {
        super(new VBox(), model, controller);

        Label headlineLabel = new Label(Res.get("muSig.myOffers.headline"));
        headlineLabel.getStyleClass().add("bisq-easy-container-headline");
        HBox headerHBox = new HBox(headlineLabel);
        headerHBox.setAlignment(Pos.CENTER_LEFT);
        headerHBox.getStyleClass().add("chat-container-header");

        HBox subheader = new HBox();
        subheader.getStyleClass().add("offerbook-subheader");
        subheader.setAlignment(Pos.CENTER);

        muSigMyOffersListView = new RichTableView<>(model.getSortedMuSigMyOffersListItems());
        muSigMyOffersListView.getFooterVBox().setVisible(false);
        muSigMyOffersListView.getFooterVBox().setManaged(false);
        muSigMyOffersListView.getStyleClass().add("mu-sig-my-offers-table");
        configMuSigMyOffersListView();
        VBox.setVgrow(muSigMyOffersListView, Priority.ALWAYS);

        root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        root.getChildren().addAll(headerHBox, Layout.hLine(), subheader, muSigMyOffersListView);
    }

    private void configMuSigMyOffersListView() {
        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.myProfile"))
                .left()
                .comparator(Comparator.comparingLong(MuSigOfferListItem::getTotalScore).reversed())
                .setCellFactory(MuSigOfferUtil.getUserProfileCellFactory())
                .minWidth(100)
                .build());

        // Date
//        muSigMyOffersListView.getSortOrder().add(dateColumn);

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.offerType"))
                .left()
                .minWidth(160)
                .comparator(Comparator.comparing(MuSigOfferListItem::getOfferIntentText))
                .valueSupplier(MuSigOfferListItem::getOfferIntentText)
                .build());

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.baseAmount"))
                .left()
                .minWidth(120)
                .comparator(Comparator.comparing(MuSigOfferListItem::getBaseAmountWithSymbol))
                .valueSupplier(MuSigOfferListItem::getBaseAmountWithSymbol)
                .build());

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.quoteAmount"))
                .left()
                .minWidth(160)
                .comparator(Comparator.comparing(MuSigOfferListItem::getQuoteAmountWithSymbol))
                .valueSupplier(MuSigOfferListItem::getQuoteAmountWithSymbol)
                .build());

        BisqTableColumn<MuSigOfferListItem> priceColumn = new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.price"))
                .left()
                .comparator(Comparator.comparing(MuSigOfferListItem::getPrice))
                .valueSupplier(MuSigOfferListItem::getPrice)
                .tooltipSupplier(MuSigOfferListItem::getPriceTooltip)
                .minWidth(200)
                .build();
        muSigMyOffersListView.getColumns().add(priceColumn);

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .left()
                .title(Res.get("muSig.myOffers.table.header.paymentMethods"))
                .setCellFactory(MuSigOfferUtil.getPaymentCellFactory())
                .minWidth(140)
                .comparator(Comparator.comparing(MuSigOfferListItem::getPaymentMethodsAsString))
                .build());
    }

    @Override
    protected void onViewAttached() {
        muSigMyOffersListView.initialize();
        muSigMyOffersListView.resetSearch();
        muSigMyOffersListView.sort();
    }

    @Override
    protected void onViewDetached() {
    }
}
