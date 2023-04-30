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
import lombok.ToString;

import java.util.Collection;

@EqualsAndHashCode
@ToString
final class ChangeListener<S> implements Observer<S> {
    private final Runnable handler;

    public ChangeListener(Runnable handler) {
        this.handler = handler;
    }

    @Override
    public void add(S element) {
        handler.run();
    }

    @Override
    public void addAll(Collection<? extends S> values) {
        handler.run();
    }

    @Override
    public void remove(Object element) {
        handler.run();
    }

    @Override
    public void removeAll(Collection<?> values) {
        handler.run();
    }

    @Override
    public void clear() {
        handler.run();
    }
}