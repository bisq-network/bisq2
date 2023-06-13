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

package bisq.account.payment_method;

public enum CryptoPaymentRail implements PaymentRail {
    CUSTOM,                             // Custom defined payment rail by the user
    BSQ,                                // BSQ (colored coin on the BTC blockchain)
    MONERO,                             // Monero blockchain
    LIQUID,                             // Liquid side chain
    NATIVE_CHAIN,                       // The native chain of that cryptocurrency
    ATOMIC_SWAP_CAPABLE_CHAIN,          // A blockchain capable for atomic cross chain swaps
    OTHER
}