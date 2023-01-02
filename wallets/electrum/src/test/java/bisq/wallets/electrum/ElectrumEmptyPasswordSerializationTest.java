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

package bisq.wallets.electrum;

import bisq.wallets.electrum.rpc.calls.ElectrumPayToRpcCall;
import bisq.wallets.json_rpc.JsonRpcCall;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ElectrumEmptyPasswordSerializationTest {

    @Test
    void testWithoutPassword() {
        var request = ElectrumPayToRpcCall.Request.builder()
                .destination("destination")
                .amount(0.1)
                .password(null)
                .build();

        JsonRpcCall jsonRpcCall = new JsonRpcCall("test_method", request);

        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<JsonRpcCall> jsonRpcCallJsonAdapter = moshi.adapter(JsonRpcCall.class);
        String jsonRequest = jsonRpcCallJsonAdapter.toJson(jsonRpcCall);

        assertThat(jsonRequest).doesNotContain("password");
    }

    @Test
    void testWithPassword() {
        var request = ElectrumPayToRpcCall.Request.builder()
                .destination("destination")
                .amount(0.1)
                .password("password")
                .build();

        JsonRpcCall jsonRpcCall = new JsonRpcCall("test_method", request);

        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<JsonRpcCall> jsonRpcCallJsonAdapter = moshi.adapter(JsonRpcCall.class);
        String jsonRequest = jsonRpcCallJsonAdapter.toJson(jsonRpcCall);

        System.out.println(jsonRequest);
        assertThat(jsonRequest).contains("password");
    }
}
