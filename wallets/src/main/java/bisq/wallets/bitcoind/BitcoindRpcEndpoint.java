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

package bisq.wallets.bitcoind;

import lombok.Getter;

public enum BitcoindRpcEndpoint {
    ADD_MULTISIG_ADDRESS("addmultisigaddress"),
    CREATE_WALLET("createwallet"),
    FINALIZE_PSBT("finalizepsbt"),
    GENERATE_TO_ADDRESS("generatetoaddress"),
    GET_ADDRESS_INFO("getaddressinfo"),
    GET_BALANCE("getbalance"),
    GET_NEW_ADDRESS("getnewaddress"),
    IMPORT_ADDRESS("importaddress"),
    LIST_TRANSACTIONS("listtransactions"),
    LIST_UNSPENT("listunspent"),
    LIST_WALLETS("listwallets"),
    LOAD_WALLET("loadwallet"),
    SEND_RAW_TRANSACTION("sendrawtransaction"),
    SEND_TO_ADDRESS("sendtoaddress"),
    STOP("stop"),
    SIGN_MESSAGE("signmessage"),
    UNLOAD_WALLET("unloadwallet"),
    VERIFY_MESSAGE("verifymessage"),
    WALLET_CREATE_FUNDED_PSBT("walletcreatefundedpsbt"),
    WALLET_PASSPHRASE("walletpassphrase"),
    WALLET_PROCESS_PSBT("walletprocesspsbt");

    @Getter
    private final String methodName;

    BitcoindRpcEndpoint(String methodName) {
        this.methodName = methodName;
    }
}
