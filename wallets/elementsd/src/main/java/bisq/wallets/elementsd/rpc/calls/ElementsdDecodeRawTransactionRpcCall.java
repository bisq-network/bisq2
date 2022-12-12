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

package bisq.wallets.elementsd.rpc.calls;

import bisq.wallets.elementsd.rpc.responses.ElementsdDecodeRawTransactionResponse;
import bisq.wallets.json_rpc.DaemonRpcCall;
import com.squareup.moshi.Json;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public class ElementsdDecodeRawTransactionRpcCall
        extends DaemonRpcCall<ElementsdDecodeRawTransactionRpcCall.Request, ElementsdDecodeRawTransactionResponse> {

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Request {
        @Json(name = "hexstring")
        private final String hexString;

        public Request(String hexString) {
            this.hexString = hexString;
        }
    }

    public ElementsdDecodeRawTransactionRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "decoderawtransaction";
    }

    @Override
    public boolean isResponseValid(ElementsdDecodeRawTransactionResponse response) {
        return true;
    }

    @Override
    public Class<ElementsdDecodeRawTransactionResponse> getRpcResponseClass() {
        return ElementsdDecodeRawTransactionResponse.class;
    }
}
