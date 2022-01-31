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

package bisq.common.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ObservedSet<T> extends CopyOnWriteArraySet<T> {
    private static final AtomicInteger HANDLER_COUNTER = new AtomicInteger(0);

    public static class Handler<M, L> {
        private final List<L> listItems;
        private final Function<M, L> mapper;
        private final Consumer<Runnable> executor;

        public Handler(List<L> listItems, Function<M, L> mapper, Consumer<Runnable> executor) {
            this.listItems = listItems;
            this.mapper = mapper;
            this.executor = executor;
        }

        public void add(M element) {
            executor.accept(() -> listItems.add(mapper.apply(element)));
        }

        public void addAll(Collection<? extends M> c) {
            executor.accept(() -> listItems.addAll(c.stream().map(mapper).collect(Collectors.toSet())));
        }

        public void remove(Object element) {
            //noinspection unchecked
            executor.accept(() -> listItems.remove(mapper.apply((M) element)));
        }

        public void removeAll(Collection<?> c) {
            //noinspection unchecked
            executor.accept(() -> listItems.removeAll(c.stream().map(element -> mapper.apply((M) element)).collect(Collectors.toSet())));
        }

        public void clear() {
            executor.accept(listItems::clear);
        }
    }


    private final Map<Integer, Handler<T, ?>> handlers = new ConcurrentHashMap<>();

    public <L> int bind(List<L> listItems, Function<T, L> mapper, Consumer<Runnable> executor) {
        int key = HANDLER_COUNTER.incrementAndGet();
        handlers.put(key, new Handler<>(listItems, mapper, executor));
        return key;
    }

    public void unbind(int key) {
        handlers.remove(key);
    }

    @Override
    public boolean add(T element) {
        boolean result = super.add(element);
        handlers.values().forEach(l -> l.add(element));
        return result;
    }

    public boolean addAll(Collection<? extends T> c) {
        boolean result = super.addAll(c);
        handlers.values().forEach(l -> l.addAll(c));
        return result;
    }

    @Override
    public boolean remove(Object element) {
        boolean result = super.remove(element);
        handlers.values().forEach(l -> l.remove(element));
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = super.removeAll(c);
        handlers.values().forEach(l -> l.removeAll(c));
        return result;
    }

    @Override
    public void clear() {
        super.clear();
        handlers.values().forEach(Handler::clear);
    }
}