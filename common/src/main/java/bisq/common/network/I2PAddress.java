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

package bisq.common.network;

import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;

import java.util.regex.Pattern;

public class I2PAddress extends Address {
    private static final int MIN_DESTINATION_LENGTH = 60;
    private static final int MAX_DESTINATION_LENGTH = 700;

    private static final Pattern I2P_B32 = Pattern.compile("^[a-z2-7]{52}\\.b32\\.i2p$", Pattern.CASE_INSENSITIVE);
    // Base64: length vary by Signing Key Type used. 516 (DSA_SHA1) - 616 (ECDSA_SHA256_P256).
    // We use EdDSA_SHA512_Ed25519 which has a length of about 524 (+/-padding variance)
    // I2P source code check only for >= 516. To be on the safe side we use 516-700
    private static final Pattern I2P_B64 = Pattern.compile("^[A-Za-z0-9+/]{516,700}={0,2}\\.i2p$", Pattern.CASE_INSENSITIVE);

    public static boolean isI2pAddress(String host) {
        return I2P_B32.matcher(host).matches() || I2P_B64.matcher(host).matches();
    }

    public I2PAddress(String host, int port) {
        super(host, port);
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateText(host, MIN_DESTINATION_LENGTH, MAX_DESTINATION_LENGTH);
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.I2P;
    }

    @Override
    public String toString() {
        return StringUtils.truncate(host, 1000) + ":" + port;
    }
}
