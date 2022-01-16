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

package bisq.identity;


import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class IdentityService implements PersistenceClient<IdentityModel> {
    public final static String DEFAULT = "default";

    @Getter
    private final Persistence<IdentityModel> persistence;
    private final KeyPairService keyPairService;
    private final NetworkService networkService;
    private final IdentityModel identityModel = new IdentityModel();

    public IdentityService(PersistenceService persistenceService, KeyPairService keyPairService, NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, "db", "identityModel");
        this.keyPairService = keyPairService;
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        //todo create identity pool
        identityModel.getIdentityByDomainId().values().forEach(identity -> {
            networkService.maybeInitializeServer(identity.nodeId(), identity.pubKey())
                    .forEach((key, value) -> value
                            .whenComplete((r, t) ->
                                    log.error("maybeInitializeServer Result at {} networkId={}, result={}",
                                            key, identity.networkId(), r)));
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void applyPersisted(IdentityModel persisted) {
        synchronized (identityModel) {
            identityModel.applyPersisted(persisted);
        }
    }

    @Override
    public IdentityModel getClone() {
        synchronized (identityModel) {
            return IdentityModel.clone(identityModel);
        }
    }

    public void shutdown() {
    }

  /*  public Identity getOrCreateDefaultIdentity() {
        return getOrCreateIdentity(DEFAULT);
    }*/

    public CompletableFuture<Identity> getOrCreateIdentity(String domainId) {
        synchronized (identityModel) {
            if (identityModel.getIdentityByDomainId().containsKey(domainId)) {
                return CompletableFuture.completedFuture(identityModel.getIdentityByDomainId().get(domainId));
            }
        }
        String keyId = StringUtils.createUid();
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        String nodeId = StringUtils.createUid();
        return networkService.getInitializedNetworkIdAsync(nodeId, pubKey)
                .thenApply(networkId -> {
                    Identity identity = new Identity(domainId, networkId, keyPair);
                    synchronized (identityModel) {
                        identityModel.getIdentityByDomainId().put(domainId, identity);
                    }
                    persist();
                    return identity;
                });
        /*
        return networkService.findNetworkId(nodeId, pubKey)
                .map(networkId -> {
                    // NetworkId was already created in the past (but servers might not be started) 
                    synchronized (identityModel) {
                        if (identityModel.getIdentityByDomainId().containsKey(domainId)) {
                            return identityModel.getIdentityByDomainId().get(domainId);
                        } else {
                            Identity newIdentity = new Identity(domainId, networkId, keyPair);
                            identityModel.getIdentityByDomainId().put(domainId, newIdentity);
                            persist();
                            return newIdentity;
                        }
                    }
                })
                .map(CompletableFuture::completedFuture)
                .orElse(() -> {
                    // No NetworkId yet created
                    return networkService.getInitializedNetworkIdAsync(nodeId, pubKey)
                            .thenApply(networkId -> {
                                Identity identity = new Identity(domainId, nodeId, keyId);
                                synchronized (identityModel) {
                                    identityModel.getIdentityByDomainId().put(domainId, identity);
                                }
                                persist();
                                return identity;
                            });
                });*/


    }


    public Optional<Identity> findIdentityByNodeId(String nodeId) {
        synchronized (identityModel) {
            return identityModel.getIdentityByDomainId().values().stream()
                    .filter(identity -> identity.networkId().getNodeId().equals(nodeId))
                    .findAny();
        }
    }

    public Optional<Identity> findIdentityByDomainId(String domainId) {
        synchronized (identityModel) {
            return Optional.ofNullable(identityModel.getIdentityByDomainId().get(domainId));
        }
    }

/*
    public NodeIdAndPubKey getNodeIdAndPubKey(String domainId) {
        Identity identity = getOrCreateIdentity(domainId);
        String keyId = identity.keyId();
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        return new NodeIdAndPubKey(identity.nodeId(), pubKey);
    }

    public NodeIdAndKeyPair getNodeIdAndKeyPair(String domainId) {
        Identity identity = getOrCreateIdentity(domainId);
        String keyId = identity.keyId();
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        return new NodeIdAndKeyPair(identity.nodeId(), pubKey, keyPair);
    }

    public NodeIdAndKeyId getNodeIdAndKeyId(String domainId) {
        Identity identity = getOrCreateIdentity(domainId);
        return new NodeIdAndKeyId(identity.nodeId(), identity.keyId());
    }*/
}
