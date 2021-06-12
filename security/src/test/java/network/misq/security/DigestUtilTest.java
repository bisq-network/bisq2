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

package network.misq.security;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Random;

@Slf4j
public class DigestUtilTest {
    @Test
    public void testPerformance() {
        long ts;
        int iterations = 100000;
        byte[] bytes = new byte[1000];
        new Random().nextBytes(bytes);
        DigestUtil.hash(bytes); // call once to not pollute time tests, first call is slow.

        ts = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            DigestUtil.hash(bytes);
        }
        log.info("RIPEMD160(sha256) {}", System.currentTimeMillis() - ts);

        ts = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            DigestUtil.sha256(bytes);
        }
        log.info("sha256 {}", System.currentTimeMillis() - ts);

        ts = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            DigestUtil.RIPEMD160(bytes);
        }
        log.info("RIPEMD160 {}", System.currentTimeMillis() - ts);

        // results
        // 100kb, 1000 iterations:
        //May-24 21:00:39.315 [main] INFO m.c.security.DigestUtilTest: keccak 446
        //May-24 21:00:39.569 [main] INFO m.c.security.DigestUtilTest: RIPEMD160(sha256) 251
        //May-24 21:00:39.811 [main] INFO m.c.security.DigestUtilTest: sha256 242
        //May-24 21:00:40.863 [main] INFO m.c.security.DigestUtilTest: RIPEMD160 1051

        // 1kb, 100 000 iterations
        //May-24 21:01:51.180 [main] INFO m.c.security.DigestUtilTest: keccak 547
        //May-24 21:01:51.584 [main] INFO m.c.security.DigestUtilTest: RIPEMD160(sha256) 402
        //May-24 21:01:51.829 [main] INFO m.c.security.DigestUtilTest: sha256 245
        //May-24 21:01:52.872 [main] INFO m.c.security.DigestUtilTest: RIPEMD160 1042

        // 100 bytes, 100 000 iterations
        //May-24 21:04:00.164 [main] INFO m.c.security.DigestUtilTest: keccak 139
        //May-24 21:04:00.538 [main] INFO m.c.security.DigestUtilTest: RIPEMD160(sha256) 370
        //May-24 21:04:00.616 [main] INFO m.c.security.DigestUtilTest: sha256 77
        //May-24 21:04:00.758 [main] INFO m.c.security.DigestUtilTest: RIPEMD160 141
    }
}
