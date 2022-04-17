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

package bisq.social.user;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//todo needs support to update previous nicknames when a new one comes which conflicts
// add weak references and use bindings
// persist profileIdsByNickName as well
@Slf4j
public class UserNameLookup {
    private static final String SEPARATOR_START = " [";
    private static final String SEPARATOR_END = "]";
    private static final int MAX_PROFILE_ID_LENGTH = Integer.MAX_VALUE;
    private static final Map<String, Set<String>> profileIdsByNickName = new HashMap<>();

    public static String getUserName(String profileId, String nickName) {
        return getUserName(profileId, nickName, SEPARATOR_START, SEPARATOR_END, MAX_PROFILE_ID_LENGTH);
    }

    public static String getUserName(String profileId, String nickName, String separatorStart, String separatorEnd, int maxProfileIdLength) {
        if (!profileIdsByNickName.containsKey(nickName)) {
            profileIdsByNickName.put(nickName, new HashSet<>());
        }

        Set<String> profileIds = profileIdsByNickName.get(nickName);
        profileIds.add(profileId);
        if (profileIds.size() == 1) {
            return nickName;
        } else {
            if (maxProfileIdLength < profileId.length()) {
                String truncatedProfileId = profileId.substring(0, maxProfileIdLength);
                return nickName + separatorStart + truncatedProfileId + "..." + separatorEnd;
            } else {
                return nickName + separatorStart + profileId + separatorEnd;
            }
        }
    }
}