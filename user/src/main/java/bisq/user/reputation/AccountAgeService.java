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
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.oracle.daobridge.DaoBridgeService;
import bisq.oracle.daobridge.model.AccountAgeCertificateRequest;
import bisq.oracle.daobridge.model.AuthorizedAccountAgeData;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class AccountAgeService extends SourceReputationService<AuthorizedAccountAgeData> {
    private static final long DAY_MS = TimeUnit.DAYS.toMillis(1);
    public static final long WEIGHT = 10;
    private final Map<Transport.Type, List<Address>> bridgeNodeAddressByTransportType;

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

    private final String baseDir;

    public AccountAgeService(String baseDir,
                             NetworkService networkService,
                             UserIdentityService userIdentityService,
                             UserProfileService userProfileService,
                             DaoBridgeService daoBridgeService) {
        super(networkService, userIdentityService, userProfileService);
        this.baseDir = baseDir;
        bridgeNodeAddressByTransportType = daoBridgeService.getBridgeNodeAddressByTransportType();
    }

    public void requestCertificate(Optional<String> clipboard) {
        clipboard.ifPresent(json -> {
            AccountAgeWitnessDto dto = new Gson().fromJson(json, AccountAgeWitnessDto.class);
            String profileId = dto.getProfileId();
            userIdentityService.findUserIdentity(profileId).ifPresent(userIdentity -> {
                String senderNodeId = userIdentity.getNodeIdAndKeyPair().getNodeId();
                AccountAgeCertificateRequest networkMessage = new AccountAgeCertificateRequest(profileId,
                        dto.getHashAsHex(),
                        dto.getDate(),
                        dto.getPubKeyBase64(),
                        dto.getSignatureBase64());
                getAddresses().forEach(address -> networkService.send(senderNodeId, networkMessage, address));
            });
        });
    }

    @Override
    protected void processAuthenticatedData(AuthenticatedData authenticatedData) {
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
    protected long calculateScore(AuthorizedAccountAgeData data) {
        return Math.min(365, getAgeInDays(data.getTime())) * WEIGHT;
    }

    private List<Map<Transport.Type, Address>> getAddresses() {
        List<Map<Transport.Type, Address>> addresses = new ArrayList<>();
        Map<Transport.Type, List<Address>> mutableClone = new HashMap<>();
        bridgeNodeAddressByTransportType.forEach((key, value) -> mutableClone.put(key, new ArrayList<>(value)));
        int iterations = 0;
        while (iterations < 10 &&
                mutableClone.values().stream().mapToLong(Collection::size).sum() > 0) {
            iterations++;
            Map<Transport.Type, Address> temp = new HashMap<>();
            mutableClone.forEach((type, value) -> {
                Optional<Address> optional = value.stream().findAny();
                if (optional.isPresent()) {
                    Address address = optional.get();
                    value.remove(address);
                    temp.put(type, address);
                }
            });
            addresses.add(temp);
        }
        return addresses;
    }
}