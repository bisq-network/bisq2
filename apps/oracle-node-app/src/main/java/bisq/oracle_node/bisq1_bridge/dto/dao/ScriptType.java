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

package bisq.oracle_node.bisq1_bridge.dto.dao;

import lombok.Getter;

@SuppressWarnings("ALL")
@Getter
public enum ScriptType {
    UNDEFINED("undefined"),
    // https://github.com/bitcoin/bitcoin/blob/master/src/script/standard.cpp
    NONSTANDARD("nonstandard"),
    PUB_KEY("pubkey"),
    PUB_KEY_HASH("pubkeyhash"),
    SCRIPT_HASH("scripthash"),
    MULTISIG("multisig"),
    NULL_DATA("nulldata"),
    WITNESS_V0_KEYHASH("witness_v0_keyhash"),
    WITNESS_V0_SCRIPTHASH("witness_v0_scripthash"),
    WITNESS_V1_TAPROOT("witness_v1_taproot"),
    WITNESS_UNKNOWN("witness_unknown");

    private final String name;

    ScriptType(String name) {
        this.name = name;
    }
}
