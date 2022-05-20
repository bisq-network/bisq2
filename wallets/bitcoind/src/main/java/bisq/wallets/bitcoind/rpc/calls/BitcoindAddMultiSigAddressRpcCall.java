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

package bisq.wallets.bitcoind.rpc.calls;

import bisq.wallets.bitcoind.rpc.responses.BitcoindAddMultisigAddressResponse;
import bisq.wallets.core.rpc.call.WalletRpcCall;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class BitcoindAddMultiSigAddressRpcCall
        extends WalletRpcCall<BitcoindAddMultiSigAddressRpcCall.Request, BitcoindAddMultisigAddressResponse> {
    @Builder
    @Getter
    public static class Request {
        @JsonProperty("nrequired")
        private final int nRequired;
        private final List<String> keys;
    }

    public BitcoindAddMultiSigAddressRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "addmultisigaddress";
    }

    @Override
    public boolean isResponseValid(BitcoindAddMultisigAddressResponse response) {
        return true;
    }

    @Override
    public Class<BitcoindAddMultisigAddressResponse> getRpcResponseClass() {
        return BitcoindAddMultisigAddressResponse.class;
    }
}
