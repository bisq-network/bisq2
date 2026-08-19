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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.user.reputation;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.data.ByteArray;
import bisq.common.data.Pair;
import bisq.common.timer.Scheduler;
import bisq.common.util.MathUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Persists ownership-proof requests and periodically renews their authorized network data.
 */
@Getter
@Slf4j
public class SignedWitnessService extends SourceReputationService<AuthorizedSignedWitnessData>
        implements PersistenceClient<SignedWitnessStore> {
    public static final double WEIGHT = 10;
    public static final long MAX_DAYS_AGE_SCORE = 2000;
    public static final long MIN_DAYS_AGE_SCORE = 61;

    @Getter
    static class SignedWitnessDto {
        private final int protocolVersion;
        private final String profileId;
        private final String hashAsHex;
        private final String accountInputDataWithSaltBase64;
        private final String pubKeyBase64;
        private final String signatureBase64;

        public SignedWitnessDto(int protocolVersion,
                                String profileId,
                                String hashAsHex,
                                String accountInputDataWithSaltBase64,
                                String pubKeyBase64,
                                String signatureBase64) {
            this.protocolVersion = protocolVersion;
            this.profileId = profileId;
            this.hashAsHex = hashAsHex;
            this.accountInputDataWithSaltBase64 = accountInputDataWithSaltBase64;
            this.pubKeyBase64 = pubKeyBase64;
            this.signatureBase64 = signatureBase64;
        }
    }

    private final SignedWitnessStore persistableStore = new SignedWitnessStore();
    private final Persistence<SignedWitnessStore> persistence;
    private final Set<AuthorizedData> activeAuthorizations = new CopyOnWriteArraySet<>();
    private final WitnessReputationClaimRegistry claimRegistry;
    private final WitnessReputationClaimRegistry.Listener claimListener =
            affectedProfileIds -> affectedProfileIds.forEach(this::recalculateScore);

    public SignedWitnessService(PersistenceService persistenceService,
                                NetworkService networkService,
                                UserIdentityService userIdentityService,
                                UserProfileService userProfileService,
                                BannedUserService bannedUserService,
                                AuthorizedBondedRolesService authorizedBondedRolesService,
                                WitnessReputationClaimRegistry claimRegistry) {
        super(networkService, userIdentityService, userProfileService, bannedUserService, authorizedBondedRolesService);
        this.claimRegistry = claimRegistry;
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.SETTINGS, persistableStore);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        claimRegistry.addListener(claimListener);
        // We delay a bit to ensure the network is well established
        Scheduler.run(this::maybeRequestAgain)
                .host(this)
                .runnableName("maybeRequestAgain")
                .after(30, TimeUnit.SECONDS);
        return super.initialize();
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        claimRegistry.removeListener(claimListener);
        return super.shutdown();
    }

    public Set<String> getJsonRequests() {
        return persistableStore.getJsonRequests();
    }

    public boolean requestAuthorization(String json) {
        boolean sent = doRequestAuthorization(json);
        if (sent) {
            persistableStore.getJsonRequests().add(json);
            persist();
        }
        return sent;
    }

    @Override
    public synchronized void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof AuthorizedSignedWitnessData data) {
            boolean removed = activeAuthorizations.removeIf(existing ->
                    Arrays.equals(existing.getAuthorizedPublicKeyBytes(),
                            authorizedData.getAuthorizedPublicKeyBytes()) &&
                            existing.getAuthorizedDistributedData().equals(data));
            if (removed) {
                if (activeAuthorizations.stream()
                        .noneMatch(existing -> existing.getAuthorizedDistributedData().equals(data))) {
                    removeFromPendingDataSet(data);
                }
                claimRegistry.remove(authorizedData, data.getProfileId(), data.getWitnessNullifier());
                recalculateScore(data.getProfileId());
            }
        }
    }

    @Override
    public synchronized void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        if (!(authorizedData.getAuthorizedDistributedData() instanceof AuthorizedSignedWitnessData data) ||
                !data.isCurrentVersion() ||
                !isAuthorized(authorizedData) ||
                !activeAuthorizations.add(authorizedData)) {
            return;
        }
        claimRegistry.add(authorizedData, data.getProfileId(), data.getWitnessNullifier());
        super.onAuthorizedDataAdded(authorizedData);
    }

    @Override
    protected Optional<AuthorizedSignedWitnessData> findRelevantData(
            AuthorizedDistributedData authorizedDistributedData) {
        return authorizedDistributedData instanceof AuthorizedSignedWitnessData data && data.isCurrentVersion()
                ? Optional.of(data)
                : Optional.empty();
    }

    @Override
    protected void addToDataSet(Set<AuthorizedSignedWitnessData> dataSet, AuthorizedSignedWitnessData data) {
        dataSet.clear();
        dataSet.add(data);
    }

    @Override
    protected ByteArray getDataKey(AuthorizedSignedWitnessData data) {
        return new ByteArray(data.getProfileId().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected ByteArray getUserProfileKey(UserProfile userProfile) {
        return userProfile.getSignedWitnessKey();
    }

    @Override
    public long calculateScore(AuthorizedSignedWitnessData data) {
        return !data.isCurrentVersion() || claimRegistry.hasConflict(data.getWitnessNullifier())
                ? 0
                : doCalculateScore(WitnessReputationProtocol.getConservativeAgeInDays(data.getDateBucket()));
    }

    public static long doCalculateScore(long ageInDays) {
        checkArgument(ageInDays >= 0);
        if (ageInDays < MIN_DAYS_AGE_SCORE) {
            return 0;
        }
        long boundedAgeInDays = Math.min(MAX_DAYS_AGE_SCORE, ageInDays);
        return MathUtils.roundDoubleToLong(boundedAgeInDays * WEIGHT);
    }

    @Override
    protected void putScore(String userProfileId, Set<AuthorizedSignedWitnessData> ignored) {
        recalculateScore(userProfileId);
    }

    private boolean doRequestAuthorization(String json) {
        try {
            SignedWitnessDto dto = new Gson().fromJson(json, SignedWitnessDto.class);
            checkArgument(dto.getProtocolVersion() == AuthorizeSignedWitnessRequest.CURRENT_VERSION,
                    "Unsupported signed-witness authorization protocol version");
            return userIdentityService.findUserIdentity(dto.getProfileId())
                    .map(userIdentity -> send(userIdentity,
                            new AuthorizeSignedWitnessRequest(dto.getProfileId(),
                                    dto.getHashAsHex(),
                                    Base64.getDecoder().decode(dto.getAccountInputDataWithSaltBase64()),
                                    dto.getPubKeyBase64(),
                                    dto.getSignatureBase64())))
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error at requestAuthorization", e);
            return false;
        }
    }

    private void maybeRequestAgain() {
        long now = System.currentTimeMillis();
        if (now - persistableStore.getLastRequested() > AuthorizedSignedWitnessData.TTL / 2) {
            Set<String> jsonRequests = persistableStore.getJsonRequests();
            if (!jsonRequests.isEmpty()) {
                jsonRequests.forEach(this::doRequestAuthorization);
                persistableStore.setLastRequested(now);
                persist();
            }
        }
    }

    private void recalculateScore(String userProfileId) {
        Optional<AuthorizedSignedWitnessData> bestData = activeAuthorizations.stream()
                .map(AuthorizedData::getAuthorizedDistributedData)
                .map(AuthorizedSignedWitnessData.class::cast)
                .filter(data -> data.getProfileId().equals(userProfileId))
                .filter(data -> !claimRegistry.hasConflict(data.getWitnessNullifier()))
                .min(java.util.Comparator.comparingLong(AuthorizedSignedWitnessData::getDateBucket));
        ByteArray userProfileKey = new ByteArray(userProfileId.getBytes(StandardCharsets.UTF_8));

        if (bestData.isEmpty()) {
            dataSetByHash.remove(userProfileKey);
            scoreByUserProfileId.remove(userProfileId);
            userProfileIdScorePair.set(new Pair<>(userProfileId, 0L));
            return;
        }

        // Keep authorization data pending until its profile has been observed, but continue to update a score that
        // was already associated with a profile which is temporarily absent from the shorter-lived profile store.
        if (userProfileService.findUserProfile(userProfileId).isEmpty() &&
                !dataSetByHash.containsKey(userProfileKey)) {
            return;
        }

        Set<AuthorizedSignedWitnessData> dataSet = new CopyOnWriteArraySet<>();
        dataSet.add(bestData.get());
        dataSetByHash.put(userProfileKey, dataSet);
        long score = doCalculateScore(
                WitnessReputationProtocol.getConservativeAgeInDays(bestData.get().getDateBucket()));
        scoreByUserProfileId.put(userProfileId, score);
        userProfileIdScorePair.set(new Pair<>(userProfileId, score));
    }
}
