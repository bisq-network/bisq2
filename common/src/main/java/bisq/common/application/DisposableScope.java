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

package bisq.common.application;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public abstract class DisposableScope implements Disposable {
    private Set<Disposable> disposables;

    public DisposableScope() {
    }

    public void dispose() {
        if (disposables != null) {
            disposables.forEach(disposable -> {
                try {
                    disposable.dispose();
                } catch (Exception exception) {
                    log.error("Calling dispose on {} failed", disposable, exception);
                }
            });
            disposables.clear();
        }
    }

    protected void addDisposable(Disposable closeable) {
        getDisposables().add(closeable);
    }

    private Collection<Disposable> getDisposables() {
        if (disposables == null) {
            disposables = new HashSet<>();
        }
        return disposables;
    }
}
