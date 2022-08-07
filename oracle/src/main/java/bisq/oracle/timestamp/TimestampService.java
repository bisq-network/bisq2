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

package bisq.oracle.timestamp;

import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle.OracleService;
import bisq.oracle.daobridge.model.AuthorizedDaoBridgeServiceProvider;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyGeneration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class TimestampService implements Service, PersistenceClient<TimestampStore>, MessageListener, DataService.Listener {
    @Getter
    private final TimestampStore persistableStore = new TimestampStore();
    @Getter
    private final Persistence<TimestampStore> persistence;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private Optional<PrivateKey> authorizedPrivateKey = Optional.empty();
    private Optional<PublicKey> authorizedPublicKey = Optional.empty();

    public TimestampService(OracleService.Config config,
                            PersistenceService persistenceService,
                            IdentityService identityService,
                            NetworkService networkService) {
        this.identityService = identityService;
        this.networkService = networkService;

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);

        String privateKey = config.getPrivateKey();
        String publicKey = config.getPublicKey();
        if (privateKey != null && !privateKey.isEmpty() && publicKey != null && !publicKey.isEmpty()) {
            try {
                authorizedPrivateKey = Optional.of(KeyGeneration.generatePrivate(Hex.decode(privateKey)));
                authorizedPublicKey = Optional.of(KeyGeneration.generatePublic(Hex.decode(publicKey)));
            } catch (GeneralSecurityException e) {
                log.error("Invalid authorization keys", e);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.addMessageListener(this);
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(service -> service.getAllAuthenticatedPayload().forEach(this::processAuthenticatedData));

        identityService.getOrCreateIdentity(IdentityService.DEFAULT)
                .whenComplete((identity, throwable) -> {
                    AuthorizedDaoBridgeServiceProvider data = new AuthorizedDaoBridgeServiceProvider(identity.getNetworkId());
                    publishAuthorizedData(data);
                });

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
            AuthorizeTimestampRequest request = ((AuthorizeTimestampRequest) networkMessage);

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
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        processAuthenticatedData(authenticatedData);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> publishAuthorizedData(AuthorizedDistributedData data) {
        return identityService.getOrCreateIdentity(IdentityService.DEFAULT)
                .thenCompose(identity -> networkService.publishAuthorizedData(data,
                        identity.getNodeIdAndKeyPair(),
                        authorizedPrivateKey.orElseThrow(),
                        authorizedPublicKey.orElseThrow()))
                .thenApply(broadCastDataResult -> true);
    }

    private void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedTimestampData) {
            AuthorizedTimestampData data = (AuthorizedTimestampData) authenticatedData.getDistributedData();

            // We might get data published from other oracle nodes and put it into our local store
            String profileId = data.getProfileId();
            if (!persistableStore.getTimestampsByProfileId().containsKey(profileId)) {
                persistableStore.getTimestampsByProfileId().put(profileId, data.getDate());
                persist();
            }
        }
    }
}