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

package bisq.offer.mu_sig;

import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.common.util.CompletableFutureUtils;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.offer.Offer;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MuSigOfferbookService implements Service, DataService.Listener {
    private final ObservableSet<MuSigOffer> offers = new ObservableSet<>();
    private final NetworkService networkService;
    private final IdentityService identityService;

    public MuSigOfferbookService(NetworkService networkService, IdentityService identityService) {
        this.networkService = networkService;
        this.identityService = identityService;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        networkService.getDataService().ifPresent(dataService ->
                dataService.getAuthenticatedData().forEach(this::onAuthenticatedDataAdded));
        networkService.addDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        networkService.removeDataServiceListener(this);
        offers.clear();
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // DataService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof MuSigOfferMessage) {
            processAddedMuSigOfferMessage((MuSigOfferMessage) distributedData);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof MuSigOfferMessage) {
            processRemovedMuSigOfferMessage((MuSigOfferMessage) distributedData);
        }
    }

    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public CompletableFuture<BroadcastResult> publishToNetwork(MuSigOffer muSigOffer) {
        return identityService.findActiveIdentity(muSigOffer.getMakerNetworkId())
                .map(identity -> networkService.publishAuthenticatedData(new MuSigOfferMessage(muSigOffer),
                        identity.getNetworkIdWithKeyPair().getKeyPair()))
                .orElse(CompletableFuture.failedFuture(new RuntimeException("No identity found for networkNodeId used in the muSigOffer")));
    }

    public CompletableFuture<BroadcastResult> removeFromNetwork(MuSigOffer muSigOffer) {
        return findIdentity(muSigOffer)
                .map(identity -> networkService.removeAuthenticatedData(new MuSigOfferMessage(muSigOffer),
                        identity.getNetworkIdWithKeyPair().getKeyPair()))
                .orElse(CompletableFuture.failedFuture(new RuntimeException("No identity found for networkNodeId used in the muSigOffer")));
    }

    public CompletableFuture<List<BroadcastResult>> refreshMyOffers() {
        Set<MuSigOffer> offers = new HashSet<>(this.offers);
        return CompletableFutureUtils.allOf(offers.stream().map(muSigOffer ->
                identityService.findActiveIdentity(muSigOffer.getMakerNetworkId())
                        .map(identity -> networkService.refreshAuthenticatedData(new MuSigOfferMessage(muSigOffer),
                                identity.getNetworkIdWithKeyPair().getKeyPair()))
                        .orElse(CompletableFuture.failedFuture(new RuntimeException("No identity found for networkNodeId used in the muSigOffer")))));
    }

    public synchronized Optional<MuSigOffer> findOffer(String offerId) {
        return offers.stream().filter(offer -> offer.getId().equals(offerId)).findAny();
    }

    public synchronized Optional<MuSigOffer> findOffer(MuSigOffer muSigOffer) {
        return offers.stream().filter(offer -> offer.equals(muSigOffer)).findAny();
    }

    public ReadOnlyObservableSet<MuSigOffer> getObservableOffers() {
        return offers;
    }

    public Set<MuSigOffer> getOffers() {
        return offers.getUnmodifiableSet();
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private boolean processAddedMuSigOfferMessage(MuSigOfferMessage offerMessage) {
        return offers.add(offerMessage.getOffer());
    }

    private boolean processRemovedMuSigOfferMessage(MuSigOfferMessage offerMessage) {
        return offers.remove(offerMessage.getOffer());
    }

    private Optional<Identity> findIdentity(Offer<?, ?> offer) {
        return identityService.findActiveIdentity(offer.getMakerNetworkId());
    }
}