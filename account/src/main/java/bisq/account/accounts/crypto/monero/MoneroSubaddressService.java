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

package bisq.account.accounts.crypto.monero;

import bisq.account.accounts.crypto.monero.knaccc.monero.address.WalletAddress;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MoneroSubaddressService {

    public String generateSubAddress(String mainAddress, String privateViewKey, long accountIndex, long subAddressIndex) {
        checkArgument(accountIndex >= 0, "accountIndex must be >= 0, was: %s", accountIndex);
        checkArgument(subAddressIndex >= 0, "subAddressIndex must be >= 0, was: %s", subAddressIndex);
        checkArgument(accountIndex > 0 || subAddressIndex > 0, "accountIndex and subAddressIndex cannot both be 0 (would represent main address)");
        checkArgument(mainAddress != null && !mainAddress.trim().isEmpty(), "mainAddress must not be null or empty");
        checkArgument(privateViewKey != null && !privateViewKey.trim().isEmpty(), "privateViewKey must not be null or empty");

        try {
            WalletAddress walletAddress = new WalletAddress(mainAddress);
            long start = System.currentTimeMillis();
            String subAddress = walletAddress.getSubaddressBase58(privateViewKey, accountIndex, subAddressIndex);
            log.info("Created new subAddress {}. Took {} ms.", subAddress, System.currentTimeMillis() - start);
            return subAddress;
        } catch (WalletAddress.InvalidWalletAddressException e) {
            log.error("WalletAddress.getSubaddressBase58 failed for mainAddress: {}", mainAddress, e);
            throw new IllegalArgumentException("Invalid main address or private view key", e);
        }
    }
}