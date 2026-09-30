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

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.options.CollateralOption;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Only the maker can publish a MuSig offer. Otherwise another user could publish a copy of the offer and remove the
 * copy, and the offerbook would remove the offer of the maker (see docs/specifications/network/storage-ownership.md).
 */
class MuSigOfferMessageOwnerTest {
    @TempDir
    Path tempDir;

    private final KeyPair makerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private AuthenticatedDataStorageService store;
    private MuSigOfferbookService offerbook;
    private MuSigOffer offer;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(MuSigOfferMessage.class, MuSigOfferMessage.META_DATA);
        store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(),
                "store",
                MuSigOfferMessage.class.getSimpleName());
        // The listener methods of the offerbook use neither the network service nor the identity service
        offerbook = new MuSigOfferbookService(null, null);
        store.addListener(new AuthenticatedDataStorageService.Listener() {
            @Override
            public void onAdded(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
                offerbook.onAuthenticatedDataAdded(authenticatedData);
            }

            @Override
            public void onRemoved(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
                offerbook.onAuthenticatedDataRemoved(authenticatedData);
            }
        });

        NetworkId makerNetworkId = new NetworkId(new AddressByTransportTypeMap(Map.of(TransportType.CLEAR,
                Address.from("127.0.0.1", 9999))), new PubKey(makerKeyPair.getPublic(), "maker"));
        offer = new MuSigOffer("offerId",
                makerNetworkId,
                Direction.SELL,
                new Market("BTC", "USD", "Bitcoin", "US Dollar"),
                new BaseSideFixedAmountSpec(1_000_000),
                new MarketPriceSpec(),
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                List.of(new CollateralOption(MuSigOffer.DEFAULT_BUYER_SECURITY_DEPOSIT,
                        MuSigOffer.DEFAULT_SELLER_SECURITY_DEPOSIT)),
                "1");
    }

    // The store writes asynchronously. A write queued now completes after all earlier ones.
    @AfterEach
    void waitForPendingWrites() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    @Test
    void copyOfOfferPublishedByAnotherUserIsRejected() throws Exception {
        DefaultAuthenticatedData authenticatedData = new DefaultAuthenticatedData(new MuSigOfferMessage(offer));
        AddAuthenticatedDataRequest makerRequest = AddAuthenticatedDataRequest.from(store, authenticatedData,
                makerKeyPair);
        assertTrue(store.add(makerRequest).isSuccess());
        assertTrue(offerbook.findOffer(offer).isPresent());

        AddAuthenticatedDataRequest copy = AddAuthenticatedDataRequest.from(store, authenticatedData, otherKeyPair);
        assertFalse(copy.isSignatureInvalid());
        DataStorageResult addResult = store.add(copy);
        // The other user has no entry, so its remove request cannot remove the offer of the maker
        DataStorageResult removeResult = store.remove(RemoveAuthenticatedDataRequest.from(store, authenticatedData,
                otherKeyPair));

        assertTrue(addResult.isOwnerInvalid(), addResult.toString());
        assertTrue(removeResult.isNoEntry(), removeResult.toString());
        assertEquals(makerRequest, store.getPersistableStore().getMap().get(makerRequest.getStorageKey()));
        assertTrue(offerbook.findOffer(offer).isPresent());
    }

    @Test
    void makerCanRemoveOffer() throws Exception {
        DefaultAuthenticatedData authenticatedData = new DefaultAuthenticatedData(new MuSigOfferMessage(offer));
        assertTrue(store.add(AddAuthenticatedDataRequest.from(store, authenticatedData, makerKeyPair)).isSuccess());

        DataStorageResult removeResult = store.remove(RemoveAuthenticatedDataRequest.from(store, authenticatedData,
                makerKeyPair));

        assertTrue(removeResult.isSuccess(), removeResult.toString());
        assertFalse(removeResult.isNoEntry(), removeResult.toString());
        assertTrue(offerbook.findOffer(offer).isEmpty());
    }
}
