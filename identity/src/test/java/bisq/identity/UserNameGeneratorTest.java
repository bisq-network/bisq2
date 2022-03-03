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

package bisq.identity;

import bisq.security.DigestUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class UserNameGeneratorTest {
    @Test
    void testFromHash() {
        byte[] hash = DigestUtil.hash("test1".getBytes(StandardCharsets.UTF_8));
        // byte[] hash = Hex.decode("11");
        String expected = "TODO";
        String result = UserNameGenerator.fromHash(hash);
        // todo enable once algorithm is implemented
        //assertEquals(expected, result);
    }
}