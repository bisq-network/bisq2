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

package bisq.user;

import bisq.security.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static bisq.user.NymIdGenerator.fromHash;
import static bisq.user.NymIdGenerator.read;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class NymIdGeneratorTest {
    @Test
    void testFromHash() {
        List<String> adverbs, adjectives, nouns;

        // Min. lists
        adverbs = List.of("1");
        adjectives = List.of("2");
        nouns = List.of("3");
        assertEquals("1230", NymIdGenerator.fromHash(BigInteger.valueOf(0), adverbs, adjectives, nouns));
        //System.out.println("from hash: "+BigInteger.valueOf(0));
        assertEquals("1231", NymIdGenerator.fromHash(BigInteger.valueOf(1), adverbs, adjectives, nouns));
        assertEquals("1232", NymIdGenerator.fromHash(BigInteger.valueOf(2), adverbs, adjectives, nouns));

        adverbs = List.of("1");
        adjectives = List.of("2", "3");
        nouns = List.of("4", "5", "6");

        // util for printing results
           /* int combinations = adverbs.size() * adjectives.size() * nouns.size();
            for (int i = 0; i < 2 * combinations; i++) {
                BigInteger hashAsBigInteger = BigInteger.valueOf(i);
                String result = fromHash(hashAsBigInteger, adverbs, adjectives, nouns);
                System.out.println("assertEquals(\"" + result + "\", fromHash(BigInteger.valueOf(" + i + "), adverbs, adjectives, nouns));");
            }*/

        assertEquals("1240", NymIdGenerator.fromHash(BigInteger.valueOf(0), adverbs, adjectives, nouns));
        assertEquals("1241", NymIdGenerator.fromHash(BigInteger.valueOf(1), adverbs, adjectives, nouns));
        assertEquals("1242", NymIdGenerator.fromHash(BigInteger.valueOf(2), adverbs, adjectives, nouns));
        assertEquals("1243", NymIdGenerator.fromHash(BigInteger.valueOf(3), adverbs, adjectives, nouns));
        assertEquals("1244", NymIdGenerator.fromHash(BigInteger.valueOf(4), adverbs, adjectives, nouns));
        assertEquals("1245", NymIdGenerator.fromHash(BigInteger.valueOf(5), adverbs, adjectives, nouns));
        assertEquals("1246", NymIdGenerator.fromHash(BigInteger.valueOf(6), adverbs, adjectives, nouns));
        assertEquals("1247", NymIdGenerator.fromHash(BigInteger.valueOf(7), adverbs, adjectives, nouns));
        assertEquals("1248", NymIdGenerator.fromHash(BigInteger.valueOf(8), adverbs, adjectives, nouns));
        assertEquals("1249", NymIdGenerator.fromHash(BigInteger.valueOf(9), adverbs, adjectives, nouns));
        assertEquals("12410", NymIdGenerator.fromHash(BigInteger.valueOf(10), adverbs, adjectives, nouns));
        assertEquals("12411", NymIdGenerator.fromHash(BigInteger.valueOf(11), adverbs, adjectives, nouns));

        // With negative numbers
        assertEquals("12411", NymIdGenerator.fromHash(BigInteger.valueOf(-11), adverbs, adjectives, nouns));
        assertEquals("12410", NymIdGenerator.fromHash(BigInteger.valueOf(-10), adverbs, adjectives, nouns));
        assertEquals("1249", NymIdGenerator.fromHash(BigInteger.valueOf(-9), adverbs, adjectives, nouns));
        assertEquals("1248", NymIdGenerator.fromHash(BigInteger.valueOf(-8), adverbs, adjectives, nouns));
        assertEquals("1247", NymIdGenerator.fromHash(BigInteger.valueOf(-7), adverbs, adjectives, nouns));
        assertEquals("1246", NymIdGenerator.fromHash(BigInteger.valueOf(-6), adverbs, adjectives, nouns));
        assertEquals("1245", NymIdGenerator.fromHash(BigInteger.valueOf(-5), adverbs, adjectives, nouns));
        assertEquals("1244", NymIdGenerator.fromHash(BigInteger.valueOf(-4), adverbs, adjectives, nouns));
        assertEquals("1243", NymIdGenerator.fromHash(BigInteger.valueOf(-3), adverbs, adjectives, nouns));
        assertEquals("1242", NymIdGenerator.fromHash(BigInteger.valueOf(-2), adverbs, adjectives, nouns));
        assertEquals("1241", NymIdGenerator.fromHash(BigInteger.valueOf(-1), adverbs, adjectives, nouns));
        assertEquals("1240", NymIdGenerator.fromHash(BigInteger.valueOf(0), adverbs, adjectives, nouns));

        adverbs = List.of("1", "2");
        adjectives = List.of("3", "4");
        nouns = List.of("5", "6");
        assertEquals("1350", NymIdGenerator.fromHash(BigInteger.valueOf(0), adverbs, adjectives, nouns));
        assertEquals("1360", NymIdGenerator.fromHash(BigInteger.valueOf(1000), adverbs, adjectives, nouns));
        assertEquals("1450", NymIdGenerator.fromHash(BigInteger.valueOf(2000), adverbs, adjectives, nouns));
        assertEquals("1460", NymIdGenerator.fromHash(BigInteger.valueOf(3000), adverbs, adjectives, nouns));
        assertEquals("2350", NymIdGenerator.fromHash(BigInteger.valueOf(4000), adverbs, adjectives, nouns));
        assertEquals("2360", NymIdGenerator.fromHash(BigInteger.valueOf(5000), adverbs, adjectives, nouns));
        assertEquals("2450", NymIdGenerator.fromHash(BigInteger.valueOf(6000), adverbs, adjectives, nouns));
        assertEquals("2460", NymIdGenerator.fromHash(BigInteger.valueOf(7000), adverbs, adjectives, nouns));
        assertEquals("1350", NymIdGenerator.fromHash(BigInteger.valueOf(8000), adverbs, adjectives, nouns));
        assertEquals("1360", NymIdGenerator.fromHash(BigInteger.valueOf(9000), adverbs, adjectives, nouns));
        assertEquals("1450", NymIdGenerator.fromHash(BigInteger.valueOf(10000), adverbs, adjectives, nouns));
        assertEquals("1460", NymIdGenerator.fromHash(BigInteger.valueOf(11000), adverbs, adjectives, nouns));
        assertEquals("2350", NymIdGenerator.fromHash(BigInteger.valueOf(12000), adverbs, adjectives, nouns));
        assertEquals("2360", NymIdGenerator.fromHash(BigInteger.valueOf(13000), adverbs, adjectives, nouns));
        assertEquals("2450", NymIdGenerator.fromHash(BigInteger.valueOf(14000), adverbs, adjectives, nouns));
        assertEquals("2460", NymIdGenerator.fromHash(BigInteger.valueOf(15000), adverbs, adjectives, nouns));

        //larger inputs
        adverbs = List.of("1", "2");
        adjectives = List.of("3", "4");
        nouns = List.of("5", "6");
        assertEquals("1360", NymIdGenerator.fromHash(BigInteger.valueOf(1000), adverbs, adjectives, nouns));
        assertEquals("1451", NymIdGenerator.fromHash(BigInteger.valueOf(2001), adverbs, adjectives, nouns));
        assertEquals("1462", NymIdGenerator.fromHash(BigInteger.valueOf(3002), adverbs, adjectives, nouns));
        assertEquals("2353", NymIdGenerator.fromHash(BigInteger.valueOf(4003), adverbs, adjectives, nouns));
        assertEquals("2364", NymIdGenerator.fromHash(BigInteger.valueOf(5004), adverbs, adjectives, nouns));
        assertEquals("2455", NymIdGenerator.fromHash(BigInteger.valueOf(6005), adverbs, adjectives, nouns));
        assertEquals("2466", NymIdGenerator.fromHash(BigInteger.valueOf(7006), adverbs, adjectives, nouns));
        assertEquals("1357", NymIdGenerator.fromHash(BigInteger.valueOf(8007), adverbs, adjectives, nouns));

        adverbs = List.of("1", "2", "3");
        adjectives = List.of("3", "4", "5");
        nouns = List.of("5", "6", "7", "8", "9", "10", "11");
        BigInteger input = new BigInteger("test".getBytes(StandardCharsets.UTF_8));
        assertEquals("356748", NymIdGenerator.fromHash(input, adverbs, adjectives, nouns));

        input = new BigInteger("test1".getBytes(StandardCharsets.UTF_8));
        assertEquals("2511537", NymIdGenerator.fromHash(input, adverbs, adjectives, nouns));

        input = new BigInteger(DigestUtil.RIPEMD160("test".getBytes(StandardCharsets.UTF_8)));
        assertEquals("358823", NymIdGenerator.fromHash(input, adverbs, adjectives, nouns));

        input = new BigInteger(DigestUtil.RIPEMD160("test1".getBytes(StandardCharsets.UTF_8)));
        assertEquals("145726", NymIdGenerator.fromHash(input, adverbs, adjectives, nouns));

        // Feed random strings, expect no exceptions
        for (int i = 0; i < 100; i++) {
            input = new BigInteger(DigestUtil.RIPEMD160(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
            log.debug(NymIdGenerator.fromHash(input, adverbs, adjectives, nouns));
        }

        // Use real word lists
        // Feed random strings, expect no exceptions
        for (int i = 0; i < 3; i++) {
            byte[] hash = DigestUtil.RIPEMD160(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            log.debug(fromHash(hash));
        }

        // Use truncated lists, fill a set and check if we get no duplications
        Set<String> set = new HashSet<>();
        adverbs = read("/adverbs.txt").subList(0, 5);
        adjectives = read("/adjectives.txt").subList(0, 5);
        nouns = read("/nouns.txt").subList(0, 5);
        int combinations = adverbs.size() * adjectives.size() * nouns.size() * 1000;
        String first = null;
        for (int i = 0; i < combinations; i++) {
            String result = NymIdGenerator.fromHash(BigInteger.valueOf(i), adverbs, adjectives, nouns);
            Assertions.assertFalse(set.contains(result));
            set.add(result);
            if (first == null) {
                first = result;
            }
        }
        assertEquals(combinations, set.size());

        // At overflowing our combinations we get the first again
        String result = NymIdGenerator.fromHash(BigInteger.valueOf(combinations), adverbs, adjectives, nouns);
        Assertions.assertTrue(set.contains(result));
        assertEquals(first, result);
    }
}