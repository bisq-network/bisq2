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
import static org.junit.jupiter.api.Assertions.assertTrue;

class PubKeyTest {
    // Chat messages name their author by the ID of the author's public key. The storage compares the hash, so the ID
    // of a hash must be unique.
    @Test
    void findHashFromIdAcceptsOnlyIdsWhichGetIdCreates() {
        PubKey pubKey = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "keyId");
        String id = pubKey.getId();

        assertArrayEquals(pubKey.getHash(), PubKey.findHashFromId(id).orElseThrow());
        assertTrue(PubKey.findHashFromId(id.toUpperCase(Locale.ROOT)).isEmpty());
        assertTrue(PubKey.findHashFromId(id.substring(1)).isEmpty());
        assertTrue(PubKey.findHashFromId("z" + id.substring(1)).isEmpty());
    }
}
