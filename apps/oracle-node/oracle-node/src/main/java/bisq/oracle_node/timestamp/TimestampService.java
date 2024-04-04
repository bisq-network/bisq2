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

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.persistence.DbSubDirectory;
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
public class TimestampService implements Service, PersistenceClient<TimestampStore>,
        ConfidentialMessageService.Listener, AuthorizedBondedRolesService.Listener {
    @Getter
    private final TimestampStore persistableStore = new TimestampStore();
    @Getter
    private final Persistence<TimestampStore> persistence;
    private final boolean staticPublicKeysProvided;
    private final NetworkService networkService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    @Setter
    private Identity identity;

    public TimestampService(PersistenceService persistenceService,
                            NetworkService networkService,
                            AuthorizedBondedRolesService authorizedBondedRolesService,
                            PrivateKey authorizedPrivateKey,
                            PublicKey authorizedPublicKey,
                            boolean staticPublicKeysProvided) {
        this.networkService = networkService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;
        this.authorizedPrivateKey = authorizedPrivateKey;
        this.authorizedPublicKey = authorizedPublicKey;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.addConfidentialMessageListener(this);
        authorizedBondedRolesService.addListener(this);

        persistableStore.getTimestampsByProfileId().forEach((key, value) -> publishAuthorizedData(new AuthorizedTimestampData(key, value, staticPublicKeysProvided)));

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeConfidentialMessageListener(this);
        authorizedBondedRolesService.removeListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof AuthorizeTimestampRequest) {
            processAuthorizeTimestampRequest((AuthorizeTimestampRequest) envelopePayloadMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // AuthorizedBondedRolesService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof AuthorizedTimestampData) {
            if (isAuthorized(authorizedData)) {
                AuthorizedTimestampData authorizedTimestampData = (AuthorizedTimestampData) authorizedData.getAuthorizedDistributedData();
                // We might get data published from other oracle nodes and put it into our local store.
                String profileId = authorizedTimestampData.getProfileId();
                if (!persistableStore.getTimestampsByProfileId().containsKey(profileId)) {
                    persistableStore.getTimestampsByProfileId().put(profileId, authorizedTimestampData.getDate());
                    persist();
                }
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isAuthorized(AuthorizedData authorizedData) {
        return authorizedBondedRolesService.hasAuthorizedPubKey(authorizedData, BondedRoleType.ORACLE_NODE);
    }

    private CompletableFuture<Boolean> publishAuthorizedData(AuthorizedDistributedData data) {
        return networkService.publishAuthorizedData(data,
                        identity.getNetworkIdWithKeyPair().getKeyPair(),
                        authorizedPrivateKey,
                        authorizedPublicKey)
                .thenApply(broadCastDataResult -> true);
    }

    private void processAuthorizeTimestampRequest(AuthorizeTimestampRequest request) {
        String profileId = request.getProfileId();
        long date;
        if (!persistableStore.getTimestampsByProfileId().containsKey(profileId)) {
            date = new Date().getTime();
            persistableStore.getTimestampsByProfileId().put(profileId, date);
            persist();
        } else {
            // If we got requested again from the user it might be because TTL is running out, and we need 
            // to republish it.
            date = persistableStore.getTimestampsByProfileId().get(profileId);
        }
        publishAuthorizedData(new AuthorizedTimestampData(profileId, date, staticPublicKeysProvided));
    }
}