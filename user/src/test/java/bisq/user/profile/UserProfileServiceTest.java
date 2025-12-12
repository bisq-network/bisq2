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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class UserProfileServiceTest {
    @Test
    void testShouldAddNymToNickName() {
        String nickName;
        String nym;
        Map<String, Set<String>> nymsByNickNameFromNetwork;
        Map<String, Set<String>> nymsByNickNameFromContactList;

        // No users in network, none in contract list -> only nickname
        nickName = "Alice";
        nym = "1";
        nymsByNickNameFromNetwork = Map.of();
        nymsByNickNameFromContactList = Map.of();
        assertFalse(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        // No users in network, user in contract list -> only nickname
        nickName = "Alice";
        nym = "1";
        nymsByNickNameFromNetwork = Map.of();
        nymsByNickNameFromContactList = Map.of("Alice", Set.of("1"));
        assertFalse(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        // No users in network, same nickname but different nym in contract list -> nickname+nym
        nickName = "Alice";
        nym = "1";
        nymsByNickNameFromNetwork = Map.of();
        nymsByNickNameFromContactList = Map.of("Alice", Set.of("2"));
        assertTrue(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        // No users in network, different nickname in contract list -> only nickname
        nickName = "Alice";
        nym = "1";
        nymsByNickNameFromNetwork = Map.of();
        nymsByNickNameFromContactList = Map.of("Bob", Set.of("1"));
        assertFalse(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        // One user in network, none in contract list -> only nickname
        nickName = "Alice";
        nym = "1";
        nymsByNickNameFromNetwork = Map.of("Alice", Set.of("1"));
        nymsByNickNameFromContactList = Map.of();
        assertFalse(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        // One user in network, same in contract list -> only nickname
        nickName = "Alice";
        nym = "1";
        nymsByNickNameFromNetwork = Map.of("Alice", Set.of("1"));
        nymsByNickNameFromContactList = Map.of("Alice", Set.of("1"));
        assertFalse(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        // One user in network, different nickname in contract list -> only nickname
        nickName = "Alice";
        nym = "1";
        nymsByNickNameFromNetwork = Map.of("Alice", Set.of("1"));
        nymsByNickNameFromContactList = Map.of("Bob", Set.of("1"));
        assertFalse(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        // One user in network, same nickname but different nym in contract list ->nickname+nym
        nickName = "Alice";
        nym = "1";
        nymsByNickNameFromNetwork = Map.of("Alice", Set.of("1"));
        nymsByNickNameFromContactList = Map.of("Alice", Set.of("2"));
        assertTrue(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        // 2 users with same nickname in network, none in contract list -> nickname+nym
        nickName = "Alice";
        nym = "1";
        nymsByNickNameFromNetwork = Map.of("Alice", Set.of("1", "2"));
        nymsByNickNameFromContactList = Map.of();
        assertTrue(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        nickName = "Alice";
        nym = "2";
        nymsByNickNameFromNetwork = Map.of("Alice", Set.of("1", "2"));
        nymsByNickNameFromContactList = Map.of();
        assertTrue(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        nickName = "Alice";
        nym = "3";
        nymsByNickNameFromNetwork = Map.of("Alice", Set.of("1", "2"));
        nymsByNickNameFromContactList = Map.of();
        assertTrue(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        // Original user not in network anymore, but in contract list. New user with same nickname appear in network -> nickname+nym
        nickName = "Alice";
        nym = "2";
        nymsByNickNameFromNetwork = Map.of("Alice", Set.of("2"));
        nymsByNickNameFromContactList = Map.of("Alice", Set.of("1"));
        assertTrue(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        // Original user not in network anymore, but in contract list. New user with different nickname appear in network -> only nickname
        nickName = "Bob";
        nym = "1";
        nymsByNickNameFromNetwork = Map.of("Bob", Set.of("1"));
        nymsByNickNameFromContactList = Map.of("Alice", Set.of("1"));
        assertFalse(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        nickName = "Alice";
        nym = "1";
        nymsByNickNameFromNetwork = Map.of("Bob", Set.of("1"));
        nymsByNickNameFromContactList = Map.of("Alice", Set.of("1"));
        assertFalse(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

        // Original user not in network anymore, but in contract list.
        // New user with same nickname appear in network, we want result for user in contact list -> nickname+nym
        nickName = "Bob";
        nym = "1";
        nymsByNickNameFromNetwork = Map.of("Bob", Set.of("2"));
        nymsByNickNameFromContactList = Map.of("Bob", Set.of("1"));
        assertTrue(UserProfileService.shouldAddNymToNickName(nickName, nym, nymsByNickNameFromNetwork, nymsByNickNameFromContactList));

    }
}