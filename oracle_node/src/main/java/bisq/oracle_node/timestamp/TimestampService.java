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

package bisq.oracle_node.timestamp;

import bisq.bonded_roles.AuthorizedOracleNode;
import bisq.common.application.Service;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.reputation.data.AuthorizedTimestampData;
import bisq.user.reputation.requests.AuthorizeTimestampRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class TimestampService implements Service, PersistenceClient<TimestampStore>, MessageListener, DataService.Listener {
    @Getter
    private final TimestampStore persistableStore = new TimestampStore();
    @Getter
    private final Persistence<TimestampStore> persistence;
    private final NetworkService networkService;
    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    @Setter
    private AuthorizedOracleNode authorizedOracleNode;
    @Setter
    private Identity identity;

    public TimestampService(PersistenceService persistenceService,
                            NetworkService networkService,
                            PrivateKey authorizedPrivateKey,
                            PublicKey authorizedPublicKey) {
        this.networkService = networkService;
        this.authorizedPrivateKey = authorizedPrivateKey;
        this.authorizedPublicKey = authorizedPublicKey;

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.addMessageListener(this);
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(service -> service.getAuthenticatedData()
                .filter(data -> data.getDistributedData() instanceof AuthorizedTimestampData)
                .map(data -> (AuthorizedTimestampData) data.getDistributedData())
                .forEach(this::processAuthorizedTimestampData));

        persistableStore.getTimestampsByProfileId().forEach((key, value) -> publishAuthorizedData(new AuthorizedTimestampData(key, value)));

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeMessageListener(this);
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof AuthorizeTimestampRequest) {
            processAuthorizeTimestampRequest((AuthorizeTimestampRequest) networkMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedTimestampData) {
            processAuthorizedTimestampData((AuthorizedTimestampData) authenticatedData.getDistributedData());
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<Boolean> publishAuthorizedData(AuthorizedDistributedData data) {
        return networkService.publishAuthorizedData(data,
                        identity.getNodeIdAndKeyPair(),
                        authorizedPrivateKey,
                        authorizedPublicKey)
                .thenApply(broadCastDataResult -> true);
    }

    private void processAuthorizeTimestampRequest(AuthorizeTimestampRequest request) {
        String profileId = request.getProfileId();
        if (!persistableStore.getTimestampsByProfileId().containsKey(profileId)) {
            long now = new Date().getTime();
            persistableStore.getTimestampsByProfileId().put(profileId, now);
            persist();
            publishAuthorizedData(new AuthorizedTimestampData(profileId, now));
        } else {
            // If we got requested again from the user it might be because TTL is running out, and we need 
            // to republish it.
            long date = persistableStore.getTimestampsByProfileId().get(profileId);
            publishAuthorizedData(new AuthorizedTimestampData(profileId, date));
        }
    }

    private void processAuthorizedTimestampData(AuthorizedTimestampData data) {
        // We might get data published from other oracle nodes and put it into our local store.
        String profileId = data.getProfileId();
        if (!persistableStore.getTimestampsByProfileId().containsKey(profileId)) {
            persistableStore.getTimestampsByProfileId().put(profileId, data.getDate());
            persist();
        }
    }
}