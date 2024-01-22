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

package bisq.common.fsm;

import lombok.Getter;

public interface State {

    boolean isFinalState();

    default boolean isAnyState() {
        return false;
    }

    String name();

    int getOrdinal();

    @Getter
    enum FsmState implements State {
        ANY(false, true, 0),
        ERROR(true, false, Integer.MAX_VALUE);

        private final boolean isFinalState;
        private final boolean isAnyState;
        private final int ordinal;

        FsmState(boolean isFinalState, boolean isAnyState, int ordinal) {
            this.isFinalState = isFinalState;
            this.isAnyState = isAnyState;
            this.ordinal = ordinal;
        }
    }
}
