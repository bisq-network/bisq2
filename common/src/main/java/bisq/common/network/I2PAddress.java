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
import bisq.common.validation.NetworkPortValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@EqualsAndHashCode(callSuper = true)
public class I2PAddress extends Address {
    private static final int MIN_DESTINATION_LENGTH = 60;
    private static final int MAX_DESTINATION_LENGTH = 700;

    private static final Pattern I2P_B32 = Pattern.compile("^[a-z2-7]{52}\\.b32\\.i2p$", Pattern.CASE_INSENSITIVE);
    // Base64: length vary by Signing Key Type used. 516 (DSA_SHA1) - 616 (ECDSA_SHA256_P256).
    // We use EdDSA_SHA512_Ed25519 which has a length of about 524 (+/-padding variance)
    // I2P source code check only for >= 516. To be on the safe side we use 516-700
    // `.i2p` suffix is supported in the base64 format. We use it only internally and there it is not expected.
    // Base 64 encoded string using the I2P alphabet A-Z, a-z, 0-9, -, ~ (See: https://docs.i2p-projekt.de/net/i2p/data/Base64.html)
    private static final Pattern I2P_B64 =
            Pattern.compile("^[A-Za-z0-9\\-~]{516,700}={0,2}$", Pattern.CASE_INSENSITIVE);

    public static boolean isBase32Destination(String destination) {
        return I2P_B32.matcher(destination).matches();
    }

    public static boolean isBase64Destination(String destination) {
        return I2P_B64.matcher(destination).matches();
    }

    @Getter
    private transient Optional<String> destinationBase32 = Optional.empty();

    public I2PAddress(String host, int port) {
        super(host, port);

        if (!isBase64Destination(host)) {
            throw new IllegalArgumentException("I2P host must be in base 64 destination format. " + host);
        }
    }

    public I2PAddress(String destinationBase64, String destinationBase32, int port) {
        super(destinationBase64, port);
        checkArgument(isBase32Destination(destinationBase32), "destinationBase32: " + destinationBase32);
        this.destinationBase32 = Optional.of(destinationBase32);
    }

    @Override
    public void verify() {
        checkArgument(NetworkPortValidation.isValid(port), "Invalid port: " + port);
        NetworkDataValidation.validateText(host, MIN_DESTINATION_LENGTH, MAX_DESTINATION_LENGTH);
        checkArgument(isBase64Destination(host), "Host must be a I2P base 64 destination");
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.I2P;
    }

    @Override
    public String toString() {
        String destination = destinationBase32.orElseGet(() -> StringUtils.truncate(host, 30) + "i2p");
        return destination + ":" + port;
    }

    public String getDestinationBase64() {
        return host;
    }
}
