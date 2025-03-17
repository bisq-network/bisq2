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

package bisq.java_se.facades;

import bisq.common.facades.GuavaFacade;
import com.google.common.primitives.ImmutableIntArray;

import java.util.stream.IntStream;

public class JavaSeGuavaFacade implements GuavaFacade {
    @Override
    public IntStream toIntStream(ImmutableIntArray value) {
        return value.stream();
    }

    @Override
    public IntStream parallel(IntStream value) {
        return value.parallel();
    }

    @Override
    public ImmutableIntArray copyOf(IntStream value) {
        return ImmutableIntArray.copyOf(value);
    }
}
