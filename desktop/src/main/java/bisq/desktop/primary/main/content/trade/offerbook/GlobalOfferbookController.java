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
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.CachingController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.MarketSelection;
import bisq.desktop.primary.main.content.trade.components.DirectionSelection;
import bisq.desktop.primary.main.content.multiSig.takeOffer.TakeOfferController;
import bisq.i18n.Res;
import bisq.offer.Offer;
import bisq.offer.OfferBookService;
import bisq.offer.OpenOfferService;
import bisq.offer.spec.Direction;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.control.Button;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class GlobalOfferbookController implements CachingController {
    private final GlobalOfferbookModel model;
    @Getter
    private final GlobalOfferbookView view;
    private final MarketSelection marketSelection;
    private final DirectionSelection directionSelection;
    private final OpenOfferService openOfferService;
    private final OfferBookService offerBookService;

    private Pin offerListPin;
    private Subscription selectedMarketSubscription, directionSubscription;

    public GlobalOfferbookController(DefaultApplicationService applicationService) {
        offerBookService = applicationService.getOfferBookService();
        openOfferService = applicationService.getOpenOfferService();

        marketSelection = new MarketSelection(applicationService.getSettingsService());
        directionSelection = new DirectionSelection();

        model = new GlobalOfferbookModel(applicationService);
        view = new GlobalOfferbookView(model, this, marketSelection.getRoot(), directionSelection.getRoot());
    }

    @Override
    public void onActivate() {
        selectedMarketSubscription = EasyBind.subscribe(marketSelection.selectedMarketProperty(),
                selectedMarket -> {
                    model.selectedMarket = selectedMarket;
                    directionSelection.setSelectedMarket(selectedMarket);
                    applyMarketChange(selectedMarket);
                });
        directionSubscription = EasyBind.subscribe(directionSelection.directionProperty(),
                direction -> {
                    model.direction = direction;
                    applyDirectionChange(direction);
                });
        offerListPin = FxBindings.<Offer, GlobalOfferListItem>bind(model.getListItems())
                .map(offer -> new GlobalOfferListItem(offer, model.marketPriceService))
                .to(offerBookService.getOffers());

        model.showAllMarkets.set(false);
        directionSelection.setDirection(Direction.BUY);

        updateFilterPredicate();
    }

    @Override
    public void onDeactivate() {
        selectedMarketSubscription.unsubscribe();
        directionSubscription.unsubscribe();
        offerListPin.unbind();
    }

    public boolean showCreateOfferTab() {
        return model.showCreateOfferTab;
    }

    public ReadOnlyBooleanProperty getShowTakeOfferTab() {
        return model.showTakeOfferTab;
    }

    private void applyMarketChange(Market market) {
        if (market != null) {
            model.priceHeaderTitle.set(Res.get("offerbook.table.header.price", market.quoteCurrencyCode(), market.baseCurrencyCode()));
            model.baseAmountHeaderTitle.set(Res.get("offerbook.table.header.baseAmount", market.baseCurrencyCode()));
            model.quoteAmountHeaderTitle.set(Res.get("offerbook.table.header.quoteAmount", market.quoteCurrencyCode()));
        }
        updateFilterPredicate();
    }

    private void applyDirectionChange(Direction takersDirection) {
        updateFilterPredicate();
    }

    private void updateFilterPredicate() {
        model.filteredItems.setPredicate(item -> {
            if (!model.showAllMarkets.get() && !item.getOffer().getMarket().equals(model.selectedMarket)) {
                return false;
            }
            if (item.getOffer().getDirection() == model.direction) {
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

    void onActionButtonClicked(GlobalOfferListItem item) {
        if (model.isMyOffer(item)) {
            onRemoveOffer(item);
        } else {
            onTakeOffer(item);
        }
    }

    void onUpdateItemWithButton(GlobalOfferListItem item, Button button) {
        if (item != null && button instanceof BisqIconButton bisqIconButton) {
            boolean isMyOffer = model.isMyOffer(item);
            bisqIconButton.setMinWidth(200);
            bisqIconButton.setMaxWidth(200);
            if (isMyOffer) {
                bisqIconButton.getIcon().setId("image-remove");
                bisqIconButton.setId("button-inactive");
            } else {
                if (item.getOffer().getDirection().mirror().isBuy()) {
                    bisqIconButton.getIcon().setId("image-buy-white");
                    bisqIconButton.setId("buy-button");
                    // bisqIconButton.setText(Res.get("direction.label.buy", item.getOffer().getMarket().baseCurrencyCode()));
                } else {
                    bisqIconButton.getIcon().setId("image-sell-white");
                    bisqIconButton.setId("sell-button");
                    //  bisqIconButton.setText(Res.get("direction.label.sell", item.getOffer().getMarket().baseCurrencyCode()));
                }
            }
        }
    }

    private void onRemoveOffer(GlobalOfferListItem item) {
        Offer offer = item.getOffer();
        openOfferService.removeMyOffer(item.getOffer())
                .whenComplete((broadCastResultFutures, throwable2) -> {
                    if (throwable2 != null) {
                        UIThread.run(() -> model.setRemoveOfferError(offer, throwable2));
                        return;
                    }
                    broadCastResultFutures.forEach((key, value) -> value.whenComplete((broadcastResult, throwable3) -> {
                        if (throwable3 != null) {
                            UIThread.run(() -> model.setRemoveOfferError(offer, throwable3));
                            return;
                        }
                        UIThread.run(() -> model.setRemoveOfferResult(offer, broadcastResult));
                    }));
                });
    }

    private void onTakeOffer(GlobalOfferListItem item) {
        model.showTakeOfferTab.set(true);
        Navigation.navigateTo(NavigationTarget.MULTI_SIG_TAKE_OFFER, new TakeOfferController.InitData(item.getOffer(), model.showTakeOfferTab));
    }
}
