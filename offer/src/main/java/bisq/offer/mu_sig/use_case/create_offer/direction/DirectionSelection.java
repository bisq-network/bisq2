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

package bisq.offer.mu_sig.use_case.create_offer.direction;

import bisq.common.application.LifecycleScope;
import bisq.common.observable.Pin;
import bisq.offer.Direction;
import bisq.offer.mu_sig.use_case.dependencies.CreateOfferDraftCookieStore;
import lombok.experimental.Delegate;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

public class DirectionSelection extends LifecycleScope {
    @Delegate
    private final CreateOfferDirectionModel model;
    private final CreateOfferDraftCookieStore cookieStore;
    private final Set<Consumer<Direction>> listeners = new CopyOnWriteArraySet<>();
    private final Object draftLock;

    public DirectionSelection(CreateOfferDraftCookieStore cookieStore, Object draftLock) {
        this.draftLock = checkNotNull(draftLock, "draftLock must not be null");
        this.cookieStore = checkNotNull(cookieStore, "cookieStore must not be null");
        this.model = new CreateOfferDirectionModel();
    }

    @Override
    public void initialize() {
        if (getDisplayDirection() == null) {
            Direction displayDirection = cookieStore.getDisplayDirection();
            applyDisplayDirection(displayDirection, false);
        }
    }


    /* --------------------------------------------------------------------- */
    // User input
    /* --------------------------------------------------------------------- */

    public void onSetDisplayDirection(Direction displayDirection) {
        synchronized (draftLock) {
            doSetDisplayDirection(displayDirection);
        }
    }

    private void doSetDisplayDirection(Direction displayDirection) {
        checkNotNull(displayDirection, "displayDirection must not be null");
        applyDisplayDirection(displayDirection, true);
    }


    private void applyDisplayDirection(Direction displayDirection, boolean notifyListeners) {
        if (displayDirection != model.getDisplayDirection()) {
            model.setDisplayDirection(displayDirection);
            cookieStore.persistDisplayDirection(displayDirection);
            if (notifyListeners) {
                listeners.forEach(listener -> listener.accept(displayDirection));
            }
        }
    }

    public Pin addDisplayDirectionListener(Consumer<Direction> listener) {
        checkNotNull(listener, "listener must not be null");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}
