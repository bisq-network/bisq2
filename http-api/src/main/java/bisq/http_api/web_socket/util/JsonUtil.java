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

package bisq.http_api.web_socket.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtil {
    // We use by convention same class name. We get the className field set by the client.
    public static boolean hasExpectedJsonClassName(Class<?> clazz, String json) {
        String regex = "\"className\":\"[^\"]*\\.([^\"]+)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String className = matcher.group(1);
            return clazz.getSimpleName().equals(className);
        } else {
            return false;
        }
    }
}
