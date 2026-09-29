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

package bisq.security.keys;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PubKeyTest {
    // Chat messages name their author by the ID of the author's public key. The storage compares the hash, so a hash
    // must have only one ID.
    @Test
    void onlyIdsWhichGetIdCreatesAreIds() {
        PubKey pubKey = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "keyId");
        String id = pubKey.getId();

        assertTrue(PubKey.isId(id));
        assertArrayEquals(pubKey.getHash(), PubKey.getHashFromId(id));
        assertFalse(PubKey.isId(id.toUpperCase(Locale.ROOT)));
        assertFalse(PubKey.isId(id.substring(1)));
        assertFalse(PubKey.isId("z" + id.substring(1)));
        assertThrows(IllegalArgumentException.class, () -> PubKey.getHashFromId(id.toUpperCase(Locale.ROOT)));
    }

    // A hash has 20 bytes, so an ID has 40 characters. Other lengths are valid hex encodings, but no IDs.
    @Test
    void idsOfOtherLengthsAreNoIds() {
        String id = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "keyId").getId();

        assertFalse(PubKey.isId(""));
        assertFalse(PubKey.isId(id.substring(2)));
        assertFalse(PubKey.isId(id + "00"));
        assertThrows(IllegalArgumentException.class, () -> PubKey.getHashFromId(id.substring(2)));
    }
}
