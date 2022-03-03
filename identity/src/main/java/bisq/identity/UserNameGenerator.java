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

import bisq.common.util.CollectionUtil;
import bisq.common.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Slf4j
public class UserNameGenerator {
    public static String fromHash(byte[] hash) {
        try {
            List<String> adjectives = read("/adjectives.txt");
            List<String> nouns = read("/nouns.txt");
            List<String> verbs = read("/verbs.txt");

            log.info("adjectives {}", adjectives);
            log.info("nouns {}", nouns);
            log.info("verbs {}", verbs);

            int numAdjectives = adjectives.size();

            BigInteger hashAsBigInteger = new BigInteger(hash);

            //todo implement algorithm
        } catch (IOException error) {
            log.error("Generating user name failed", error);
            throw new RuntimeException(error);
        }
        //todo remove once algorithm is implemented
        return CollectionUtil.getRandomElement(List.of("Satoshi Nakamoto", "Hal Finney", "Wei Dai", "Adam Back",
                "David Chaum", "Neal Koblitz", "Whitfield Diffie", "Adi Shamir", "Dan Boneh", "Daniel J. Bernstein",
                "Bruce Schneier", "Ralph Merkle", "Horst Feistel", "Claude Elwood Shannon", "John Nash",
                "Leonard Adleman", "Martin Hellman", "Ronald Rivest"));
    }

    private static List<String> read(String resource) throws IOException {
        List<String> list = new ArrayList<>();
        try (Scanner adjScanner = new Scanner(FileUtils.getResourceAsStream(resource))) {
            while (adjScanner.hasNextLine()) {
                list.add(adjScanner.nextLine());
            }
            return list;
        }
    }
}