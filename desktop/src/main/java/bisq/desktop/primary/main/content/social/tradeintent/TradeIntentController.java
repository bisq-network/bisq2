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

package bisq.desktop.primary.main.content.social.tradeintent;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.social.components.UserProfileDisplay;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.social.chat.ChatService;
import bisq.social.intent.TradeIntent;
import bisq.social.intent.TradeIntentListingsService;
import bisq.social.intent.TradeIntentService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TradeIntentController implements Controller/*, ChatService.Listener*/ {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final ChatService chatService;
    private final Optional<DataService> dataService;
    private final TradeIntentModel model;
    @Getter
    private final TradeIntentView view;
    private final TradeIntentListingsService tradeIntentListingsService;
    private final TradeIntentService tradeIntentService;
    private final UserProfileDisplay userProfileDisplay;
    private Optional<DataService.Listener> dataListener = Optional.empty();
    private Pin listItemsPin;

    public TradeIntentController(DefaultApplicationService applicationService) {
        networkService = applicationService.getNetworkService();
        identityService = applicationService.getIdentityService();
        chatService = applicationService.getChatService();
        tradeIntentService = applicationService.getTradeIntentService();
        tradeIntentListingsService = applicationService.getTradeIntentListingsService();
        dataService = networkService.getDataService();

        userProfileDisplay = new UserProfileDisplay(applicationService.getUserProfileService());
        model = new TradeIntentModel(applicationService);
        view = new TradeIntentView(model, this, userProfileDisplay.getRoot());
    }

    @Override
    public void onViewAttached() {
        listItemsPin = FxBindings.<TradeIntent, TradeIntentListItem>bind(model.getListItems())
                .map(TradeIntentListItem::new)
                .to(tradeIntentListingsService.getTradeIntents());
    }

    @Override
    public void onViewDetached() {
        listItemsPin.unbind();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // UI
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void onCreateTradeIntent(String ask, String bid) {
        tradeIntentService.createTradeIntent(ask, bid)
                .whenComplete((tradeIntent, throwable) -> {
                    if (throwable == null) {
                        model.getTradeIntentProperty().set(tradeIntent);
                        onPublishTradeIntent();
                    } else {
                        //todo provide error to UI
                    }
                });
    }

    public void onPublishTradeIntent() {
        tradeIntentService.publishTradeIntent(model.getTradeIntentProperty().get());
    }

    public void onActionButtonClicked(TradeIntentListItem item) {
        if (model.isMyTradeIntent(item)) {
            onRemoveTradeIntent(item);
        } else {
            onContactPeer(item);
        }
    }

    private void onRemoveTradeIntent(TradeIntentListItem item) {
        TradeIntent tradeIntent = item.getTradeIntent();
        tradeIntentService.removeMyTradeIntent(tradeIntent)
                .whenComplete((broadCastResultFutures, throwable2) -> {
                    if (throwable2 != null) {
                        // UIThread.run(() -> model.setRemoveOfferError(tradeIntent, throwable2));
                        return;
                    }
                    broadCastResultFutures.entrySet().forEach(broadCastResultFuture -> {
                        broadCastResultFuture.getValue().whenComplete((broadcastResult, throwable3) -> {
                            if (throwable3 != null) {
                                // UIThread.run(() -> model.setRemoveOfferError(tradeIntent, throwable3));
                                return;
                            }
                            // UIThread.run(() -> model.setRemoveOfferResult(tradeIntent, broadcastResult));
                        });
                    });
                });
    }

    private void onContactPeer(TradeIntentListItem item) {
        Navigation.navigateTo(NavigationTarget.HANGOUT, item.getTradeIntent());
    }
}
