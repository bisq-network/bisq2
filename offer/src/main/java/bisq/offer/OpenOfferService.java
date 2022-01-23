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

package bisq.offer;

import bisq.network.NetworkService;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

public class OpenOfferService {

    private final NetworkService networkService;
    private final Set<OpenOffer> openOffers = new CopyOnWriteArraySet<>();

    public OpenOfferService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        //todo
        future.complete(true);
        return future;
    }

   /* public void createNewOffer(long askAmount) {
        Map<Transport.Type, Address> map = Map.of(Transport.Type.CLEAR, Address.localHost(3333));
        NetworkId makerNetworkId = new NetworkId(map, new PubKey(null, "default"), "default");
        SwapSide askSwapSide = new SwapSide(Coin.asBtc(askAmount), List.of());
        SwapSide bidSwapSide = new SwapSide(Fiat.of(5000, "USD"), List.of(FiatSettlement.ZELLE));
        SwapOffer offer = new SwapOffer(bidSwapSide,
                askSwapSide,
                "USD",
                List.of(SwapProtocolType.REPUTATION, SwapProtocolType.MULTISIG),
                makerNetworkId);
        networkService.addData(offer);
    }*/

    public void newOpenOffer(Offer offer) {
        OpenOffer openOffer = new OpenOffer(offer);
        openOffers.add(openOffer);
        //  Persistence.write(openOffers);
    }

    public void shutdown() {

    }
}
