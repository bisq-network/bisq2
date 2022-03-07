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
import java.math.BigInteger;

@Slf4j
public class UserNameGenerator {
    public static String fromHash(byte[] hash) {
        try {
            List<String> adjectives = read("/adjectives.txt");
            List<String> nouns = read("/nouns.txt");
            List<String> adverbs = read("/adverbs.txt");

            BigInteger numAdverbs = BigInteger.valueOf(adverbs.size());
            BigInteger numAdjectives = BigInteger.valueOf(adjectives.size());
            BigInteger numNouns = BigInteger.valueOf(nouns.size());
            BigInteger maxNumber = BigInteger.valueOf(999);
            BigInteger poolSize = ((numAdverbs.multiply(numAdjectives)).multiply(numNouns)).multiply(maxNumber);

            BigInteger hashAsBigInteger = new BigInteger(hash);
            BigInteger base = BigInteger.valueOf(2);
            BigInteger maxHashSize = base.pow(256);

            BigInteger normalizedHash = hashAsBigInteger.multiply(poolSize);
            normalizedHash = normalizedHash.divide(maxHashSize);

            BigInteger adverbIndex = normalizedHash.divide(numAdjectives.multiply(numNouns.multiply(maxNumber)));
            BigInteger reminder = normalizedHash.subtract(adverbIndex.multiply(numAdjectives.multiply(numNouns.multiply(maxNumber))));

            BigInteger adjectiveIndex = reminder.divide(numNouns.multiply(maxNumber));
            reminder = reminder.subtract(adjectiveIndex.multiply(numNouns.multiply(maxNumber)));

            BigInteger nounIndex = reminder.divide(maxNumber);
            reminder = reminder.subtract(nounIndex.multiply(maxNumber));

            int adverbsIndexInt = adverbIndex.intValue();
            int adjectiveIndexInt = adjectiveIndex.intValue();
            int nounIndexInt = nounIndex.intValue();
            int reminderInt = reminder.intValue();

            System.out.println(" ");
            System.out.println("From this day forward you shall also be known as: "+adverbs.get(adverbsIndexInt)+adjectives.get(adjectiveIndexInt)+nouns.get(nounIndexInt)+reminderInt);
            System.out.println(" ");

//            log.info("adverbs {}", adverbs);
//            log.info("adjectives {}", adjectives);
//            log.info("nouns {}", nouns);
            log.info("numAdjectives {} ", numAdjectives);
            log.info("numAdverbs {}", numAdverbs);
            log.info("numNouns {}", numNouns);
            log.info("poolSize {}", poolSize);
            log.info("hashAsInt {}", hashAsBigInteger);
            log.info("maxHashSi {}", maxHashSize);
            log.info("normalizedHash {}", normalizedHash);
            log.info("adverbIndex {}", adverbIndex);
            log.info("adjectiveIndex {}", adjectiveIndex);
            log.info("nounIndex {}", nounIndex);
            log.info("reminder {}", reminder);



            //todo implement algorithm
        } catch (IOException error) {
            log.error("Generating user name failed", error);
            throw new RuntimeException(error);
        }
        //todo remove once algorithm is implemented
        return "TODO";
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