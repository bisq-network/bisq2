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

package network.misq.protocol;

import java.util.Map;

/**
 * Provides the security related aspects for the protocol.
 */
public interface SecurityProvider {
    Type getType();

    enum Type {
        /**
         * For trustless protocols, such as cross-chain swaps like Farcaster, Submarine (on-chain to Lightning) swaps,
         * same-chain BSQ-BTC swaps, BSQ-backed BTC loans repayable using SIGHASH_ANYONECANPAY, etc.
         */
        SMART_CONTRACT,
        /**
         * For 2-3 and 2-2 multisig (MAD or arbitrator-backed) and off-chain (Lightning) escrow secured contracts.
         */
        ESCROW,
        /**
         * For bond (e.g. burnable BSQ) secured contracts.
         */
        BOND,
        /**
         * For reputation secured contracts.
         */
        REPUTATION
    }

    /**
     * A unit type. This is a workaround for the fact that all initialized {@link SharedState} properties must be
     * non-null, so {@link Void} cannot be used as a return type. TODO: Consider allowing null return values.
     */
    enum Unit {UNIT}

    interface SharedState {
        Map<String, ?> sendMessage(String recipient);

        void receiveMessage(String sender, Map<String, ?> message);
    }
}
