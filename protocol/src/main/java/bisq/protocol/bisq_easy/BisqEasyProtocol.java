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

package bisq.protocol.bisq_easy;

import bisq.protocol.fsm.FiniteStateMachine;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Getter
public abstract class BisqEasyProtocol<M extends BisqEasyProtocolModel> {
    protected final M model;
    protected final transient FiniteStateMachine fsm = new FiniteStateMachine();

    public BisqEasyProtocol(M model) {
        this.model = model;
        configStateMachine();
        fsm.setCurrentState(model.getFsmState().get());
    }

    protected abstract void configStateMachine();
}