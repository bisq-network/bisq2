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

package bisq.common.guava;

import com.google.common.primitives.ImmutableIntArray;

import java.util.function.Function;
import java.util.stream.IntStream;

public class GuavaAndroidFunctionProvider implements GuavaFunctionProvider {
    public Function<ImmutableIntArray, IntStream> getToIntStreamFunction() {
        return array -> IntStream.range(0, array.length()).map(array::get);
    }

    public Function<IntStream, IntStream> getToParallelFunction() {
        return intStream -> {
            ImmutableIntArray array = ImmutableIntArray.copyOf(intStream.toArray());
            return IntStream.range(0, array.length()).map(array::get).parallel();
        };
    }

    public Function<IntStream, ImmutableIntArray> getCopyOfFunction() {
        return intStream -> ImmutableIntArray.copyOf(intStream.toArray());
    }
}
