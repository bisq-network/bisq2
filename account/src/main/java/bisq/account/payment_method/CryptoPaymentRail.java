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

//TODO Requires more thoughts
public enum CryptoPaymentRail implements PaymentRail {
    CUSTOM,                  // Custom/user-defined
    NATIVE_CHAIN,            // Main chain of the asset (e.g. BTC on Bitcoin, ETH on Ethereum)
    SMART_CCONTRACT,         // Asset transferred via a smart contract on a host chain (ERC20, TRC20, SPL)
    LAYER_2,                 // Layer 2 (Lightning,...)
    SIDECHAIN,               // Sidechain (Liquid, RBTC,...)
    WRAPPED_ASSET,           // Cross-chain wrapped assets (WBTC, WETH)
    CUSTODIAL,               // Off-chain/internal ledger (CEX transfers, PayPal USD)
    CBDC,                    // CBDC
    OTHER                    // Catch-all
}