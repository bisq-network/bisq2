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

package bisq.wallets.json_rpc;

import lombok.Getter;
import okhttp3.Credentials;
import okhttp3.HttpUrl;

@Getter
public class JsonRpcEndpointSpec {
    private final HttpUrl url;
    private final String username;
    private final String password;

    public JsonRpcEndpointSpec(String url, String username, String password) {
        this(HttpUrl.parse(url), username, password);
    }

    public JsonRpcEndpointSpec(HttpUrl url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getAuthHeaderValue() {
        return Credentials.basic(username, password);
    }
}
