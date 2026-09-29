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

package bisq.network.p2p.services.data.storage.auth;

import bisq.common.data.ByteArray;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.security.DigestUtil;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Oracle and seed nodes of earlier versions published their authorized data with the key pair of their network
 * identity instead of their authorized key pair, which owns the data. For the entries of a store which the earlier key
 * pair published and the authorized key pair authorized, this publishes the data again with the authorized key pair
 * and removes the entries of the earlier key pair (see docs/specifications/network/storage-ownership.md). It removes an
 * entry only if the store accepted the new entry, or if the owner removed the data.
 * <p>
 * Only oracle and seed nodes use it. It can be removed when no entry of an earlier publisher is valid anymore:
 * AuthorizedData.LEGACY_OWNER_CUTOFF_DATE plus the time to live of the transition types.
 */
public final class AuthorizedDataPublisherMigration {
    private AuthorizedDataPublisherMigration() {
    }

    // Applies the requests to the store and returns those which it accepted, for broadcasting
    public static List<AuthenticatedDataRequest> migrate(AuthenticatedDataStorageService store,
                                                         KeyPair earlierPublisherKeyPair,
                                                         KeyPair authorizedKeyPair) throws GeneralSecurityException {
        byte[] earlierPublicKeyBytes = earlierPublisherKeyPair.getPublic().getEncoded();
        byte[] authorizedPublicKeyBytes = authorizedKeyPair.getPublic().getEncoded();
        byte[] authorizedPubKeyHash = DigestUtil.hash(authorizedPublicKeyBytes);
        List<AddAuthenticatedDataRequest> entriesOfEarlierPublisher = store.getPersistableStore().getMap().values()
                .stream()
                .filter(AddAuthenticatedDataRequest.class::isInstance)
                .map(AddAuthenticatedDataRequest.class::cast)
                .filter(request -> !request.isExpired())
                .filter(request -> Arrays.equals(request.getSignaturePubKeyEncoded(), earlierPublicKeyBytes))
                .filter(request -> request.getAuthenticatedSequentialData().getAuthenticatedData()
                        instanceof AuthorizedData authorizedData &&
                        Arrays.equals(authorizedData.getAuthorizedPublicKeyBytes(), authorizedPublicKeyBytes) &&
                        authorizedData.isOwner(authorizedPubKeyHash))
                .toList();

        List<AuthenticatedDataRequest> acceptedRequests = new ArrayList<>();
        for (AddAuthenticatedDataRequest entry : entriesOfEarlierPublisher) {
            AuthorizedData authorizedData = (AuthorizedData) entry.getAuthenticatedSequentialData().getAuthenticatedData();
            if (!isRemovedByOwner(store, authorizedData, authorizedPublicKeyBytes)) {
                // Nodes of earlier versions store the entries of both key pairs under one storage key (V1), and they
                // accept a request only with a higher sequence number than the stored one. With a sequence number above
                // the one of the remove request below, they replace the entry of the earlier key pair in any order.
                // Then they also apply later remove requests of the authorized key pair.
                AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store,
                        authorizedData,
                        authorizedKeyPair,
                        entry.getSequenceNumber() + 2);
                if (!store.add(addRequest).isSuccess()) {
                    // For example, the store is full. The entry of the earlier key pair is the source of the data, so
                    // it stays, and a later run migrates it.
                    continue;
                }
                acceptedRequests.add(addRequest);
            }
            RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(store,
                    authorizedData,
                    earlierPublisherKeyPair);
            if (store.remove(removeRequest).isSuccess()) {
                acceptedRequests.add(removeRequest);
            }
        }
        return acceptedRequests;
    }

    // Data which the owner removed is not published again, also if the entry of the earlier key pair is still stored.
    // For example, an oracle node can remove a bonded role with its authorized key pair before the migration runs.
    private static boolean isRemovedByOwner(AuthenticatedDataStorageService store,
                                            AuthorizedData authorizedData,
                                            byte[] authorizedPublicKeyBytes) {
        ByteArray storageKeyOfOwner = AddAuthenticatedDataRequest.getStorageKey(
                AddAuthenticatedDataRequest.getAuthenticatedDataHash(authorizedData),
                authorizedPublicKeyBytes);
        return store.getPersistableStore().getMap().get(storageKeyOfOwner) instanceof RemoveAuthenticatedDataRequest;
    }
}
