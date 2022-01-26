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
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.primary.main.content.trade.components.DirectionSelection;
import bisq.desktop.primary.main.content.trade.components.MarketSelection;
import bisq.desktop.primary.main.content.trade.create.CreateOfferController;
import bisq.desktop.primary.main.content.trade.take.TakeOfferController;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.oracle.marketprice.MarketPriceService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class OfferbookController implements InitWithDataController<OfferbookController.InitData> {
    public static record InitData(Optional<BooleanProperty> showCreateOfferTab,
                                  Optional<BooleanProperty> showTakeOfferTab) {
    }

    private final NetworkService networkService;
    private final IdentityService identityService;
    private final Optional<DataService> dataService;
    private final OfferbookModel model;
    @Getter
    private final OfferbookView view;
    private final ChangeListener<Market> selectedMarketListener;
    private final ChangeListener<Direction> directionListener;
    private final MarketSelection marketSelection;
    private final DirectionSelection directionSelection;
    private Optional<DataService.Listener> dataListener = Optional.empty();

    public OfferbookController(DefaultApplicationService applicationService) {
        networkService = applicationService.getNetworkService();
        identityService = applicationService.getIdentityService();
        dataService = networkService.getDataService();
        MarketPriceService marketPriceService = applicationService.getMarketPriceService();

        marketSelection = new MarketSelection(marketPriceService);
        directionSelection = new DirectionSelection(marketSelection.selectedMarketProperty());

        model = new OfferbookModel(applicationService,
                marketSelection.selectedMarketProperty(),
                directionSelection.directionProperty());

        view = new OfferbookView(model, this, marketSelection.getView(), directionSelection.getView());

        selectedMarketListener = (observable, oldValue, newValue) -> applyMarketChange(newValue);
        directionListener = (observable, oldValue, newValue) -> applyDirectionChange(newValue);
    }

    @Override
    public void initWithData(OfferbookController.InitData data) {
        data.showCreateOfferTab.ifPresent(e -> model.showCreateOfferTab = e);
        data.showTakeOfferTab.ifPresent(e -> model.showTakeOfferTab = e);
    }

    public boolean showCreateOfferTab() {
        return model.showCreateOfferTab.get();
    }

    public boolean showTakeOfferTab() {
        return model.showTakeOfferTab.get();
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

    @Override
    public void onViewAttached() {
        model.showAllMarkets.set(false);
        directionSelection.setDirection(Direction.BUY);

        model.getSelectedMarketProperty().addListener(selectedMarketListener);
        model.getDirectionProperty().addListener(directionListener);

        dataService.ifPresent(dataService -> {
            dataListener = Optional.of(new DataService.Listener() {
                @Override
                public void onNetworkPayloadAdded(NetworkPayload networkPayload) {
                    if (networkPayload instanceof AuthenticatedPayload payload &&
                            payload.getData() instanceof Offer offer) {
                        UIThread.run(() -> model.addOffer(offer));
                    }
                }

                @Override
                public void onNetworkPayloadRemoved(NetworkPayload networkPayload) {
                    if (networkPayload instanceof AuthenticatedPayload payload && payload.getData() instanceof Offer offer) {
                        UIThread.run(() -> model.removeOffer(offer));
                    }
                }
            });
            dataService.addListener(dataListener.get());
            model.fillOfferListItems(dataService.getAuthenticatedPayloadByStoreName("Offer")
                    .filter(payload -> payload.getData() instanceof Offer)
                    .map(payload -> (Offer) payload.getData())
                    .map(offer -> new OfferListItem(offer, model.marketPriceService))
                    .collect(Collectors.toList()));
        });

        updateFilterPredicate();
    }

    @Override
    public void onViewDetached() {
        model.getSelectedMarketProperty().removeListener(selectedMarketListener);
        dataService.ifPresent(dataService -> dataListener.ifPresent(dataService::removeListener));
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
        Identity identity = identityService.findActiveIdentity(offer.getId()).orElseThrow();
        // We do not retire the identity as it might be still used in the chat. For a mature implementation we would
        // need to check if there is any usage still for that identity and if not retire it.
        log.error("onRemoveOffer nodeIdAndKeyPair={}", identity.getNodeIdAndKeyPair());
        networkService.removeData(offer, identity.getNodeIdAndKeyPair())
                .whenComplete((broadCastResultFutures, throwable2) -> {
                    if (throwable2 != null) {
                        UIThread.run(() -> model.setRemoveOfferError(offer, throwable2));
                        return;
                    }
                    broadCastResultFutures.forEach(broadCastResultFuture -> {
                        broadCastResultFuture.whenComplete((broadcastResult, throwable3) -> {
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
        Navigation.navigateTo(NavigationTarget.TAKE_OFFER,
                new TakeOfferController.InitData(item.getOffer(), model.showTakeOfferTab));
    }
}
