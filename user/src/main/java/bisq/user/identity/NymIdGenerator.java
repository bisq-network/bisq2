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

package bisq.user.identity;

import bisq.common.file.FileUtils;
import bisq.common.util.ByteArrayUtils;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Generates a combination of and adverb + adjective + noun + a number from a given hash as input (hash of pubKey).
 * Number of combinations: 4833 * 450 * 12591 * 1000 = 27385711200000 (2 ^ 44.6)
 * Algorithm and word lists borrowed from: <a href="https://raw.githubusercontent.com/Reckless-Satoshi/robosats/main/api/nick_generator/">Reckless-Satoshi</a>
 */
@Slf4j
public class NymIdGenerator {
    private static final BigInteger MAX_INTEGER = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final String DEFAULT_SEPARATOR = "-";
    private static final List<String> ADVERBS, ADJECTIVES, NOUNS;

    static {
        ADVERBS = read("adverbs.txt");
        ADJECTIVES = read("adjectives.txt");
        NOUNS = read("nouns.txt");
    }

    public static String generate(byte[] pubKeyHash, byte[] powSolution) {
        byte[] combined = ByteArrayUtils.concat(powSolution, pubKeyHash);
        BigInteger input = new BigInteger(combined);
        return generate(input, ADVERBS, ADJECTIVES, NOUNS, DEFAULT_SEPARATOR);
    }

    @VisibleForTesting
    static String generate(BigInteger input, List<String> adverbs, List<String> adjectives, List<String> nouns) {
        return generate(input, adverbs, adjectives, nouns, "");
    }

    static String generate(BigInteger input, List<String> adverbs, List<String> adjectives, List<String> nouns, String separator) {
        input = input.abs();
        BigInteger numAdjectives = BigInteger.valueOf(adjectives.size());
        BigInteger numNouns = BigInteger.valueOf(nouns.size());
        BigInteger appendixNumber = BigInteger.valueOf(1000);

        BigInteger adverbIndex = input.divide(numAdjectives.multiply(numNouns).multiply(appendixNumber));
        BigInteger remainderHash = input.subtract(adverbIndex.multiply(numAdjectives.multiply(numNouns.multiply(appendixNumber))));
        BigInteger adjectiveIndex = remainderHash.divide(numNouns.multiply(appendixNumber));
        BigInteger remainderAdverb = remainderHash.subtract(adjectiveIndex.multiply(numNouns).multiply(appendixNumber));
        BigInteger nounsIndex = remainderAdverb.divide(appendixNumber);
        BigInteger appendixIndex = remainderAdverb.subtract(nounsIndex.multiply(appendixNumber));

        // Limit BigInteger value to MAX_INTEGER and further to list size by applying mod
        int adverbsIndexInt = adverbIndex.mod(MAX_INTEGER).intValue() % adverbs.size();
        int adjectiveIndexInt = adjectiveIndex.mod(MAX_INTEGER).intValue() % adjectives.size();
        int nounIndexInt = nounsIndex.mod(MAX_INTEGER).intValue() % nouns.size();
        int appendixInt = appendixIndex.mod(MAX_INTEGER).intValue();
        appendixInt = appendixInt % 1000;
        return adverbs.get(adverbsIndexInt) + separator +
                adjectives.get(adjectiveIndexInt) + separator +
                nouns.get(nounIndexInt) + separator +
                appendixInt;
    }

    @VisibleForTesting
    static List<String> read(String resource) {
        List<String> list = new ArrayList<>();
        try (Scanner scanner = new Scanner(FileUtils.getResourceAsStream(resource))) {
            while (scanner.hasNextLine()) {
                list.add(scanner.nextLine());
            }
            return list;
        } catch (IOException e) {
            // Not expected so we convert to a RuntimeException
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}