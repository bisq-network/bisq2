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

package network.misq.offer;

import network.misq.account.FiatTransfer;
import network.misq.common.monetary.Coin;
import network.misq.common.monetary.Fiat;
import network.misq.contract.AssetTransfer;
import network.misq.contract.SwapProtocolType;
import network.misq.network.p2p.INetworkService;
import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.security.PubKey;
import network.misq.wallets.Wallet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

public class OpenOfferRepository {
    // Expected dependency for deactivating offers if not sufficient wallet balance
    Wallet wallet;

    private final INetworkService networkService;
    private final Set<OpenOffer> openOffers = new CopyOnWriteArraySet<>();

    public OpenOfferRepository(INetworkService networkService) {
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        //todo
        future.complete(true);
        return future;
    }

    public void createNewOffer(long askAmount) {
        Map<Transport.Type, Address> map = Map.of(Transport.Type.CLEAR, Address.localHost(3333));
        NetworkId makerNetworkId = new NetworkId(map, new PubKey(null, "default"), "default");
        Asset askAsset = new Asset(Coin.asBtc(askAmount), List.of(), AssetTransfer.Type.MANUAL);
        Asset bidAsset = new Asset(Fiat.of(5000, "USD"), List.of(FiatTransfer.ZELLE), AssetTransfer.Type.MANUAL);
        Offer offer = new Offer(List.of(SwapProtocolType.REPUTATION, SwapProtocolType.MULTISIG),
                makerNetworkId, bidAsset, askAsset);
        networkService.addData(offer);
    }

    public void newOpenOffer(Offer offer) {
        OpenOffer openOffer = new OpenOffer(offer);
        openOffers.add(openOffer);
        //  Persistence.write(openOffers);
    }

    public void shutdown() {

    }
}
