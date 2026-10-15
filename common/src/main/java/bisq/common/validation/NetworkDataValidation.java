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
import bisq.common.proto.Proto;
import bisq.common.util.DateUtils;
import bisq.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class NetworkDataValidation {
    public static final long TWO_HOURS = TimeUnit.HOURS.toMillis(2);
    public static final long BISQ_1_LAUNCH_DATE = DateUtils.getUTCDate(2016, GregorianCalendar.APRIL, 27).getTime();
    /**
     * Exposed so a caller that has to reject the id before we do can ask for the width rather than
     * repeat it. Both messages below append the input they rejected, which is what makes a caller
     * facing untrusted input check first instead of catching.
     */
    public static final int PROFILE_ID_LENGTH = 40;
    public static final int MAX_ID_LENGTH = 50;

    // The messages of the exceptions name the limit and the size of a rejected value, not the value. The values come
    // from other nodes, for example chat texts, payment account data or Bitcoin addresses, and the callers log the
    // exception messages. Users share their log files with support.
    public static void validateDate(long date) {
        // Date can be max 2 hours in future and cannot be older than bisq 1 launch date
        checkArgument(date < System.currentTimeMillis() + TWO_HOURS && date > BISQ_1_LAUNCH_DATE,
                "Date is either too far in the future or too far in the past. date=" + new Date(date));
    }

    public static void validateHash(byte[] hash) {
        checkArgument(hash.length == 20,
                "Hash must be 20 bytes");
    }

    // The DER encoding of an ECDSA signature usually has 70 to 72 bytes. It is shorter if r or s has leading zero bytes.
    // Nodes of all versions reject a signature outside of this range, so a node must not create one (see
    // SignatureUtil.sign).
    public static final int MIN_EC_SIGNATURE_LENGTH = 68;
    public static final int MAX_EC_SIGNATURE_LENGTH = 74;

    public static void validateECSignature(byte[] signature) {
        checkArgument(signature.length >= MIN_EC_SIGNATURE_LENGTH && signature.length <= MAX_EC_SIGNATURE_LENGTH,
                "Signature not of the expected size. signature.length=" + signature.length);
    }

    // ECPubKey is usually 88–91 bytes
    public static void validateECPubKey(byte[] pubKey) {
        checkArgument(isECPubKeySizeValid(pubKey),
                "Public key not of the expected size. pubKey.length=" + pubKey.length);
    }

    // For a caller which must reject key bytes of another size before it parses them
    public static boolean isECPubKeySizeValid(byte[] pubKey) {
        return pubKey.length > 50 && pubKey.length < 100;
    }


    // IDs are created with StringUtils.createUid() which generates 36 chars. We allow upt to 50 for more flexibility.
    // Can be short id as well or custom IDs...
    public static void validateId(String id) {
        checkArgument(id.length() <= MAX_ID_LENGTH,
                "ID must not be longer than " + MAX_ID_LENGTH + " characters. id.length=" + id.length());
    }

    public static void validateId(Optional<String> id) {
        id.ifPresent(NetworkDataValidation::validateId);
    }

    // Profile ID is hash as hex
    public static void validateProfileId(String profileId) {
        checkArgument(profileId.length() == PROFILE_ID_LENGTH,
                "Profile ID must be " + PROFILE_ID_LENGTH + " characters. profileId.length=" + profileId.length());
    }

    public static void validateTradeId(String tradeId) {
        // For private channels we combine user profile IDs for channelId
        validateText(tradeId, 200);
    }

    public static void validateSerializedSize(Proto proto, int maxSize) {
        checkArgument(proto.getSerializedSize() <= maxSize,
                "Proto must not be larger than " + maxSize + " bytes. serializedSize=" + proto.getSerializedSize());
    }

    public static void validateText(String text, int maxLength) {
        checkArgument(text.length() <= maxLength,
                "Text must not be longer than " + maxLength + ". text.length=" + text.length());
    }

    public static void validateText(String text, int minLength, int maxLength) {
        validateText(text, maxLength);
        checkArgument(text.length() >= minLength,
                "Text must not be shorter than " + minLength + ". text.length=" + text.length());
    }

    public static void validateText(Optional<String> text, int maxTextLength) {
        text.ifPresent(e -> validateText(e, maxTextLength));
    }

    public static void validateRequiredText(String text, int minLength, int maxLength) {
        checkArgument(!StringUtils.isEmpty(text), "Text must not be null or empty");
        validateText(text, minLength, maxLength);
    }

    public static void validateRequiredText(String text, int maxLength) {
        checkArgument(!StringUtils.isEmpty(text), "Text must not be null or empty");
        validateText(text, maxLength);
    }

    public static void validateOptionalText(String text, int maxLength) {
        if (StringUtils.isNotEmpty(text)) {
            validateText(text, maxLength);
        }
    }

    public static void validateByteArray(byte[] bytes, int maxLength) {
        checkArgument(bytes.length <= maxLength,
                "Byte array must not be longer than " + maxLength + ". bytes.length=" + bytes.length);
    }

    public static void validateByteArray(byte[] bytes, int minLength, int maxLength) {
        checkArgument(bytes.length >= minLength,
                "Byte array must not be shorter than " + minLength + ". bytes.length=" + bytes.length);
        validateByteArray(bytes, maxLength);
    }

    // Longest supported version is xxx.xxx.xxx
    public static void validateVersion(String version) {
        Version.validate(version);
        checkArgument(version.length() <= 11 && !version.isEmpty(),
                "Version too long or empty. version.length=" + version.length());
    }

    // Language or country code
    public static void validateCode(String code) {
        checkArgument(code.length() < 10,
                "Code too long. code.length=" + code.length());
        checkArgument(code.length() > 1,
                "Code too short. code.length=" + code.length());
    }

    public static void validateRequiredCode(String code) {
        checkArgument(!StringUtils.isEmpty(code), "Code must not be null or empty");
        validateCode(code);
    }

    public static void validateBtcTxId(String txId) {
        checkArgument(txId.length() == 64,
                "BTC txId must be 64 characters. txId.length=" + txId.length());
    }

    public static void validateBtcAddress(String address) {
        checkArgument(address.length() >= 26 && address.length() <= 90,
                "BTC address not in expected size. address.length=" + address.length());
    }

    public static void validateHashAsHex(String hashAsHex) {
        checkArgument(hashAsHex.length() == 40,
                "Hash as hex must be 40 characters. hashAsHex.length=" + hashAsHex.length());
    }

    // Bisq 1 pubKeys about 600 chars
    public static void validatePubKeyBase64(String pubKeyBase64) {
        checkArgument(pubKeyBase64.length() < 1000,
                "pubKeyBase64 too long. pubKeyBase64.length=" + pubKeyBase64.length());
    }

    // About 176 chars
    public static void validatePubKeyHex(String pubKeyHex) {
        checkArgument(pubKeyHex.length() < 200,
                "pubKeyHex too long. pubKeyHex.length=" + pubKeyHex.length());
    }

    // Bisq 1 signatureBase64 about 88 chars 
    public static void validateSignatureBase64(String signatureBase64) {
        checkArgument(signatureBase64.length() < 100,
                "signatureBase64 too long. signatureBase64.length=" + signatureBase64.length());
    }

    // Bisq 1 bond usernames
    public static void validateBondUserName(String bondUserName) {
        checkArgument(bondUserName.length() < 100,
                "Bond username too long. bondUserName.length=" + bondUserName.length());
    }

    // A Bisq 1 bond is looked up by its username, so an empty name can never identify a bond. Used for messages
    // sent to the Bisq 1 bridge. Parsing of received or persisted records keeps using validateBondUserName, so
    // that a stricter rule cannot make an existing record unreadable.
    public static void validateRequiredBondUserName(String bondUserName) {
        checkArgument(!StringUtils.isEmpty(bondUserName), "Bond username must not be null or empty");
        validateBondUserName(bondUserName);
    }
}