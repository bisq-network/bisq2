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

package bisq.user.reputation;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.common.data.ByteArray;
import bisq.common.data.Pair;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.map.HashMapObserver;
import bisq.network.NetworkService;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;


@Slf4j
public abstract class SourceReputationService<T extends AuthorizedDistributedData> implements Service, AuthorizedBondedRolesService.Listener {
    protected static final long DAY_AS_MS = TimeUnit.DAYS.toMillis(1);
    public static final int MAX_AGE_BOOST_DAYS = 365;
    public static final double MAX_AGE_BOOST_PERIOD = TimeUnit.DAYS.toMillis(MAX_AGE_BOOST_DAYS);

    public static long getAgeInDays(long date) {
        return (System.currentTimeMillis() - date) / DAY_AS_MS;
    }

    protected final NetworkService networkService;
    protected final UserIdentityService userIdentityService;
    protected final UserProfileService userProfileService;
    private final BannedUserService bannedUserService;
    protected final AuthorizedBondedRolesService authorizedBondedRolesService;
    @Getter
    protected final Map<ByteArray, Set<T>> dataSetByHash = new ConcurrentHashMap<>();
    @Getter
    protected final Map<String, Long> scoreByUserProfileId = new ConcurrentHashMap<>();
    @Getter
    protected final Observable<Pair<String, Long>> userProfileIdScorePair = new Observable<>();
    private final Map<ByteArray, Set<T>> pendingDataSetByHash = new ConcurrentHashMap<>();
    // The user profiles by the key of this source: the profiles which the profile store holds, and the profiles which
    // have a data set. A score stays while its data exists, also when the shorter-lived profile store does not hold the
    // profile, and removing the score with the data needs the profile ID.
    private final Map<ByteArray, UserProfile> userProfileByUserProfileKey = new ConcurrentHashMap<>();
    // Guarded by this. The stored publications of the relevant data, by data key, for the sources which use
    // onAuthorizedDataAdded and onAuthorizedDataRemoved of this class. Equal data can have several publications, for
    // example one of each oracle node. Data stays in the derived state while one of its publications is stored.
    private final Map<ByteArray, Set<Publication<T>>> publicationsByDataKey = new HashMap<>();
    private Pin userProfileByIdPin;

    private record Publication<D>(ByteArray authorizedPublicKey, D data) {
    }


    public SourceReputationService(NetworkService networkService,
                                   UserIdentityService userIdentityService,
                                   UserProfileService userProfileService,
                                   BannedUserService bannedUserService,
                                   AuthorizedBondedRolesService authorizedBondedRolesService) {
        this.networkService = networkService;
        this.userIdentityService = userIdentityService;
        this.userProfileService = userProfileService;
        this.bannedUserService = bannedUserService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        authorizedBondedRolesService.addListener(this);
        // In case we have received a AuthorizedDistributedData without the matching user profile because the
        // user profile was not retrieved yet from the network, we reprocess the AuthorizedDistributedData.
        userProfileByIdPin = userProfileService.getUserProfileById().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String key, UserProfile userProfile) {
                handleAddedUserProfile(userProfile);
            }

            @Override
            public void putAll(Map<? extends String, ? extends UserProfile> map) {
                // Clone to unmodifiable Map to avoid ConcurrentModificationException
                Map.copyOf(map).values().forEach(value -> handleAddedUserProfile(value));
            }

            @Override
            public void remove(Object key) {
                if (key instanceof String userProfileId) {
                    handleRemovedUserProfile(userProfileId);
                }
            }

