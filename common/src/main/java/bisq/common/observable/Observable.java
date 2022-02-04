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

package bisq.common.observable;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class Observable<T> implements Serializable {
    private T value;
    private transient final Set<Consumer<T>> observers = new CopyOnWriteArraySet<>();

    public Observable() {
    }

    public Observable(T value) {
        set(value);
    }

    public Pin addObserver(Consumer<T> observer) {
        observers.add(observer);
        observer.accept(value);
        return () -> observers.remove(observer);
    }

    public void set(T value) {
        this.value = value;
        observers.forEach(observer -> observer.accept(value));
    }

    public T get() {
        return value;
    }
}