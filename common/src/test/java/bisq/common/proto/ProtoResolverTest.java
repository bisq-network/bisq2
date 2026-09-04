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

package bisq.common.proto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtoResolverTest {
    @Test
    void protoTypeIsTheSecondPackageTokenAndTheSimpleName() {
        assertEquals("common.NetworkStorageWhiteList", ProtoResolver.getProtoType(NetworkStorageWhiteList.class));
    }

    @Test
    void classWithoutAPackageIsRejectedWithItsName() {
        // int.class stands in for a class whose name lost its package, which is what an obfuscator repackaging into
        // the root package produces. The message has to name the class, otherwise there is no way to tell which keep
        // rule is missing.
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ProtoResolver.getProtoType(int.class));

        assertTrue(exception.getMessage().contains("int"), exception.getMessage());
    }
}
