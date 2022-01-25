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

import bisq.application.DefaultServiceProvider;
import bisq.common.monetary.Market;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.trade.components.MarketSelection;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import bisq.offer.Offer;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.social.chat.ChatService;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class OfferbookController implements Controller {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final ChatService chatService;
    private final Optional<DataService> dataService;
    private final OfferbookModel model;
    @Getter
    private final OfferbookView view;
    private final ChangeListener<Market> selectedMarketListener;
    private final MarketSelection marketSelection;
    private Optional<DataService.Listener> dataListener = Optional.empty();

    public OfferbookController(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        chatService = serviceProvider.getChatService();
        dataService = networkService.getDataService();
        MarketPriceService marketPriceService = serviceProvider.getMarketPriceService();

        model = new OfferbookModel(serviceProvider);

        marketSelection = new MarketSelection(marketPriceService);
        view = new OfferbookView(model, this, marketSelection.getView());

        selectedMarketListener = (observable, oldValue, newValue) -> model.applyMarketChange(newValue);
    }

    @Override
    public void onViewAttached() {
        model.getSelectedMarket().addListener(selectedMarketListener);
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
                    .map(e -> (Offer) e.getData())
                    .map(OfferListItem::new)
                    .collect(Collectors.toList()));
        });
    }

    @Override
    public void onViewDetached() {
        model.getSelectedMarket().removeListener(selectedMarketListener);
        dataService.ifPresent(dataService -> dataListener.ifPresent(dataService::removeListener));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // UI
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void onActionButtonClicked(OfferListItem item) {
        if (model.isMyOffer(item)) {
            onRemoveOffer(item);
        } else {
            onTakeOffer(item);
        }
    }

    private void onRemoveOffer(OfferListItem item) {
        Offer offer = item.getOffer();
        Identity identity = identityService.findActiveIdentity(offer.getId()).orElseThrow();
        // We do not retire the identity as it might be still used in the chat. For a mature implementation we would
        // need to check if there is any usage still for that identity and if not retire it.
        log.error("onRemoveTradeIntent nodeIdAndKeyPair={}", identity.getNodeIdAndKeyPair());
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
        Navigation.navigateTo(NavigationTarget.TAKE_OFFER, item.getOffer());
    }

}
