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

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

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
    private final Map<ByteArray, UserProfile> userProfileByUserProfileKey = new ConcurrentHashMap<>();
    private Pin userProfileByIdPin;


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
            }

            @Override
            public void clear() {
            }
        });
        return CompletableFuture.completedFuture(true);
    }

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
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        findRelevantData(authorizedData.getAuthorizedDistributedData())
                .ifPresent(data -> {
                    if (isAuthorized(authorizedData) && isDataValid(data)) {
                        handleAddedAuthorizedDistributedData(data);
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

    protected void putScore(String userProfileId, Set<T> dataSet) {
        long score = dataSet.stream().mapToLong(this::calculateScore).sum();
        scoreByUserProfileId.put(userProfileId, score);
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

    private void handleAddedUserProfile(UserProfile userProfile) {
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

    private void handleAddedAuthorizedDistributedData(T data) {
        ByteArray providedHash = getDataKey(data);
        Optional.ofNullable(userProfileByUserProfileKey.get(providedHash))
                .ifPresentOrElse(userProfile -> {
                    ByteArray hash = getUserProfileKey(userProfile);
                    if (!dataSetByHash.containsKey(hash)) {
                        dataSetByHash.put(hash, new HashSet<>());
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