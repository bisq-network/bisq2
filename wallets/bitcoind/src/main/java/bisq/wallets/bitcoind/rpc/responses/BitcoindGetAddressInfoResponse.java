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

package bisq.wallets.bitcoind.rpc.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BitcoindGetAddressInfoResponse {
    private String address;
    private String scriptPubKey;
    private boolean ismine;
    private boolean iswatchonly;
    private boolean solvable;

    private String desc;
    @JsonProperty("parent_desc")
    private String parentDesc;

    private boolean isscript;
    private boolean ischange;
    private boolean iswitness;
    private int witness_version;
    private String witness_program;
    private String script;
    private String hex;
    private String[] pubkeys;
    private int sigsrequired;
    private String pubkey;
    private Object embedded;
    private boolean iscompressed;
    private int timestamp;
    private String hdkeypath;
    private String hdseedid;
    private String hdmasterfingerprint;
    private String[] labels;
}
