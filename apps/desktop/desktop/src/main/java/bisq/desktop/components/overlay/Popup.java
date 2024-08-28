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

package bisq.desktop.components.overlay;

import bisq.desktop.common.threading.UIScheduler;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

@EqualsAndHashCode(callSuper = true)
public class Popup extends Overlay<Popup> {

    public Popup() {
    }

    public void onReadyForDisplay() {
        super.display();
    }

    @Override
    protected Popup cast() {
        return this;
    }

    @Override
    protected void onShow() {
        Manager.queueForDisplay(this);
    }

    @Override
    protected void onHidden() {
        Manager.onHidden(this);
    }

    @Slf4j
    private static class Manager {
        private static final Queue<Popup> popups = new LinkedBlockingQueue<>(5);
        private static final Set<Popup> displayedPopups = new HashSet<>();

        private static void queueForDisplay(Popup popup) {
            boolean result = popups.offer(popup);
            if (!result) {
                log.warn("The capacity is full with popups in the queue.\n\t" +
                        "Not added new popup=" + popup);
            }
            displayNext();
        }

        private static void onHidden(Popup popup) {
            boolean wasRemoved = displayedPopups.remove(popup);
            if (!wasRemoved) {
                log.warn("We got isHidden called with a popup not existing in our displayedPopups.");
            }
            UIScheduler.run(Manager::displayNext).after(100);
        }

        private static void displayNext() {
            Popup popup = popups.poll();
            if (popup != null) {
                popup.onReadyForDisplay();
                displayedPopups.add(popup);
            }
        }
    }
}
