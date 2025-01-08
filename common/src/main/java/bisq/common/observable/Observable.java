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

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

@Slf4j
@EqualsAndHashCode
public class Observable<S> implements ReadOnlyObservable<S> {
    private S value;

    @EqualsAndHashCode.Exclude
    private final Set<Consumer<S>> observers = new CopyOnWriteArraySet<>();

    public Observable() {
    }

    public Observable(S value) {
        set(value);
    }

    public Pin addObserver(Consumer<S> observer) {
        observers.add(observer);
        try {
            observer.accept(value);
        } catch (Exception e) {
            log.error("Observer {} caused an exception at handling update.", observer, e);
        }
        return () -> observers.remove(observer);
    }

    @Override
    public void removeObserver(Consumer<S> observer) {
        observers.remove(observer);
    }

    public void set(S value) {
        if ((this.value == null && value == null) || (this.value != null && this.value.equals(value))) {
            return;
        }
        this.value = value;
        observers.forEach(observer -> {
            try {
                observer.accept(value);
            } catch (Exception e) {
                log.error("Observer {} caused an exception at handling update.", observer, e);
            }
        });
    }

    public S get() {
        return value;
    }

    @Override
    public String toString() {
        return "Observable{value=" + value + '}';
    }
}