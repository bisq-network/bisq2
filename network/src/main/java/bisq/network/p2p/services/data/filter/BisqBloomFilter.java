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


import bisq.common.data.ByteArray;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

@ToString
@Slf4j
@SuppressWarnings("UnstableApiUsage")
public class BisqBloomFilter implements DataFilter {
    // A fpp of 0.01 and expectedInsertions of 10000 results in a filter size of 12kb (see BloomFilterTest). 
    private static final double FPP = 0.01;
    private static final int EXPECTED_INSERTIONS = 10000;

    @Getter
    private final BloomFilter<byte[]> filter;

    public BisqBloomFilter(Set<byte[]> hashes) {
        this(hashes, EXPECTED_INSERTIONS, FPP);
    }

    @VisibleForTesting
    BisqBloomFilter(Set<byte[]> hashes, int expectedInsertions, double fpp) {
        log.error("Num hashes {}", hashes.size());
        filter = BloomFilter.create(Funnels.byteArrayFunnel(), expectedInsertions, fpp);
        hashes.forEach(filter::put);
    }

    @Override
    public boolean doInclude(ByteArray key) {
        //log.error("doInclude result={}",  !filter.mightContain(key.getBytes()));
        return !filter.mightContain(key.getBytes());
    }

    public static BloomFilter<byte[]> readFrom(InputStream inputStream) throws IOException {
        return BloomFilter.readFrom(inputStream, Funnels.byteArrayFunnel());
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        filter.writeTo(outputStream);
    }
}
