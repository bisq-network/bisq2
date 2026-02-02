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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

/**
 * Simple Observer which notifies about any change of the source collection.
 *
 * @param <S> The type of the source collection element.
 */
@Slf4j
@EqualsAndHashCode
@ToString
final class SimpleCollectionObserver<S> implements CollectionObserver<S> {
    private final Runnable observer;

    public SimpleCollectionObserver(Runnable observer) {
        this.observer = observer;
    }

    void onChanged() {
        try {
            observer.run();
        } catch (Exception e) {
            log.error("Observer {} caused an exception at handling update.", observer, e);
        }

    }

    @Override
    public void onAdded(S element) {
        onChanged();
    }

    @Override
    public void onAllAdded(Collection<? extends S> values) {
        onChanged();
    }

    @Override
    public void onAllSet(Collection<? extends S> values) {
        onChanged();
    }

    @Override
    public void onRemoved(Object element) {
        onChanged();
    }

    @Override
    public void onAllRemoved(Collection<?> values) {
        onChanged();
    }

    @Override
    public void onCleared() {
        onChanged();
    }
}