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

package bisq.network.p2p.services.data.storage;

import bisq.common.data.ByteArray;
import bisq.common.util.ByteArrayUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.security.DigestUtil;

import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Storage key V2: RIPEMD160 of the preimage. The preimage contains the data hash (RIPEMD160 of SHA256), so a single
 * RIPEMD160 is enough.
 * <p>
 * Preimage: the domain separation tag, a 0x00 byte, the 20-byte hash of the stored data, and for
 * signed data the public key bytes of the owner. See docs/specifications/network/storage-keys.md.
 * <p>
 * The layout needs no length prefixes: the tag contains no 0x00 byte, the data hash has a fixed length, and the public
 * key is the last element. So every preimage has exactly one decomposition.
 */
public final class StorageKeyV2 {
    public static final String AUTHENTICATED_DATA_TAG = "bisq.AuthenticatedDataRequest.storageKeyV2";
    public static final String MAILBOX_DATA_TAG = "bisq.MailboxRequest.storageKeyV2";
    public static final String APPEND_ONLY_DATA_TAG = "bisq.AddAppendOnlyDataRequest.storageKeyV2";

    private static final byte[] TAG_TERMINATOR = new byte[]{0};

    public static ByteArray fromPreImage(byte[] preImage) {
        return new ByteArray(DigestUtil.RIPEMD160(preImage));
    }

    public static ByteArray create(String tag, byte[] dataHash, byte[] ownerPublicKeyBytes) {
        return fromPreImage(createPreImage(tag, dataHash, ownerPublicKeyBytes));
    }

    public static byte[] createPreImage(String tag, byte[] dataHash) {
        return ByteArrayUtils.concat(encodeTag(tag), validateDataHash(dataHash));
    }

    public static byte[] createPreImage(String tag, byte[] dataHash, byte[] ownerPublicKeyBytes) {
        checkArgument(ownerPublicKeyBytes.length > 0, "Owner public key must not be empty");
        return ByteArrayUtils.concat(encodeTag(tag), validateDataHash(dataHash), ownerPublicKeyBytes);
    }

    private static byte[] encodeTag(String tag) {
        checkArgument(tag.indexOf('\0') < 0, "Tag must not contain a 0x00 byte");
        return ByteArrayUtils.concat(tag.getBytes(StandardCharsets.US_ASCII), TAG_TERMINATOR);
    }

    private static byte[] validateDataHash(byte[] dataHash) {
        NetworkDataValidation.validateHash(dataHash);
        return dataHash;
    }

    private StorageKeyV2() {
    }
}
