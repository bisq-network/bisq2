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

package bisq.social.user.profile;

import bisq.common.util.FileUtils;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Generates a combination of and adverb + adjective + noun + a number from a given hash as input (hash of pubkey).
 * Number of combinations: 4833 * 450 * 12591 * 1000 = 27385711200000 (2 ^ 44.6)
 * Algorithm and word lists borrowed from: https://raw.githubusercontent.com/Reckless-Satoshi/robosats/main/api/nick_generator/
 */
@Slf4j
public class ProfileIdGenerator {
    private static final BigInteger MAX_INTEGER = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final String DEFAULT_SEPARATOR = "-";

    public static String fromHash(byte[] hash) {
        List<String> adverbs = read("/adverbs.txt");
        List<String> adjectives = read("/adjectives.txt");
        List<String> nouns = read("/nouns.txt");
        return fromHash(new BigInteger(hash), adverbs, adjectives, nouns, DEFAULT_SEPARATOR);
    }

    @VisibleForTesting
    static String fromHash(BigInteger hashAsBigInteger, List<String> adverbs, List<String> adjectives, List<String> nouns) {
        return fromHash(hashAsBigInteger, adverbs, adjectives, nouns, "");
    }

    static String fromHash(BigInteger hashAsBigInteger, List<String> adverbs, List<String> adjectives, List<String> nouns, String separator) {
        hashAsBigInteger = hashAsBigInteger.abs();
        BigInteger numAdjectives = BigInteger.valueOf(adjectives.size());
        BigInteger numNouns = BigInteger.valueOf(nouns.size());
        BigInteger appendixNumber = BigInteger.valueOf(1000);

        BigInteger adverbIndex = hashAsBigInteger.divide(numAdjectives.multiply(numNouns).multiply(appendixNumber));
        BigInteger remainderHash = hashAsBigInteger.subtract(adverbIndex.multiply(numAdjectives.multiply(numNouns.multiply(appendixNumber))));
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