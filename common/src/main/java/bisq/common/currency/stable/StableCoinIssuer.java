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

public enum StableCoinIssuer {
    TETHER("Tether Ltd."),
    CIRCLE("Circle Internet Financial, LLC"),
    MAKERDAO("MakerDAO"),
    FIRST_DIGITAL("First Digital Trust Limited"),
    PAXOS("Paxos Trust Company"),
    GEMINI("Gemini Trust Company, LLC"),
    TECHTERYX("Techteryx Ltd."), // issuer of TrueUSD (TUSD)
    STABLESAT("Galoy Inc."),     // synthetic USD via Stablesats
    LIGHTNING_LABS("Lightning Labs"), // Taproot Assets experimental issuer
    FEDIMINT("Fedimint Federation");

    @Getter
    private final String displayName;

    StableCoinIssuer(String displayName) {
        this.displayName = displayName;
    }
}

