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
import bisq.common.timer.Scheduler;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class MuSigOfferService implements Service {
    private final MuSigOfferbookService muSigOfferbookService;
    private final MyMuSigOffersService myMuSigOffersService;
    private Scheduler republishMyOffersScheduler;

    public MuSigOfferService(PersistenceService persistenceService,
                             NetworkService networkService,
                             IdentityService identityService) {
        this.muSigOfferbookService = new MuSigOfferbookService(networkService, identityService);
        this.myMuSigOffersService = new MyMuSigOffersService(persistenceService);
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        republishMyOffers();
        stopRepublishMyOffersScheduler();
        republishMyOffersScheduler = Scheduler.run(this::republishMyOffers)
                .host(this)
                .runnableName("republishMyOffers")
                .periodically(1, 60, TimeUnit.MINUTES);

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        stopRepublishMyOffersScheduler();
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public CompletableFuture<BroadcastResult> publishAndAddOffer(String offerId) {
        return findOffer(offerId)
                .map(this::publishAndAddOffer)
                .orElse(CompletableFuture.failedFuture(new RuntimeException("Offer with not found. OfferID=" + offerId)));
    }

    public CompletableFuture<BroadcastResult> publishAndAddOffer(MuSigOffer offer) {
        myMuSigOffersService.addOffer(offer);
        return muSigOfferbookService.publishToNetwork(offer);
    }

    // Publish offer to network after it had been deactivated
    public CompletableFuture<BroadcastResult> activateOffer(MuSigOffer offer) {
        myMuSigOffersService.activateOffer(offer);
        return muSigOfferbookService.publishToNetwork(offer);
    }

    // Removes offer from network but leaves it in my offers
    public CompletableFuture<BroadcastResult> deactivateOffer(MuSigOffer offer) {
        myMuSigOffersService.deactivateOffer(offer);
        return muSigOfferbookService.removeFromNetwork(offer);
    }

    public CompletableFuture<BroadcastResult> removeOffer(String offerId) {
        return findOffer(offerId)
                .map(this::removeOffer)
                .orElse(CompletableFuture.failedFuture(new RuntimeException("Offer with not found. OfferID=" + offerId)));
    }

    public CompletableFuture<BroadcastResult> removeOffer(MuSigOffer offer) {
        myMuSigOffersService.removeOffer(offer);
        return muSigOfferbookService.removeFromNetwork(offer);
    }

    public Optional<MuSigOffer> findOffer(String offerId) {
        return muSigOfferbookService.findOffer(offerId);
    }

    public void republishMyOffers() {
        myMuSigOffersService.getActivatedOffers().forEach(this::publishAndAddOffer);
    }

    public MuSigOffer cloneOffer(MuSigOffer offer) {
        return MuSigOffer.fromProto(offer.toProto(true));
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void stopRepublishMyOffersScheduler() {
        if (republishMyOffersScheduler != null) {
            republishMyOffersScheduler.stop();
            republishMyOffersScheduler = null;
        }
    }
}