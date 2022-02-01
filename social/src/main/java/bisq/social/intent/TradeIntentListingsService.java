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

package bisq.social.intent;

import bisq.common.data.ObservedSet;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class TradeIntentListingsService {
    @Getter
    private final ObservedSet<TradeIntent> tradeIntents = new ObservedSet<>();
    private final DataService dataService;

    public TradeIntentListingsService(NetworkService networkService) {
        checkArgument(networkService.getDataService().isPresent(),
                "networkService.getDataService() is expected to be present if OfferBookService is used");
        dataService = networkService.getDataService().get();
        dataService.addListener(new DataService.Listener() {
            @Override
            public void onNetworkPayloadAdded(NetworkPayload networkPayload) {
                if (networkPayload instanceof AuthenticatedPayload payload &&
                        payload.getData() instanceof TradeIntent offer) {
                    tradeIntents.add(offer);
                }
            }

            @Override
            public void onNetworkPayloadRemoved(NetworkPayload networkPayload) {
                if (networkPayload instanceof AuthenticatedPayload payload &&
                        payload.getData() instanceof TradeIntent offer) {
                    tradeIntents.remove(offer);
                }
            }
        });
    }

    public CompletableFuture<Boolean> initialize() {
        tradeIntents.addAll(dataService.getAuthenticatedPayloadByStoreName("TradeIntent")
                .filter(payload -> payload.getData() instanceof TradeIntent)
                .map(payload -> (TradeIntent) payload.getData())
                .collect(Collectors.toList()));
        return CompletableFuture.completedFuture(true);
    }
}