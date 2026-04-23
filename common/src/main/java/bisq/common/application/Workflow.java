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

import bisq.common.observable.Pin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class Workflow {
    private Set<Pin> pins;

    public Workflow() {
    }

    public final void dispose() {
        if (pins != null) {
            pins.forEach(Pin::unbind);
            pins.clear();
        }
    }

    protected void pin(Pin pin) {
        getPins().add(pin);
    }

    private Collection<Pin> getPins() {
        if (pins == null) {
            pins = new HashSet<>();
        }
        return pins;
    }

}
