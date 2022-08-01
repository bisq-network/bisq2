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
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.oracle.daobridge.model.AuthorizeSignedWitnessRequest;
import bisq.oracle.daobridge.model.AuthorizedSignedWitnessData;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class SignedWitnessService extends SourceReputationService<AuthorizedSignedWitnessData> {
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

    private final String baseDir;

    public SignedWitnessService(String baseDir,
                                NetworkService networkService,
                                UserIdentityService userIdentityService,
                                UserProfileService userProfileService) {
        super(networkService, userIdentityService, userProfileService);
        this.baseDir = baseDir;
    }

    public boolean requestAuthorization(String json) {
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
                        String senderNodeId = userIdentity.getNodeIdAndKeyPair().getNodeId();
                        AuthorizeSignedWitnessRequest networkMessage = new AuthorizeSignedWitnessRequest(profileId,
                                dto.getHashAsHex(),
                                dto.getAccountAgeWitnessDate(),
                                witnessSignDate,
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

    @Override
    protected void processAuthenticatedData(AuthenticatedData authenticatedData) {
        super.processAuthenticatedData(authenticatedData);
        if (authenticatedData.getDistributedData() instanceof AuthorizedSignedWitnessData) {
            processData((AuthorizedSignedWitnessData) authenticatedData.getDistributedData());
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
}