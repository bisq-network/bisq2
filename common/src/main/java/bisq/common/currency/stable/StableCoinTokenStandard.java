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

package bisq.common.currency.stable;

import lombok.Getter;

public enum StableCoinTokenStandard {
    // Ethereum
    ERC20("ERC-20"),
    ERC721("ERC-721"),
    ERC1155("ERC-1155"),
    ERC4626("ERC-4626"),
    ERC777("ERC-777"),

    // Thron
    TRC20("TRC-20"),

    // Bitcoin
    RGB("RGB"),
    Omni("Omni"),

    // Lightning Network
    TaprootAssets("Taproot Assets"),
    BOLT11("BOLT-11"),

    // Solana
    SPL("SPL"),

    // BNB Smart Chain
    BEP20("BEP-20"),
    BEP721("BEP-721"),
    BEP1155("BEP-1155");

    @Getter
    private final String displayName;

    StableCoinTokenStandard(String displayName) {
        this.displayName = displayName;
    }
}
