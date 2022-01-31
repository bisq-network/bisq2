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

package bisq.desktop.primary.main.content.trade.offerbook;

import bisq.application.DefaultApplicationService;
import bisq.common.monetary.Market;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.primary.main.content.trade.components.DirectionSelection;
import bisq.desktop.primary.main.content.trade.components.MarketSelection;
import bisq.desktop.primary.main.content.trade.create.CreateOfferController;
import bisq.desktop.primary.main.content.trade.take.TakeOfferController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.offer.OfferBookService;
import bisq.offer.OfferService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OfferbookController implements Controller {
    private final OfferbookModel model;
    @Getter
    private final OfferbookView view;
    private final ChangeListener<Market> selectedMarketListener;
    private final ChangeListener<Direction> directionListener;
    private final MarketSelection marketSelection;
    private final DirectionSelection directionSelection;
    private final OfferService offerService;
    private final OfferBookService offerBookService;

    private int offerBindingKey;

    public OfferbookController(DefaultApplicationService applicationService) {
        offerBookService = applicationService.getOfferBookService();
        offerService = applicationService.getOfferService();

        marketSelection = new MarketSelection(applicationService.getUserService());
        directionSelection = new DirectionSelection(marketSelection.selectedMarketProperty());

        model = new OfferbookModel(applicationService,
                marketSelection.selectedMarketProperty(),
                directionSelection.directionProperty());

        view = new OfferbookView(model, this, marketSelection.getView(), directionSelection.getView());

        selectedMarketListener = (observable, oldValue, newValue) -> applyMarketChange(newValue);
        directionListener = (observable, oldValue, newValue) -> applyDirectionChange(newValue);
    }

    @Override
    public void onViewAttached() {
        offerBindingKey = offerBookService.getOffers().bind(model.getListItems(),
                offer -> new OfferListItem(offer, model.marketPriceService),
                UIThread::run);
        model.showAllMarkets.set(false);
        directionSelection.setDirection(Direction.BUY);

        model.getSelectedMarketProperty().addListener(selectedMarketListener);
        model.getDirectionProperty().addListener(directionListener);

        updateFilterPredicate();
    }

    @Override
    public void onViewDetached() {
        offerBookService.getOffers().unbind(offerBindingKey);
        model.getSelectedMarketProperty().removeListener(selectedMarketListener);
    }

    public ReadOnlyBooleanProperty showCreateOfferTab() {
        return model.showCreateOfferTab;
    }

    public ReadOnlyBooleanProperty getShowTakeOfferTab() {
        return model.showTakeOfferTab;
    }

    private void applyMarketChange(Market market) {
        if (market != null) {
            model.priceHeaderTitle.set(Res.offerbook.get("offerbook.table.header.price", market.quoteCurrencyCode(), market.baseCurrencyCode()));
            model.baseAmountHeaderTitle.set(Res.offerbook.get("offerbook.table.header.baseAmount", market.baseCurrencyCode()));
            model.quoteAmountHeaderTitle.set(Res.offerbook.get("offerbook.table.header.quoteAmount", market.quoteCurrencyCode()));
        }
        updateFilterPredicate();
    }

    private void applyDirectionChange(Direction takersDirection) {
        updateFilterPredicate();
    }

    private void updateFilterPredicate() {
        model.filteredItems.setPredicate(item -> {
            if (!model.showAllMarkets.get() && !item.getOffer().getMarket().equals(model.selectedMarketProperty().get())) {
                return false;
            }
            if (item.getOffer().getDirection() == model.directionProperty().get()) {
                return false;
            }

            return true;
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // UI
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    void onShowAllMarketsChanged(boolean selected) {
        model.showAllMarkets.set(selected);
        model.marketSelectionDisabled.set(selected);
        updateFilterPredicate();
    }

    void onCreateOffer() {
        model.showCreateOfferTab.set(true);
        Navigation.navigateTo(NavigationTarget.CREATE_OFFER,
                new CreateOfferController.InitData(model.selectedMarketProperty().get(),
                        model.directionProperty().get(),
                        model.showCreateOfferTab));
    }

    void onActionButtonClicked(OfferListItem item) {
        if (model.isMyOffer(item)) {
            onRemoveOffer(item);
        } else {
            onTakeOffer(item);
        }
    }

    void onUpdateItemWithButton(OfferListItem item, BisqButton button) {
        if (item != null && button instanceof BisqIconButton bisqIconButton) {
            boolean isMyOffer = model.isMyOffer(item);
            bisqIconButton.setFixWidth(200);
            if (isMyOffer) {
                bisqIconButton.getIcon().setId("image-remove");
                bisqIconButton.setId("button-inactive");
            } else {
                if (item.getOffer().getDirection().mirror().isBuy()) {
                    bisqIconButton.getIcon().setId("image-buy-white");
                    bisqIconButton.setId("buy-button");
                    // bisqIconButton.setText(Res.offerbook.get("direction.label.buy", item.getOffer().getMarket().baseCurrencyCode()));
                } else {
                    bisqIconButton.getIcon().setId("image-sell-white");
                    bisqIconButton.setId("sell-button");
                    //  bisqIconButton.setText(Res.offerbook.get("direction.label.sell", item.getOffer().getMarket().baseCurrencyCode()));
                }
            }
        }
    }

    private void onRemoveOffer(OfferListItem item) {
        Offer offer = item.getOffer();
        offerService.removeMyOffer(item.getOffer())
                .whenComplete((broadCastResultFutures, throwable2) -> {
                    if (throwable2 != null) {
                        UIThread.run(() -> model.setRemoveOfferError(offer, throwable2));
                        return;
                    }
                    broadCastResultFutures.entrySet().forEach(broadCastResultFuture -> {
                        broadCastResultFuture.getValue().whenComplete((broadcastResult, throwable3) -> {
                            if (throwable3 != null) {
                                UIThread.run(() -> model.setRemoveOfferError(offer, throwable3));
                                return;
                            }
                            UIThread.run(() -> model.setRemoveOfferResult(offer, broadcastResult));
                        });
                    });
                });
    }

    private void onTakeOffer(OfferListItem item) {
        model.showTakeOfferTab.set(true);
        Navigation.navigateTo(NavigationTarget.TAKE_OFFER, new TakeOfferController.InitData(item.getOffer(), model.showTakeOfferTab));
    }
}
