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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@ToString

@Getter

@EqualsAndHashCode
public class Transition {
    private final Set<State> sourceStates = new HashSet<>();
    @Setter
    private State targetState;
    @Setter
    private Class<? extends Event> eventClass;
    @Setter
    private Optional<Class<? extends EventHandler>> eventHandlerClass = Optional.empty();

    boolean isValid() {
        return !sourceStates.isEmpty() &&
                targetState != null &&
                eventClass != null &&
                !sourceStates.contains(targetState);
    }
}
