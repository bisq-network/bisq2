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

package bisq.network.p2p.services.data.filter;

import bisq.common.ObjectSerializer;
import bisq.common.data.ByteArray;
import bisq.common.util.FileUtils;
import com.google.common.hash.BloomFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
public class BloomFilterTest {
    @Test
    public void testBloomFilter() throws IOException {
        double fpp = 0.01;
        double entries = 1000;
        int expectedInsertions = 1000;
        Set<byte[]> filterContent = new HashSet<>();
        for (int i = 0; i < entries; i++) {
            filterContent.add(getBytes());
        }
        BisqBloomFilter bisqBloomFilter = new BisqBloomFilter(filterContent, expectedInsertions, fpp);

        // All initial data must be found
        filterContent.forEach(hash -> assertFalse(bisqBloomFilter.doInclude(new ByteArray(hash))));

        int falsePositives = 0;
        // Create new additional data not in the bisqBloomFilter
        long ts = System.currentTimeMillis();
        for (int i = (int) entries; i < 2 * entries; i++) {
            if (!bisqBloomFilter.doInclude(new ByteArray(getBytes()))) {
                falsePositives++;
            }
        }
        // mightContain calls for 1000.0 entries took 3 ms 
        // mightContain calls for 10000.0 entries took 14 ms 
        log.info("mightContain calls for {} entries took {} ms", entries, System.currentTimeMillis() - ts);
        int serializedSize = ObjectSerializer.serialize(bisqBloomFilter.getFilter()).length;
        double realFpp = falsePositives / entries;
        double deviation = falsePositives > 0 ? realFpp / fpp - 1 : 0;
        log.info("falsePositives {}", falsePositives);
        log.info("fpp={}, expectedInsertions={}, realFpp={}, deviation={}, serializedSize={} bytes",
                fpp, expectedInsertions, realFpp, deviation, serializedSize);
        assertTrue(deviation < 0.2);

        // fpp=0.01, expectedInsertions=10000, realFpp=0.0107, deviation=0.06999999999999984, serializedSize=12398 bytes 
        // fpp=0.1, expectedInsertions=10000, realFpp=0.1055, deviation=0.05499999999999994, serializedSize=6406 bytes 
        // fpp=0.01, expectedInsertions=1000, realFpp=0.011, deviation=0.09999999999999987, serializedSize=1614 bytes 
        // fpp=0.001, expectedInsertions=1000, realFpp=0.002, deviation=1.0, serializedSize=2214 bytes 

        File tempFile = File.createTempFile("bloomfilter_test", null, null);
        FileUtils.deleteOnExit(tempFile);
        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        bisqBloomFilter.writeTo(fileOutputStream);
        FileInputStream fileInputStream = new FileInputStream(tempFile);
        BloomFilter<byte[]> persisted = BisqBloomFilter.readFrom(fileInputStream);
        assertEquals(persisted, bisqBloomFilter.getFilter());
    }

    private byte[] getBytes() {
        byte[] bytes = new byte[100];
        new Random().nextBytes(bytes);
        return bytes;
    }
}
