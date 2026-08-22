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
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * We persist our json request data and do the authorisation request again at each start if the age of the last request
 * exceeds the half of the TTL of the AuthorizedAccountAgeData. That way the network does not keep inactive data for
 * too long.
 */
@Getter
@Slf4j
public class AccountAgeService extends SourceReputationService<AuthorizedAccountAgeData> implements PersistenceClient<AccountAgeStore>, AuthorizedBondedRolesService.Listener {
    public static final long WEIGHT = 4;
    public static final long MAX_DAYS_AGE_SCORE = 2000;

    // Has to be in sync with Bisq1 class
    @Getter
    static class AccountAgeWitnessDto {
        private final int protocolVersion;
        private final String profileId;
        private final String hashAsHex;
        private final String accountInputDataWithSaltBase64;
        private final String pubKeyBase64;
        private final String signatureBase64;

        public AccountAgeWitnessDto(int protocolVersion,
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

    @Getter
    private final AccountAgeStore persistableStore = new AccountAgeStore();
    @Getter
    private final Persistence<AccountAgeStore> persistence;
    private final Set<AuthorizedData> activeAuthorizations = new CopyOnWriteArraySet<>();
    private final WitnessReputationClaimRegistry claimRegistry;
    private final WitnessReputationClaimRegistry.Listener claimListener =
            affectedProfileIds -> affectedProfileIds.forEach(this::recalculateScore);

    public AccountAgeService(PersistenceService persistenceService,
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
                .after(3, TimeUnit.SECONDS);
        return super.initialize();
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        claimRegistry.removeListener(claimListener);
        return super.shutdown();
    }

    @Override
    public synchronized void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof AuthorizedAccountAgeData data) {
            boolean removed = activeAuthorizations.removeIf(existing ->
                    Arrays.equals(existing.getAuthorizedPublicKeyBytes(),
                            authorizedData.getAuthorizedPublicKeyBytes()) &&
                            existing.getAuthorizedDistributedData().equals(data));
            if (!removed) {
                return;
            }
            if (activeAuthorizations.stream()
                    .noneMatch(existing -> existing.getAuthorizedDistributedData().equals(data))) {
                removeFromPendingDataSet(data);
            }
            claimRegistry.remove(authorizedData, data.getProfileId(), data.getWitnessNullifier());
            recalculateScore(data.getProfileId());
        }
    }

    @Override
    public synchronized void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        if (!(authorizedData.getAuthorizedDistributedData() instanceof AuthorizedAccountAgeData data) ||
                !data.isCurrentVersion() ||
                !isAuthorized(authorizedData) ||
                !activeAuthorizations.add(authorizedData)) {
            return;
        }
        claimRegistry.add(authorizedData, data.getProfileId(), data.getWitnessNullifier());
        super.onAuthorizedDataAdded(authorizedData);
    }

    @Override
    protected Optional<AuthorizedAccountAgeData> findRelevantData(AuthorizedDistributedData authorizedDistributedData) {
        return authorizedDistributedData instanceof AuthorizedAccountAgeData data && data.isCurrentVersion() ?
                Optional.of(data) :
                Optional.empty();
    }

    @Override
    protected void addToDataSet(Set<AuthorizedAccountAgeData> dataSet, AuthorizedAccountAgeData data) {
        dataSet.clear();
        dataSet.add(data);
    }

    @Override
    protected ByteArray getDataKey(AuthorizedAccountAgeData data) {
        return new ByteArray(data.getProfileId().getBytes(StandardCharsets.UTF_8));
    }


    @Override
    protected ByteArray getUserProfileKey(UserProfile userProfile) {
        return userProfile.getAccountAgeKey();
    }

    @Override
    public long calculateScore(AuthorizedAccountAgeData data) {
        return !data.isCurrentVersion() || claimRegistry.hasConflict(data.getWitnessNullifier())
                ? 0
                : doCalculateScore(WitnessReputationProtocol.getConservativeAgeInDays(data.getDateBucket()));
    }

    public static long doCalculateScore(long ageInDays) {
        checkArgument(ageInDays >= 0);
        long boundedAgeInDays = Math.min(MAX_DAYS_AGE_SCORE, ageInDays);
        return MathUtils.roundDoubleToLong(boundedAgeInDays * WEIGHT);
    }

    public boolean requestAuthorization(String json) {
        boolean sent = doRequestAuthorization(json);
        if (sent) {
            persistableStore.getJsonRequests().add(json);
            persist();
        }
        return sent;
    }

    private boolean doRequestAuthorization(String json) {
        try {
            AccountAgeWitnessDto dto = new Gson().fromJson(json, AccountAgeWitnessDto.class);
            String profileId = dto.getProfileId();
            checkArgument(dto.getProtocolVersion() == AuthorizeAccountAgeRequest.CURRENT_VERSION,
                    "Unsupported account age authorization protocol version");
            return userIdentityService.findUserIdentity(profileId).map(userIdentity -> {
                        AuthorizeAccountAgeRequest request = new AuthorizeAccountAgeRequest(profileId,
                                dto.getHashAsHex(),
                                java.util.Base64.getDecoder().decode(dto.getAccountInputDataWithSaltBase64()),
                                dto.getPubKeyBase64(),
                                dto.getSignatureBase64());
                        return send(userIdentity, request);
                    })
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error at requestAuthorization", e);
            return false;
        }
    }

    private void maybeRequestAgain() {
        long now = System.currentTimeMillis();
        if (now - persistableStore.getLastRequested() > AuthorizedAccountAgeData.TTL / 2) {
            Set<String> jsonRequests = persistableStore.getJsonRequests();
            if (!jsonRequests.isEmpty()) {
                jsonRequests.forEach(this::doRequestAuthorization);
                persistableStore.setLastRequested(now);
                persist();
            }
        }
    }

    @Override
    protected void putScore(String userProfileId, Set<AuthorizedAccountAgeData> ignored) {
        recalculateScore(userProfileId);
    }

    private void recalculateScore(String userProfileId) {
        Optional<AuthorizedAccountAgeData> bestData = activeAuthorizations.stream()
                .map(AuthorizedData::getAuthorizedDistributedData)
                .map(AuthorizedAccountAgeData.class::cast)
                .filter(data -> data.getProfileId().equals(userProfileId))
                .filter(data -> !claimRegistry.hasConflict(data.getWitnessNullifier()))
                .min(java.util.Comparator.comparingLong(AuthorizedAccountAgeData::getDateBucket));
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

        Set<AuthorizedAccountAgeData> dataSet = new java.util.concurrent.CopyOnWriteArraySet<>();
        dataSet.add(bestData.get());
        dataSetByHash.put(userProfileKey, dataSet);
        long score = doCalculateScore(
                WitnessReputationProtocol.getConservativeAgeInDays(bestData.get().getDateBucket()));
        scoreByUserProfileId.put(userProfileId, score);
        userProfileIdScorePair.set(new Pair<>(userProfileId, score));
    }
}
