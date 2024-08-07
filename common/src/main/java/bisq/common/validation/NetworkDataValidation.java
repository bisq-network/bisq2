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

package bisq.common.validation;

import bisq.common.platform.Version;
import bisq.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class NetworkDataValidation {
    public static final long TWO_HOURS = TimeUnit.HOURS.toMillis(2);
    public static final long BISQ_1_LAUNCH_DATE = DateUtils.getUTCDate(2016, GregorianCalendar.APRIL, 27).getTime();

    public static void validateDate(long date) {
        // Date can be max 2 hours in future and cannot be older than bisq 1 launch date
        checkArgument(date < System.currentTimeMillis() + TWO_HOURS && date > BISQ_1_LAUNCH_DATE,
                "Date is either too far in the future or too far in the past. date=" + new Date(date));
    }

    public static void validateHash(byte[] hash) {
        checkArgument(hash.length == 20,
                "Hash must be 20 bytes");
    }

    // Signature are usually 71 - 73 chars
    public static void validateECSignature(byte[] signature) {
        checkArgument(signature.length >= 68 && signature.length <= 74,
                "Signature not of the expected size. signature=" + Arrays.toString(signature));
    }

    public static void validateECPubKey(byte[] pubKey) {
        checkArgument(pubKey.length > 50 && pubKey.length < 100,
                "Public key not of the expected size. pubKey=" + Arrays.toString(pubKey));
    }

    public static void validateECPubKey(PublicKey publicKey) {
        validateECPubKey(publicKey.getEncoded());
    }


    // IDs are created with StringUtils.createUid() which generates 36 chars. We allow upt to 50 for more flexibility.
    // Can be short id as well or custom IDs...
    public static void validateId(String id) {
        checkArgument(id.length() <= 50, "ID must not be longer than 50 characters. id=" + id);
    }

    public static void validateId(Optional<String> id) {
        id.ifPresent(NetworkDataValidation::validateId);
    }

    // Profile ID is hash as hex
    public static void validateProfileId(String profileId) {
        checkArgument(profileId.length() == 40, "Profile ID must be 40 characters. profileId=" + profileId);
    }

    public static void validateTradeId(String tradeId) {
        // For private channels we combine user profile IDs for channelId
        validateText(tradeId, 200);
    }

    public static void validateText(String text, int maxLength) {
        checkArgument(text.length() <= maxLength,
                "Text must not be longer than " + maxLength + ". text=" + text);
    }

    public static void validateText(Optional<String> text, int maxTextLength) {
        text.ifPresent(e -> validateText(e, maxTextLength));
    }

    public static void validateByteArray(byte[] bytes, int maxLength) {
        checkArgument(bytes.length <= maxLength,
                "Byte array must not be longer than " + maxLength + ". bytes.length=" + bytes.length);
    }

    // Longest supported version is xxx.xxx.xxx
    public static void validateVersion(String version) {
        Version.validate(version);
        checkArgument(version.length() <= 11 && !version.isEmpty(),
                "Version too long or empty. version=" + version);
    }

    // Language or country code
    public static void validateCode(String code) {
        checkArgument(code.length() < 10,
                "Code too long. code=" + code);
    }

    public static void validateBtcTxId(String txId) {
        checkArgument(txId.length() == 64,
                "BTC txId must be 64 characters. txId=" + txId);
    }

    public static void validateBtcAddress(String address) {
        checkArgument(address.length() >= 26 && address.length() <= 90,
                "BTC address not in expected size. address=" + address);
    }

    public static void validateHashAsHex(String hashAsHex) {
        checkArgument(hashAsHex.length() == 40,
                "Hash as hex must be 40 characters. hashAsHex=" + hashAsHex);
    }

    // Bisq 1 pubKeys about 600 chars
    public static void validatePubKeyBase64(String pubKeyBase64) {
        checkArgument(pubKeyBase64.length() < 1000,
                "pubKeyBase64 too long. pubKeyBase64=" + pubKeyBase64);
    }

    // About 176 chars
    public static void validatePubKeyHex(String pubKeyHex) {
        checkArgument(pubKeyHex.length() < 200,
                "signatureBase64 too long.pubKeyHex=" + pubKeyHex);
    }

    // Bisq 1 signatureBase64 about 88 chars 
    public static void validateSignatureBase64(String signatureBase64) {
        checkArgument(signatureBase64.length() < 100,
                "signatureBase64 too long. signatureBase64=" + signatureBase64);
    }

    // Bisq 1 bond usernames
    public static void validateBondUserName(String bondUserName) {
        checkArgument(bondUserName.length() < 100,
                "Bond username too long. bondUserName=" + bondUserName);
    }
}