            @Override
            public void clear() {
                handleClearedUserProfiles();
            }
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (userProfileByIdPin != null) {
            userProfileByIdPin.unbind();
            userProfileByIdPin = null;
        }
        authorizedBondedRolesService.removeListener(this);
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // AuthorizedBondedRolesService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public synchronized void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        findRelevantData(authorizedData.getAuthorizedDistributedData())
                .ifPresent(data -> {
                    if (isAuthorized(authorizedData) && isDataValid(data)) {
                        addPublication(authorizedData, data);
                        handleAddedAuthorizedDistributedData(data);
                    }
                });
    }

    // The store removed a publication, because its owner removed it or because it expired. Only a publication which
    // onAuthorizedDataAdded added counts, so a publication of an unauthorized key does not remove equal data of an
    // oracle node.
    @Override
    public synchronized void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        findRelevantData(authorizedData.getAuthorizedDistributedData())
                .ifPresent(data -> {
                    if (removePublication(authorizedData, data) && !hasPublication(data)) {
                        handleRemovedAuthorizedDistributedData(data);
                    }
                });
    }

    protected boolean isDataValid(T data) {
        return true;
    }

    protected abstract Optional<T> findRelevantData(AuthorizedDistributedData authorizedDistributedData);

    // Some services don't support multiple entries and will override that method
    protected void addToDataSet(Set<T> dataSet, T data) {
        dataSet.add(data);
    }

    protected abstract ByteArray getDataKey(T data);

    protected abstract ByteArray getUserProfileKey(UserProfile userProfile);

    // The same key as getUserProfileKey(UserProfile) of the profile with this ID
    protected abstract ByteArray getUserProfileKey(String userProfileId);

    protected void removeFromPendingDataSet(T data) {
        ByteArray dataKey = getDataKey(data);
        pendingDataSetByHash.computeIfPresent(dataKey, (ignored, dataSet) -> {
            dataSet.remove(data);
            return dataSet.isEmpty() ? null : dataSet;
        });
    }

    protected void putScore(String userProfileId, Set<T> dataSet) {
        long score = dataSet.stream().mapToLong(this::calculateScore).sum();
        scoreByUserProfileId.put(userProfileId, score);
        notifyScoreChanged(userProfileId, score);
    }

    // Removes the data set and the score of a profile for which this source has no data anymore
    protected void removeDataSetAndScore(ByteArray userProfileKey, String userProfileId) {
        dataSetByHash.remove(userProfileKey);
        if (userProfileService.findUserProfile(userProfileId).isEmpty()) {
            userProfileByUserProfileKey.remove(userProfileKey);
        }
        scoreByUserProfileId.remove(userProfileId);
        notifyScoreChanged(userProfileId, 0);
    }

    // Observable.set notifies the observers only of a new value. They must also get a pair which equals the last one,
    // for example when a score of 0 is removed right after it was set. The observers ignore null.
    protected void notifyScoreChanged(String userProfileId, long score) {
        userProfileIdScorePair.set(null);
        userProfileIdScorePair.set(new Pair<>(userProfileId, score));
    }

    protected boolean send(UserIdentity userIdentity, EnvelopePayloadMessage request) {
        checkArgument(!bannedUserService.isUserProfileBanned(userIdentity.getUserProfile()));
        if (authorizedBondedRolesService.getAuthorizedOracleNodes().isEmpty()) {
            return false;
        }
        authorizedBondedRolesService.getAuthorizedOracleNodes().forEach(oracleNode ->
                networkService.confidentialSend(request, oracleNode.getNetworkId(), userIdentity.getNetworkIdWithKeyPair()));
        return true;
    }

    public long getScore(String userProfileId) {
        return scoreByUserProfileId.containsKey(userProfileId) ? scoreByUserProfileId.get(userProfileId) : 0;
    }

    // Whether this source has data, and thus a score, for the profile. The score can be 0.
    public boolean hasScore(String userProfileId) {
        return scoreByUserProfileId.containsKey(userProfileId);
    }

    public abstract long calculateScore(T data);

    protected boolean isAuthorized(AuthorizedData authorizedData) {
        return authorizedBondedRolesService.hasAuthorizedPubKey(authorizedData, BondedRoleType.ORACLE_NODE);
    }

    protected static double getAgeBoostFactor(long eventTime) {
        checkArgument(eventTime >= 0);
        long age = Math.max(0, System.currentTimeMillis() - eventTime);
        double ageFactor = Math.min(1, age / MAX_AGE_BOOST_PERIOD);
        return 1 + ageFactor;
    }

    private synchronized void handleAddedUserProfile(UserProfile userProfile) {
        ByteArray userProfileKey = getUserProfileKey(userProfile);
        userProfileByUserProfileKey.putIfAbsent(userProfileKey, userProfile);
        Set<T> dataSet = pendingDataSetByHash.get(userProfileKey);
        if (dataSet != null) {
            // Clone to avoid ConcurrentModificationException
            Set<T> clone = new HashSet<>(dataSet);
            clone.forEach(data -> {
                // We could get multiple AuthorizedDistributedData for one user profile
               /* log.debug("We received a user profile with username {} which had a pending " +
                                "AuthorizedDistributedData with dataKey {} and re-process the AuthorizedDistributedData",
                        userProfile.getUserName(), getDataKey(data));*/
                handleAddedAuthorizedDistributedData(data);
                dataSet.remove(data);
                if (dataSet.isEmpty()) {
                    pendingDataSetByHash.remove(userProfileKey);
                }
            });
        }
    }

    private synchronized void handleRemovedUserProfile(String userProfileId) {
        ByteArray userProfileKey = getUserProfileKey(userProfileId);
        if (!dataSetByHash.containsKey(userProfileKey)) {
            userProfileByUserProfileKey.remove(userProfileKey);
        }
    }

    private synchronized void handleClearedUserProfiles() {
        userProfileByUserProfileKey.keySet().removeIf(userProfileKey -> !dataSetByHash.containsKey(userProfileKey));
    }

    private void addPublication(AuthorizedData authorizedData, T data) {
        publicationsByDataKey.computeIfAbsent(getDataKey(data), dataKey -> new HashSet<>())
                .add(new Publication<>(new ByteArray(authorizedData.getAuthorizedPublicKeyBytes()), data));
    }

    // Whether the publication was added before
    private boolean removePublication(AuthorizedData authorizedData, T data) {
        ByteArray dataKey = getDataKey(data);
        Set<Publication<T>> publications = publicationsByDataKey.get(dataKey);
        if (publications == null ||
                !publications.remove(new Publication<>(new ByteArray(authorizedData.getAuthorizedPublicKeyBytes()), data))) {
            return false;
        }
        if (publications.isEmpty()) {
            publicationsByDataKey.remove(dataKey);
        }
        return true;
    }

    private boolean hasPublication(T data) {
        return getPublishedData(getDataKey(data)).anyMatch(data::equals);
    }

    private Stream<T> getPublishedData(ByteArray dataKey) {
        return publicationsByDataKey.getOrDefault(dataKey, Set.of()).stream()
                .map(Publication::data)
                .distinct();
    }

    // The data has no stored publication anymore
    private void handleRemovedAuthorizedDistributedData(T data) {
        removeFromPendingDataSet(data);
        ByteArray dataKey = getDataKey(data);
        if (!dataSetByHash.containsKey(dataKey)) {
            return;
        }
        // addToDataSet can have dropped other data because of the removed data, for example a newer profile age
        // because of an older one. So we build the data set again from all data of the key which is still published,
        // as a node which starts with this data would. While a key has a data set, its profile is known and none of
        // its data is pending.
        Set<T> dataSet = new CopyOnWriteArraySet<>();
        getPublishedData(dataKey).forEach(publishedData -> addToDataSet(dataSet, publishedData));
        String userProfileId = userProfileByUserProfileKey.get(dataKey).getId();
        if (dataSet.isEmpty()) {
            removeDataSetAndScore(dataKey, userProfileId);
        } else {
            dataSetByHash.put(dataKey, dataSet);
            putScore(userProfileId, dataSet);
        }
    }

    // AccountAgeService and SignedWitnessService keep their own state of the publications, and call this method
    // instead of onAuthorizedDataAdded of this class.
    protected void handleAddedAuthorizedDistributedData(T data) {
        ByteArray providedHash = getDataKey(data);
        Optional.ofNullable(userProfileByUserProfileKey.get(providedHash))
                .ifPresentOrElse(userProfile -> {
                    ByteArray hash = getUserProfileKey(userProfile);
                    if (!dataSetByHash.containsKey(hash)) {
                        dataSetByHash.put(hash, new CopyOnWriteArraySet<>());
                    }
                    Set<T> dataSet = dataSetByHash.get(hash);
                    addToDataSet(dataSet, data);
                    putScore(userProfile.getId(), dataSet);
                }, () -> {
                    // It is expected to receive data which do not have a matching user profile at startup in case
                    // the AuthorizedDistributedData is delivered before the user profile.
                    // As the TTL of the reputation AuthorizedDistributedData is 100 days, but the user profile only
                    // 15 days, that is another reason why we will get AuthorizedDistributedData without matching
                    // user profile.
                    log.debug("Could not find a user profile matching the data key {} from the AuthorizedDistributedData. " +
                                    "We add it to the pendingAuthorizedDistributedDataSet for re-processing once a new user profile gets added.",
                            providedHash);
                    pendingDataSetByHash.putIfAbsent(providedHash, new CopyOnWriteArraySet<>());
                    Set<T> dataSet = pendingDataSetByHash.get(providedHash);
                    dataSet.add(data);
                });
    }
}
