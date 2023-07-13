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

package bisq.bonded_roles.explorer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"txid", "vout", "prevout", "scriptsig", "scriptsig_asm", "witness", "is_coinbase", "sequence"})
public class Input {
    @JsonProperty("txid")
    private String txId;
    @JsonProperty("vout")
    private Integer outputIndex;
    @JsonProperty("prevout")
    private Output prevOut;
    @JsonProperty("scriptsig")
    private String scriptSig;
    @JsonProperty("scriptsig_asm")
    private String scriptSigAsm;
    @JsonProperty("witness")
    private List<String> witness;
    @JsonProperty("is_coinbase")
    private boolean isCoinbase;
    private long sequence;
}