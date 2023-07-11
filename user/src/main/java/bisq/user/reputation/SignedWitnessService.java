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

import bisq.bonded_roles.AuthorizedBondedRolesService;
import bisq.common.data.ByteArray;
import bisq.common.timer.Scheduler;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * We persist our json request data and do the authorisation request again at each start if the age of the last request
 * exceeds the half of the TTL of the AuthorizedSignedWitnessData. That way the network does not keep inactive data for
 * too long.
 */
@Getter
@Slf4j
public class SignedWitnessService extends SourceReputationService<AuthorizedSignedWitnessData> implements PersistenceClient<SignedWitnessStore> {
    private static final long DAY_MS = TimeUnit.DAYS.toMillis(1);
    public static final long WEIGHT = 50;

    // Has to be in sync with Bisq1 class
    @Getter
    static class SignedWitnessDto {
        private final String profileId;
        private final String hashAsHex;
        private final long accountAgeWitnessDate;
        private final long witnessSignDate;
        private final String pubKeyBase64;
        private final String signatureBase64;

        public SignedWitnessDto(String profileId,
                                String hashAsHex,
                                long accountAgeWitnessDate,
                                long witnessSignDate,
                                String pubKeyBase64,
                                String signatureBase64) {
            this.profileId = profileId;
            this.hashAsHex = hashAsHex;
            this.accountAgeWitnessDate = accountAgeWitnessDate;
            this.witnessSignDate = witnessSignDate;
            this.pubKeyBase64 = pubKeyBase64;
            this.signatureBase64 = signatureBase64;
        }
    }

    @Getter
    private final SignedWitnessStore persistableStore = new SignedWitnessStore();
    @Getter
    private final Persistence<SignedWitnessStore> persistence;

    public SignedWitnessService(PersistenceService persistenceService,
                                NetworkService networkService,
                                UserIdentityService userIdentityService,
                                UserProfileService userProfileService,
                                AuthorizedBondedRolesService authorizedBondedRolesService) {
        super(networkService, userIdentityService, userProfileService, authorizedBondedRolesService);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        // We delay a bit to ensure the network is well established
        Scheduler.run(this::maybeRequestAgain).after(30, TimeUnit.SECONDS);
        return super.initialize();
    }

    public Set<String> getJsonRequests() {
        return persistableStore.getJsonRequests();
    }

    public boolean requestAuthorization(String json) {
        persistableStore.getJsonRequests().add(json);
        persist();
        return doRequestAuthorization(json);
    }

    @Override
    protected Optional<AuthorizedSignedWitnessData> findRelevantData(AuthorizedDistributedData authorizedDistributedData) {
        return authorizedDistributedData instanceof AuthorizedSignedWitnessData ?
                Optional.of((AuthorizedSignedWitnessData) authorizedDistributedData) :
                Optional.empty();
    }

    @Override
    protected void addToDataSet(Set<AuthorizedSignedWitnessData> dataSet, AuthorizedSignedWitnessData data) {
        if (dataSet.isEmpty()) {
            dataSet.add(data);
            return;
        }

        // If new data is older than existing entry we clear set and add our new data, otherwise we ignore the new data.
        AuthorizedSignedWitnessData existing = new ArrayList<>(dataSet).get(0);
        if (existing.getWitnessSignDate() > data.getWitnessSignDate()) {
            dataSet.clear();
            dataSet.add(data);
        }
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
        long age = getAgeInDays(data.getWitnessSignDate());
        if (age <= 60) {
            return 0;
        }
        return Math.min(365, age) * WEIGHT;
    }

    private boolean doRequestAuthorization(String json) {
        getJsonRequests().add(json);
        persist();
        try {
            SignedWitnessDto dto = new Gson().fromJson(json, SignedWitnessDto.class);
            long witnessSignDate = dto.getWitnessSignDate();
            long age = getAgeInDays(witnessSignDate);
            if (age < 61) {
                log.error("witnessSignDate has to be at least 61 days. witnessSignDate={}", witnessSignDate);
                return false;
            }
            String profileId = dto.getProfileId();
            return userIdentityService.findUserIdentity(profileId).map(userIdentity -> {
                        AuthorizeSignedWitnessRequest request = new AuthorizeSignedWitnessRequest(profileId,
                                dto.getHashAsHex(),
                                dto.getAccountAgeWitnessDate(),
                                witnessSignDate,
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
        if (now - persistableStore.getLastRequested() > AuthorizedSignedWitnessData.TTL / 2) {
            persistableStore.getJsonRequests().forEach(this::doRequestAuthorization);
            persistableStore.setLastRequested(now);
        }
    }
}