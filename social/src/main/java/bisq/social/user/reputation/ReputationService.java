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

package bisq.social.user.reputation;

import bisq.common.data.ByteArray;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.oracle.daobridge.model.AuthorizedProofOfBurnData;
import bisq.persistence.PersistenceService;
import bisq.social.user.ChatUser;
import bisq.social.user.ChatUserService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ReputationService implements DataService.Listener {
    private final PersistenceService persistenceService;
    private final NetworkService networkService;
    private final ChatUserService chatUserService;
    private final Map<ByteArray, AuthorizedProofOfBurnData> authorizedProofOfBurnDataByHash = new ConcurrentHashMap<>();

    public ReputationService(PersistenceService persistenceService, NetworkService networkService, ChatUserService chatUserService) {
        this.persistenceService = persistenceService;
        this.networkService = networkService;
        this.chatUserService = chatUserService;


    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.addDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedProofOfBurnData authorizedProofOfBurnData) {
            addAuthorizedProofOfBurnData(authorizedProofOfBurnData);
        }
    }

    private void addAuthorizedProofOfBurnData(AuthorizedProofOfBurnData authorizedProofOfBurnData) {
        authorizedProofOfBurnDataByHash.put(new ByteArray(authorizedProofOfBurnData.getHash()), authorizedProofOfBurnData);
    }

    public ReputationScore getReputationScore(ChatUser chatUser) {
        //todo
        return new ReputationScore(new Random().nextInt(100), new Random().nextInt(10000), new Random().nextInt(100) / 100d);
    }

}