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
    CUSTOM,                             // Custom defined payment rail by the user
    NATIVE_CHAIN,                       // The native chain of that cryptocurrency
    LN,                                 // Lightning Network
    LIQUID,                             // Liquid side chain
    BSQ,                                // BSQ (colored coin on the BTC blockchain)
    OTHER;

    @Override
    public String getTradeLimit() {
        //todo
        return "10000 USD";
    }

    @Override
    public String getTradeDuration() {
        return "24 hours";
    }
}