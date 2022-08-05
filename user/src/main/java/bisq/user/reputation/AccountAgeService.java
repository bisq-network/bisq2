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

import bisq.common.data.ByteArray;
import bisq.common.timer.Scheduler;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.oracle.daobridge.model.AuthorizeAccountAgeRequest;
import bisq.oracle.daobridge.model.AuthorizedAccountAgeData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * We persist our json request data and do the authorisation request again at each start if the age of the last request
 * exceeds the half of the TTL of the AuthorizedAccountAgeData. That way the network does not keep inactive data for
 * too long.
 */
@Getter
@Slf4j
public class AccountAgeService extends SourceReputationService<AuthorizedAccountAgeData> implements PersistenceClient<AccountAgeStore> {
    private static final long DAY_MS = TimeUnit.DAYS.toMillis(1);
    public static final long WEIGHT = 10;

    // Has to be in sync with Bisq1 class
    @Getter
    static class AccountAgeWitnessDto {
        private final String profileId;
        private final String hashAsHex;
        private final long date;
        private final String pubKeyBase64;
        private final String signatureBase64;

        public AccountAgeWitnessDto(String profileId,
                                    String hashAsHex,
                                    long date,
                                    String pubKeyBase64,
                                    String signatureBase64) {
            this.profileId = profileId;
            this.hashAsHex = hashAsHex;
            this.date = date;
            this.pubKeyBase64 = pubKeyBase64;
            this.signatureBase64 = signatureBase64;
        }
    }

    @Getter
    private final AccountAgeStore persistableStore = new AccountAgeStore();
    @Getter
    private final Persistence<AccountAgeStore> persistence;
    private final String baseDir;

    public AccountAgeService(String baseDir,
                             PersistenceService persistenceService,
                             NetworkService networkService,
                             UserIdentityService userIdentityService,
                             UserProfileService userProfileService) {
        super(networkService, userIdentityService, userProfileService);
        this.baseDir = baseDir;
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        // We delay a bit to ensure the network is well established
        Scheduler.run(this::maybeRequestAgain).after(3, TimeUnit.SECONDS);
        return super.initialize();
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedAccountAgeData) {
            AuthorizedAccountAgeData data = (AuthorizedAccountAgeData) authenticatedData.getDistributedData();
            String userProfileId = data.getProfileId();
            userProfileService.findUserProfile(userProfileId)
                    .map(this::getUserProfileKey)
                    .ifPresent(dataSetByHash::remove);
            if (scoreByUserProfileId.containsKey(userProfileId)) {
                scoreByUserProfileId.remove(userProfileId);
                userProfileIdOfUpdatedScore.set(userProfileId);
            }
        }
    }

    public boolean requestAuthorization(String json) {
        persistableStore.getJsonRequests().add(json);
        persist();
        return doRequestAuthorization(json);
    }

    @Override
    protected void processAuthenticatedData(AuthenticatedData authenticatedData) {
        super.processAuthenticatedData(authenticatedData);
        if (authenticatedData.getDistributedData() instanceof AuthorizedAccountAgeData) {
            processData((AuthorizedAccountAgeData) authenticatedData.getDistributedData());
        }
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
        return Math.min(365, getAgeInDays(data.getDate())) * WEIGHT;
    }

    private boolean doRequestAuthorization(String json) {
        try {
            AccountAgeWitnessDto dto = new Gson().fromJson(json, AccountAgeWitnessDto.class);
            String profileId = dto.getProfileId();
            if (daoBridgeServiceProviders.isEmpty()) {
                log.warn("daoBridgeServiceProviders is empty");
                return false;

            }
            return userIdentityService.findUserIdentity(profileId).map(userIdentity -> {
                        AuthorizeAccountAgeRequest networkMessage = new AuthorizeAccountAgeRequest(profileId,
                                dto.getHashAsHex(),
                                dto.getDate(),
                                dto.getPubKeyBase64(),
                                dto.getSignatureBase64());
                        daoBridgeServiceProviders.forEach(receiverNetworkId ->
                                networkService.confidentialSend(networkMessage, receiverNetworkId, userIdentity.getNodeIdAndKeyPair()));
                        return true;
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
            persistableStore.getJsonRequests().forEach(this::doRequestAuthorization);
            persistableStore.setLastRequested(now);
        }
    }
}