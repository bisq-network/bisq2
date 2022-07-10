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

package bisq.user.profile;

import bisq.network.NetworkService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class UserProfileService implements PersistenceClient<UserProfileStore> {
    private static final String SEPARATOR_START = " [";
    private static final String SEPARATOR_END = "]";

    @Getter
    private final UserProfileStore persistableStore = new UserProfileStore();
    @Getter
    private final Persistence<UserProfileStore> persistence;
    private final NetworkService networkService;

    public UserProfileService(PersistenceService persistenceService,
                              NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.networkService = networkService;
        UserNameLookup.setUserProfileService(this);
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    // todo: 
    // - needs support to update previous nicknames when a new one comes which conflicts
    // - add weak references and use bindings
    public String getUserName(String nym, String nickName) {
        Map<String, Set<String>> nymsByNickName = getNymsByNickName();
        if (!nymsByNickName.containsKey(nickName)) {
            nymsByNickName.put(nickName, new HashSet<>());
        }

        Set<String> nyms = nymsByNickName.get(nickName);
        nyms.add(nym);
        if (nyms.size() == 1) {
            return nickName;
        } else {
            return nickName + SEPARATOR_START + nym + SEPARATOR_END;
        }
    }

    public List<UserProfile> getUserProfiles() {
        // todo
        return new ArrayList<>();
    }

    public Map<String, Set<String>> getNymsByNickName() {
        return persistableStore.getNymsByNickName();
    }
}
