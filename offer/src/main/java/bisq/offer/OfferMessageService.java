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

import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class OfferMessageService implements Service, DataService.Listener {
    @Getter
    private final ObservableSet<Offer<?, ?>> offers = new ObservableSet<>();
    private final NetworkService networkService;
    private final IdentityService identityService;

    public OfferMessageService(NetworkService networkService, IdentityService identityService) {
        this.networkService = networkService;
        this.identityService = identityService;
        checkArgument(networkService.getDataService().isPresent(),
                "networkService.getDataService() is expected to be present if OfferBookService is used");
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(dataService ->
                dataService.getAllAuthenticatedData().forEach(this::onAuthenticatedDataAdded));
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof OfferMessage) {
            processAddedMessage((OfferMessage) distributedData);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof OfferMessage) {
            processRemovedMessage((OfferMessage) distributedData);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<DataService.BroadCastDataResult> addToNetwork(Offer<?, ?> offer) {
        return identityService.findActiveIdentityByNodeId(offer.getMakerNetworkId().getNodeId())
                .map(identity -> networkService.publishAuthenticatedData(new OfferMessage(offer), identity.getNodeIdAndKeyPair()))
                .orElse(CompletableFuture.failedFuture(new RuntimeException("No identity found for networkNodeId used in the offer")));
    }

    public CompletableFuture<DataService.BroadCastDataResult> removeFromNetwork(Offer<?, ?> offer) {
        return findIdentity(offer)
                .map(identity -> networkService.removeAuthenticatedData(new OfferMessage(offer), identity.getNodeIdAndKeyPair()))
                .orElse(CompletableFuture.failedFuture(new RuntimeException("No identity found for networkNodeId used in the offer")));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean processAddedMessage(OfferMessage offerMessage) {
        return offers.add(offerMessage.getOffer());
    }

    private boolean processRemovedMessage(OfferMessage offerMessage) {
        return offers.remove(offerMessage.getOffer());
    }

    private Optional<Identity> findIdentity(Offer<?, ?> offer) {
        return identityService.findActiveIdentityByNodeId(offer.getMakerNetworkId().getNodeId());
    }
}