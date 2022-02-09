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

package bisq.wallets.model;

public record Transaction(String txId, String address, double amount, int confirmations) {
    public static class Builder {
        private String txId;
        private String address;
        private double amount;
        private int confirmations;

        public Transaction build() {
            return new Transaction(
                    txId,
                    address,
                    amount,
                    confirmations
            );
        }

        public Builder txId(String txId) {
            this.txId = txId;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder confirmations(int confirmations) {
            this.confirmations = confirmations;
            return this;
        }
    }
}
