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

import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.social.user.ChatUser;
import bisq.social.user.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ReputationService {
    private final PersistenceService persistenceService;
    private final NetworkService networkService;
    private final UserProfileService userProfileService;

    public ReputationService(PersistenceService persistenceService, NetworkService networkService, UserProfileService userProfileService) {
        this.persistenceService = persistenceService;
        this.networkService = networkService;
        this.userProfileService = userProfileService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }

    public ReputationScore getReputationScore(ChatUser chatUser) {
        //todo
        return new ReputationScore(new Random().nextInt(100), new Random().nextInt(10000), new Random().nextInt(100) / 100d);
    }
}