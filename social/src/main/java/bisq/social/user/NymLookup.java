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

import bisq.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// todo: 
// - needs support to update previous nicknames when a new one comes which conflicts
// - add weak references and use bindings
// - persist nymByNickName
@Slf4j
public class NymLookup {
    private static final String SEPARATOR_START = " [";
    private static final String SEPARATOR_END = "]";
    private static final int MAX_NYM_LENGTH = Integer.MAX_VALUE;
    private static final Map<String, Set<String>> nymByNickName = new HashMap<>();

    public static String getUserName(String nym, String nickName) {
        return getUserName(nym, nickName, SEPARATOR_START, SEPARATOR_END, MAX_NYM_LENGTH);
    }

    public static String getUserName(String nym, String nickName, String separatorStart, String separatorEnd, int maxNymLength) {
        if (!nymByNickName.containsKey(nickName)) {
            nymByNickName.put(nickName, new HashSet<>());
        }

        Set<String> profileIds = nymByNickName.get(nickName);
        profileIds.add(nym);
        if (profileIds.size() == 1) {
            return nickName;
        } else {
            return nickName + separatorStart + StringUtils.truncate(nym, maxNymLength) + separatorEnd;
        }
    }
}