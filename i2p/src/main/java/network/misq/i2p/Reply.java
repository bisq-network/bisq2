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

package network.misq.i2p;

import java.util.HashMap;
import java.util.Map;

class Reply {
    private final String reply;
    private final Map<String, String> map = new HashMap<>();

    Reply(String reply) {
        this.reply = reply;

        String[] tokens = reply.split(" ");
        for (String token : tokens) {
            if (token.contains("=")) {
                String[] keyValuePair = token.split("=", 2);
                map.put(keyValuePair[0], keyValuePair[1]);
            }
        }
    }

    boolean isSuccess() {
        return "OK".equals(get("RESULT"));
    }

    boolean isError() {
        return !reply.startsWith("DEST REPLY") && !isSuccess();
    }

    String get(String key) {
        return map.get(key);
    }

    @Override
    public String toString() {
        return reply;
    }
}
