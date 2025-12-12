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


import bisq.common.application.Service;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.timer.Scheduler;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkIdService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.Node;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleService;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class IdentityService extends RateLimitedPersistenceClient<IdentityStore> implements Service {
    public static final String DEFAULT_IDENTITY_TAG = "default";

    @Getter
    private final IdentityStore persistableStore = new IdentityStore();
    @Getter
    private final Persistence<IdentityStore> persistence;
    private final KeyBundleService keyBundleService;
    private final NetworkService networkService;
    private final Object lock = new Object();
    private final NetworkIdService networkIdService;
    @Getter
    private final Observable<RuntimeException> fatalException = new Observable<>();

    public IdentityService(PersistenceService persistenceService,
                           KeyBundleService keyBundleService,
                           NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.keyBundleService = keyBundleService;
        this.networkService = networkService;
        networkIdService = networkService.getNetworkIdService();
    }

    @Override
    public IdentityStore preProcessPersisted(IdentityStore persisted) {
        // We store the key bundle also in identity. To ensure it is in sync with the key bundle from
        // key bundle service, we check for equality and if not matching we replace the key bundle.
        var optionalDefaultIdentity = persisted.getDefaultIdentity()
                .map(this::maybeUpdateKeyBundle);

        var activeIdentityByTag = persisted.getActiveIdentityByTag().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> maybeUpdateKeyBundle(e.getValue())
                ));

        var retired = persisted.getRetired().stream()
                .map(this::maybeUpdateKeyBundle)
                .collect(Collectors.toSet());

        return new IdentityStore(optionalDefaultIdentity, Map.copyOf(activeIdentityByTag), Set.copyOf(retired));
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    /**
     * Creates and initialized the default identity. This includes initialisation of the associated network node
     * on at least one transport.
     */
    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        // Create default identity
        getOrCreateDefaultIdentity();

        if (getActiveIdentityByTag().isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }

        List<Identity> snapshot;
        synchronized (lock) {
            snapshot = List.copyOf(getActiveIdentityByTag().values());
        }
        snapshot.forEach(this::maybeUpdateNetworkId);

        // We get called after networkService with the default node is already initialized.
        // We publish now all onion services of our active identities.
        // We do not wait for them to be completed as with many identities and in case tor is slow that could
        // trigger startup timeouts.
        networkService.getSupportedTransportTypes().forEach(this::initializeAllActiveIdentities);

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        fatalException.set(null);
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public Identity getOrCreateDefaultIdentity() {
        Optional<Identity> defaultIdentity = persistableStore.getDefaultIdentity();
        defaultIdentity.ifPresent(this::maybeUpdateNetworkId);
        return defaultIdentity
                .orElseGet(() -> {
                    Identity identity = createIdentity(DEFAULT_IDENTITY_TAG);
                    synchronized (lock) {
                        persistableStore.setDefaultIdentity(identity);
                    }
                    persist();
                    return identity;
                });
    }

    /**
     * Creates new identity based on given parameters.
     */
    public CompletableFuture<Identity> createNewActiveIdentity(String identityTag, KeyPair keyPair) {
        KeyBundle keyBundle = keyBundleService.createAndPersistKeyBundle(identityTag, keyPair);
        NetworkId networkId = networkIdService.getOrCreateNetworkId(keyBundle, identityTag);
        Identity identity = new Identity(identityTag, networkId, keyBundle);

        synchronized (lock) {
            getActiveIdentityByTag().put(identityTag, identity);
        }
        persist();
        // We return the identity if at least one transport node got initialized
        return networkService.anySuppliedInitializedNode(networkId)
                .thenApply(nodes -> identity);
    }

    public boolean retireActiveIdentity(String identityTag) {
        boolean wasRemoved;
        synchronized (lock) {
            Identity identity = getActiveIdentityByTag().remove(identityTag);
            wasRemoved = identity != null;
            if (wasRemoved) {
                persistableStore.getRetired().add(identity);
            }
        }
        if (wasRemoved) {
            persist();
        }
        return wasRemoved;
    }

    public Optional<Identity> findActiveIdentity(String identityTag) {
        synchronized (lock) {
            return Optional.ofNullable(getActiveIdentityByTag().get(identityTag));
        }
    }

    public Optional<Identity> findActiveIdentity(NetworkId networkId) {
        synchronized (lock) {
            return getActiveIdentityByTag().values().stream()
                    .filter(e -> e.getNetworkId().equals(networkId))
                    .findAny();
        }
    }

    public Optional<Identity> findRetiredIdentityByNetworkId(NetworkId networkId) {
        synchronized (lock) {
            return getRetired().stream()
                    .filter(e -> e.getNetworkId().equals(networkId))
                    .findAny();
        }
    }

    public Optional<Identity> findAnyIdentityByNetworkId(NetworkId networkId) {
        synchronized (lock) {
            return Stream.concat(Stream.concat(persistableStore.getDefaultIdentity().stream(),
                                    getActiveIdentityByTag().values().stream()),
                            getRetired().stream())
                    .filter(e -> e.getNetworkId().equals(networkId))
                    .findAny();
        }
    }

    public Map<String, Identity> getActiveIdentityByTag() {
        return persistableStore.getActiveIdentityByTag();
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private CompletableFuture<List<Node>> initializeAllActiveIdentities(TransportType transportType) {
        List<Identity> snapshot;
        synchronized (lock) {
            snapshot = getActiveIdentityByTag().values().stream()
                    .filter(identity -> !identity.getTag().equals(IdentityService.DEFAULT_IDENTITY_TAG))
                    .collect(Collectors.toList());
        }
        Stream<CompletableFuture<Node>> futures = snapshot.stream()
                .map(identity -> networkService.supplyInitializedNode(transportType, identity.getNetworkId()));

        return CompletableFutureUtils.allOf(futures);
    }

    private CompletableFuture<Identity> createAndInitializeNewActiveIdentity(String identityTag, Identity identity) {
        synchronized (lock) {
            getActiveIdentityByTag().put(identityTag, identity);
        }
        persist();

        return networkService.anySuppliedInitializedNode(identity.getNetworkId())
                .thenApply(node -> identity);
    }

    private Identity createIdentity(String identityTag) {
        String keyId = keyBundleService.getKeyIdFromTag(identityTag);
        KeyBundle keyBundle = keyBundleService.getOrCreateKeyBundle(keyId);
        NetworkId networkId = networkIdService.getOrCreateNetworkId(keyBundle, identityTag);
        return new Identity(identityTag, networkId, keyBundle);
    }

    private Set<Identity> getRetired() {
        return persistableStore.getRetired();
    }


    @VisibleForTesting
    CompletableFuture<Identity> createAndInitializeNewActiveIdentity(String identityTag) {
        return createAndInitializeNewActiveIdentity(identityTag, createIdentity(identityTag));
    }


    @VisibleForTesting
    Identity getOrCreateIdentity(String identityTag) {
        return findActiveIdentity(identityTag)
                .orElseGet(() -> createAndInitializeNewActiveIdentity(identityTag).join());
    }

    private void maybeUpdateNetworkId(Identity identity) {
        String identityTag = identity.getTag();
        // Update networkId if we got I2P added
        networkIdService.maybeUpdateNetworkId(identity.getKeyBundle(), identityTag);

        networkIdService.findNetworkId(identityTag)
                .ifPresent(fromNetworkIdStore -> {
                    NetworkId fromIdentityStore = identity.getNetworkId();
                    if (fromNetworkIdStore.equals(fromIdentityStore)) {
                        return;
                    }

                    AddressByTransportTypeMap mapFromIdentityStore = fromIdentityStore.getAddressByTransportTypeMap();
                    AddressByTransportTypeMap mapFromNetworkStore = fromNetworkIdStore.getAddressByTransportTypeMap();
                    Set<TransportType> missing = mapFromNetworkStore.keySet().stream()
                            .filter(transportType ->
                                    mapFromNetworkStore.getAddress(transportType).isPresent() &&
                                            (mapFromIdentityStore.getAddress(transportType).isEmpty() ||
                                                    !mapFromNetworkStore.getAddress(transportType).get().equals(mapFromIdentityStore.getAddress(transportType).get()))
                            )
                            .collect(Collectors.toSet());
                    if (missing.isEmpty()) {
                        // mapFromIdentityStore contains different entries or mapFromNetworkStore miss entries
                        String errorMessage = "Data inconsistency detected.\n" +
                                "The inconsistent data got restored and requires a restart of the application.\n" +
                                "Details:\n" +
                                "The persisted networkId from our identity store does not match the persisted networkId from the networkIdStore.\n" +
                                "The issue could have happened if the cache directory was deleted in versions before 2.1.0. " +
                                "In that case the networkId from networkIdService got assigned a new random port.\n" +
                                "Data:\n" +
                                "FromIdentityStore=" + fromIdentityStore + "\nFromNetworkIdStore=" + fromNetworkIdStore;
                        log.error(errorMessage);
                        networkIdService.recoverInvalidNetworkIds(fromIdentityStore, identityTag);
                        fatalException.set(new RuntimeException(errorMessage));
                    } else {
                        // Expected in case we have got added I2P to an identity which was created with Tor only
                        KeyBundle keyBundle = identity.getKeyBundle();
                        Identity upDatedIdentity = new Identity(identityTag, fromNetworkIdStore, keyBundle);
                        log.warn("We update the identity for tag {} with the new networkId from network ID store. {}\n" +
                                "This is expected when user update to the I2P enabled version", identityTag, fromNetworkIdStore);
                        synchronized (lock) {
                            if (identityTag.equals(DEFAULT_IDENTITY_TAG)) {
                                persistableStore.setDefaultIdentity(upDatedIdentity);
                            } else {
                                persistableStore.getActiveIdentityByTag().put(identityTag, upDatedIdentity);
                            }
                        }
                        persist();
                    }
                });
    }

    private Identity maybeUpdateKeyBundle(Identity identity) {
        var optionalKeyBundle = keyBundleService.findKeyBundle(identity.getKeyBundle().getKeyId());
        checkArgument(optionalKeyBundle.isPresent(), "keyBundle from keyBundleService must be present. IdentityTag=" + identity.getTag());
        var keyBundle = optionalKeyBundle.get();
        if (!identity.getKeyBundle().equals(keyBundle)) {
            log.warn("keyBundle from identity is not matching the one from keyBundleService. " +
                    "Updating Identity with keyBundle from keyBundleService. IdentityTag={}", identity.getTag());
            Scheduler.run(this::persist).after(2000);
            return new Identity(identity.getTag(), identity.getNetworkId(), keyBundle);
        }
        return identity;
    }
}