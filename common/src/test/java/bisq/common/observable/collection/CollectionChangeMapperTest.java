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

package bisq.common.observable.collection;

import bisq.common.observable.Pin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * A list of the UI mirrors a collection of a service, for example the mediation cases of a mediator. One element which
 * cannot be mapped to a list item must not keep the other elements out of the list.
 */
class CollectionChangeMapperTest {
    private static final String FAILING = "failing";
    private static final Function<String, Boolean> ACCEPT_ALL = element -> true;
    private static final Function<String, String> MAP = element -> {
        if (element.equals(FAILING)) {
            throw new StringIndexOutOfBoundsException("begin 0, end 8, length 7");
        }
        return element.toUpperCase();
    };

    private final ObservableArray<String> source = new ObservableArray<>();
    private final List<String> target = new ArrayList<>();

    @Test
    void elementWhichCannotBeMappedDoesNotHideTheElementsWhichExistWhenTheMapperIsAdded() {
        source.addAll(List.of("a", FAILING, "b"));

        assertThatCode(this::addMapper).doesNotThrowAnyException();

        assertThat(target).containsExactly("A", "B");
    }

    @Test
    void elementWhichCannotBeMappedDoesNotHideTheElementsWhichAreAddedWithIt() {
        addMapper();

        assertThatCode(() -> source.addAll(List.of("a", FAILING, "b"))).doesNotThrowAnyException();

        assertThat(target).containsExactly("A", "B");
    }

    @Test
    void elementWhichCannotBeMappedDoesNotHideTheElementsWhichAreSetWithIt() {
        addMapper();
        source.add("old");

        assertThatCode(() -> source.setAll(List.of("a", FAILING, "b"))).doesNotThrowAnyException();

        assertThat(target).containsExactly("A", "B");
    }

    @Test
    void addingAnElementWhichCannotBeMappedDoesNotFailAndDoesNotStopLaterElements() {
        addMapper();
        source.add("a");

        assertThatCode(() -> source.add(FAILING)).doesNotThrowAnyException();
        source.add("b");

        assertThat(source).containsExactly("a", FAILING, "b");
        assertThat(target).containsExactly("A", "B");
    }

    @Test
    void elementWhoseFilterFailsDoesNotHideTheOtherElements() {
        source.addAll(List.of("a", FAILING, "b"));

        source.addCollectionChangeMapper(target,
                element -> {
                    if (element.equals(FAILING)) {
                        throw new IllegalStateException("filter failed");
                    }
                    return true;
                },
                String::toUpperCase,
                Runnable::run);

        assertThat(target).containsExactly("A", "B");
    }

    @Test
    void removingAnElementWhichCouldNotBeMappedDoesNotFailAndKeepsTheOtherElements() {
        addMapper();
        source.addAll(List.of("a", FAILING, "b"));

        assertThatCode(() -> source.remove(FAILING)).doesNotThrowAnyException();
        assertThat(target).containsExactly("A", "B");

        assertThatCode(() -> source.removeAll(List.of(FAILING, "a"))).doesNotThrowAnyException();
        assertThat(target).containsExactly("B");
    }

    @Test
    void elementsWhichCanBeMappedAreMirrored() {
        Pin pin = addMapper();
        source.addAll(List.of("a", "b"));
        source.add("c");
        source.remove("a");

        assertThat(target).containsExactly("B", "C");

        pin.unbind();
        source.add("d");
        assertThat(target).containsExactly("B", "C");
    }

    private Pin addMapper() {
        return source.addCollectionChangeMapper(target, ACCEPT_ALL, MAP, Runnable::run);
    }
}
