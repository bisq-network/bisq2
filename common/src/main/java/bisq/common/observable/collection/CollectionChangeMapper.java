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

import bisq.common.util.ExceptionUtil;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Observer implementation which maps the changes of the source collection to the
 * target collection (usually a JavaFx ObservableCollection) using the mapFunction and the given
 * executor (usually runs the runnable on the JavaFX Application Thread).
 * This is useful for mirroring changes of the source collection on the target collection while supporting
 * the UI frameworks constraints and mapping to different element types (usually ListItems).
 *
 * A source element whose filter function or map function throws is skipped. It must not keep the other elements out
 * of the target collection: the source collection can hold data of other users, for example the mediation cases of a
 * mediator, and one element which cannot be mapped would otherwise empty the whole list.
 *
 * @param <S> The type of the collection element of the source collection
 * @param <T> The type of the collection element of the target collection
 */
@Slf4j
@EqualsAndHashCode
@ToString
final class CollectionChangeMapper<S, T> implements CollectionObserver<S> {
    private static final int MAX_LOGGED_STACK_TRACE_LINES = 10;

    private final Collection<T> targetCollection;
    private final Function<S, Boolean> filterFunction;
    private final Function<S, T> mapFunction;
    private final Consumer<Runnable> executor;

    CollectionChangeMapper(Collection<T> targetCollection,
                           Function<S, Boolean> filterFunction,
                           Function<S, T> mapFunction,
                           Consumer<Runnable> executor) {
        this.targetCollection = targetCollection;
        this.filterFunction = filterFunction;
        this.mapFunction = mapFunction;
        this.executor = executor;
    }

    @Override
    public void onAdded(S sourceItem) {
        executor.accept(() -> filterAndMap(sourceItem).ifPresent(item -> {
            if (!targetCollection.contains(item)) {
                targetCollection.add(item);
            }
        }));
    }

    @Override
    public void onAllAdded(Collection<? extends S> sourceItems) {
        executor.accept(() -> targetCollection.addAll(sourceItems.stream()
                .flatMap(sourceItem -> filterAndMap(sourceItem).stream())
                .filter(item -> !targetCollection.contains(item))
                .toList()));
    }

    @Override
    public void onAllSet(Collection<? extends S> sourceItems) {
        executor.accept(() -> {
            targetCollection.clear();
            targetCollection.addAll(sourceItems.stream()
                    .flatMap(sourceItem -> filterAndMap(sourceItem).stream())
                    .toList());
        });
    }

    @Override
    public void onRemoved(Object sourceItem) {
        executor.accept(() -> {
            //noinspection unchecked
            S sourceItemCasted = (S) sourceItem;
            // We do not apply the filter at remove as the remove action could have impact on the filter
            // predicate (e.g. if we close an item and set a flag used in the filter)
            map(sourceItemCasted).ifPresent(targetCollection::remove);
        });
    }

    @Override
    public void onAllRemoved(Collection<?> sourceItems) {
        executor.accept(() -> targetCollection.removeAll(sourceItems.stream()
                .map(element -> {
                    //noinspection unchecked
                    return (S) element;
                })
                .flatMap(sourceItem -> map(sourceItem).stream())
                .collect(Collectors.toSet())));
    }

    private Optional<T> filterAndMap(S sourceItem) {
        try {
            return filterFunction.apply(sourceItem) ? Optional.of(mapFunction.apply(sourceItem)) : Optional.empty();
        } catch (RuntimeException e) {
            logSkippedElement(sourceItem, e);
            return Optional.empty();
        }
    }

    // An element which could not be mapped when it was added is not in the target collection, so there is nothing to
    // remove.
    private Optional<T> map(S sourceItem) {
        try {
            return Optional.of(mapFunction.apply(sourceItem));
        } catch (RuntimeException e) {
            logSkippedElement(sourceItem, e);
            return Optional.empty();
        }
    }

    // The element and the message of the exception can contain data of other users, so the log gets only the type of
    // the element and the beginning of the stack trace without messages.
    private void logSkippedElement(S sourceItem, RuntimeException e) {
        String stackTrace = ExceptionUtil.getSafeStackTraceAsString(e).lines()
                .limit(MAX_LOGGED_STACK_TRACE_LINES)
                .collect(Collectors.joining("\n"));
        log.error("Skipped an element of type {} which cannot be mapped to the target collection. Cause:\n{}",
                sourceItem == null ? "null" : sourceItem.getClass().getSimpleName(),
                stackTrace);
    }

    @Override
    public void onCleared() {
        executor.accept(targetCollection::clear);
    }
}